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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

        val upload = Upload(UUID.randomUUID(), 0L, "alice", StoragePath("/pending-upload"), 0L)
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
    fun test_finaliseUpload_rejectsPathIntoUploadsFolder() {
        val uploadsFolder = ".me.polynom.polycloud.uploads"

        val storage = LocalStorage(StoragePath("/"), rootDir, true)

        val upload = Upload(UUID.randomUUID(), 0L, "alice", StoragePath("/$uploadsFolder/evil.txt"), 0L)
        assertThrows<StorageQuotaException> {
            storage.prepareUpload(upload)
        }
    }
}
