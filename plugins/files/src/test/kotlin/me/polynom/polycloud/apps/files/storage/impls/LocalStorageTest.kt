package me.polynom.polycloud.apps.files.storage.impls

import me.polynom.polycloud.apps.files.exceptions.StorageReadOnlyException
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
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes

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
}
