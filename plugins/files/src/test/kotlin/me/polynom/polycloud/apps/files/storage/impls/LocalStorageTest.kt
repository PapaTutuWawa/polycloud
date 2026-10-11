package me.polynom.polycloud.apps.files.storage.impls

import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageQuotaException
import me.polynom.polycloud.apps.files.exceptions.StorageReadOnlyException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.storage.EntryMeta
import me.polynom.polycloud.apps.files.storage.StoragePath
import me.polynom.polycloud.apps.files.storage.StorageTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.IOError
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalStorageTest : StorageTest<LocalStorage> {
    @TempDir
    lateinit var rootDir: Path

    override fun createStorage(vararg files: Triple<String, StoragePath, ByteArray>): LocalStorage {
        for ((user, path, content) in files) {
            val fsPath = rootDir.resolve(user, path.toString().substring(1))
            if (path.file) {
                fsPath.parent.createDirectories()
                fsPath.createFile()
                fsPath.writeBytes(content)
            } else {
                fsPath.createDirectories()
            }
        }

        return LocalStorage(StoragePath("/"), rootDir, false)
    }

    @Test
    fun test_delete_throwsOnReadOnlyFileSystem() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/file.txt"), ByteArray(0)),
            )

        // Make directory read-only
        val userDir = rootDir.resolve("alice")
        val writablePermissions = Files.getPosixFilePermissions(userDir)
        Files.setPosixFilePermissions(
            userDir,
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE),
        )

        try {
            assertThrows<IOError> {
                storage.delete("alice", StoragePath("/file.txt"))
            }
        } finally {
            Files.setPosixFilePermissions(userDir, writablePermissions)
        }
    }

    @Test
    fun test_uploadsFolder_isHiddenAndInaccessible() {
        // Mirrors the private UPLOADS_FOLDER constant in LocalStorage.
        val uploadsFolder = ".me.polynom.polycloud.uploads"

        // Pending uploads live directly under `root`, so only a shared backend exposes
        // that namespace to users at all.
        val storage = LocalStorage(StoragePath("/"), rootDir, true)

        val upload = Upload(0L, "alice", StoragePath("/pending-upload")).apply { id = UUID.randomUUID() }
        storage.prepareUpload(upload)
        assertEquals(listOf(), storage.list("alice", StoragePath("/")))
        storage.finaliseUpload(upload)
        assertEquals(
            listOf(
                EntryMeta("pending-upload", false, 0),
            ),
            storage.list("alice", StoragePath("/")),
        )

        assertThrows<StoragePathDoesNotExistException> {
            storage.list("alice", StoragePath("/$uploadsFolder/"))
        }

        val uploadPath = StoragePath("/$uploadsFolder/${upload.id}")
        assertThrows<StoragePathDoesNotExistException> {
            storage.read("alice", uploadPath)
        }

        assertThrows<StoragePathDoesNotExistException> {
            storage.delete("alice", uploadPath)
        }
    }

    @Test
    fun test_finaliseUpload_ontoExistingFile_replacesIt() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a.txt"), "old".toByteArray()),
            )

        val upload = Upload(UUID.randomUUID(), 3L, "alice", "/a.txt", 0L)
        storage.prepareUpload(upload)
        storage.continueUpload(upload, "new".toByteArray().inputStream())
        storage.finaliseUpload(upload)

        assertEquals("new", rootDir.resolve("alice", "a.txt").readText())
    }

    @Test
    fun test_finaliseUpload_rejectsPathIntoUploadsFolder() {
        val uploadsFolder = ".me.polynom.polycloud.uploads"

        val storage = LocalStorage(StoragePath("/"), rootDir, true)

        val upload = Upload(0L, "alice", StoragePath("/$uploadsFolder/evil.txt")).apply { id = UUID.randomUUID() }
        assertThrows<StorageQuotaException> {
            storage.prepareUpload(upload)
        }
    }

    @Test
    fun test_stat_missingPath_returnsNull() {
        val storage = createStorage()

        assertNull(storage.stat("alice", StoragePath("/missing.txt")))
    }

    @Test
    fun test_stat_nonSharedBackendRootForUserWhoNeverWroteAnything_reportsAsEmptyExistingFolder() {
        // The per-user root is lazily created on first write (see prepareUpload()); stat() must
        // report it as an already-existing empty folder beforehand, not as missing - this is
        // what makes a brand-new, never-written-to mount point show up in a parent listing.
        val storage = createStorage()
        assertFalse(rootDir.resolve("brand-new-user").exists())

        val stat = storage.stat("brand-new-user", StoragePath("/"))

        assertNotNull(stat)
        assertTrue(stat.directory)
    }

    @Test
    fun test_stat_sharedBackendMissingRoot_stillReportsMissing() {
        val storage = LocalStorage(StoragePath("/"), rootDir.resolve("does-not-exist"), true)

        assertNull(storage.stat("alice", StoragePath("/")))
    }

    @Test
    fun test_stat_file_reportsSizeAndNotDirectory() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/file.txt"), "hello".toByteArray()),
            )

        val stat = storage.stat("alice", StoragePath("/file.txt"))

        assertNotNull(stat)
        assertFalse(stat.directory)
        assertEquals(5L, stat.size)
    }

    @Test
    fun test_stat_folder_reportsDirectoryWithNullSize() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/folder/"), ByteArray(0)),
            )

        val stat = storage.stat("alice", StoragePath("/folder"))

        assertNotNull(stat)
        assertTrue(stat.directory)
        assertNull(stat.size)
    }

    @Test
    fun test_stat_ignoresTheFileFlagOnThePath() {
        // Folder on disk, but the StoragePath claims to be a file - stat() must not trust that.
        val storage =
            createStorage(
                Triple("alice", StoragePath("/folder/"), ByteArray(0)),
            )

        val stat = storage.stat("alice", StoragePath(listOf("folder"), true))

        assertNotNull(stat)
        assertTrue(stat.directory)
    }

    @Test
    fun test_createDirectory_createsEmptyFolder() {
        val storage = createStorage()

        storage.createDirectory("alice", StoragePath("/folder"))

        assertTrue(rootDir.resolve("alice", "folder").exists())
        assertEquals(emptyList(), storage.list("alice", StoragePath("/folder/")))
    }

    @Test
    fun test_createDirectory_missingParent_throws() {
        val storage = createStorage()

        assertThrows<StoragePathDoesNotExistException> {
            storage.createDirectory("alice", StoragePath("/missing/folder"))
        }
    }

    @Test
    fun test_createDirectory_alreadyExists_throws() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/folder/"), ByteArray(0)),
            )

        assertThrows<FileAlreadyExistsException> {
            storage.createDirectory("alice", StoragePath("/folder"))
        }
    }

    @Test
    fun test_copy_file_duplicatesContentAndKeepsSource() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a.txt"), "hello".toByteArray()),
            )

        storage.copy("alice", StoragePath("/a.txt"), StoragePath("/b.txt"))

        assertEquals("hello", rootDir.resolve("alice", "b.txt").readText())
        assertTrue(rootDir.resolve("alice", "a.txt").exists())
    }

    @Test
    fun test_copy_folderDeep_copiesDescendants() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a/"), ByteArray(0)),
                Triple("alice", StoragePath("/a/f.txt"), "hi".toByteArray()),
            )

        storage.copy("alice", StoragePath("/a"), StoragePath("/b"))

        assertEquals("hi", rootDir.resolve("alice", "b", "f.txt").readText())
        assertTrue(rootDir.resolve("alice", "a", "f.txt").exists())
    }

    @Test
    fun test_copy_folderOntoExistingFolder_replacesRatherThanMerges() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a/"), ByteArray(0)),
                Triple("alice", StoragePath("/a/new.txt"), "new".toByteArray()),
                Triple("alice", StoragePath("/b/"), ByteArray(0)),
                Triple("alice", StoragePath("/b/stale.txt"), ByteArray(0)),
            )

        storage.copy("alice", StoragePath("/a"), StoragePath("/b"))

        assertTrue(rootDir.resolve("alice", "b", "new.txt").exists())
        assertFalse(rootDir.resolve("alice", "b", "stale.txt").exists())
    }

    @Test
    fun test_copy_missingSource_throws() {
        val storage = createStorage()

        assertThrows<StoragePathDoesNotExistException> {
            storage.copy("alice", StoragePath("/missing.txt"), StoragePath("/b.txt"))
        }
    }

    @Test
    fun test_copy_missingDestinationParent_throws() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a.txt"), ByteArray(0)),
            )

        assertThrows<StoragePathDoesNotExistException> {
            storage.copy("alice", StoragePath("/a.txt"), StoragePath("/missing/b.txt"))
        }
    }

    @Test
    fun test_move_file_relocatesAndRemovesSource() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a.txt"), "hello".toByteArray()),
            )

        storage.move("alice", StoragePath("/a.txt"), StoragePath("/b.txt"))

        assertEquals("hello", rootDir.resolve("alice", "b.txt").readText())
        assertFalse(rootDir.resolve("alice", "a.txt").exists())
    }

    @Test
    fun test_move_ontoExistingDestination_replacesIt() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a.txt"), "new".toByteArray()),
                Triple("alice", StoragePath("/b.txt"), "old".toByteArray()),
            )

        storage.move("alice", StoragePath("/a.txt"), StoragePath("/b.txt"))

        assertEquals("new", rootDir.resolve("alice", "b.txt").readText())
    }

    @Test
    fun test_move_folder_relocatesDescendants() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/a/"), ByteArray(0)),
                Triple("alice", StoragePath("/a/f.txt"), "hi".toByteArray()),
            )

        storage.move("alice", StoragePath("/a"), StoragePath("/b"))

        assertFalse(rootDir.resolve("alice", "a").exists())
        assertEquals("hi", rootDir.resolve("alice", "b", "f.txt").readText())
    }
}
