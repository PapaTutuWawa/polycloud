package me.polynom.polycloud.apps.files.storage.impls

import com.sun.nio.file.ExtendedOpenOption
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.apache.tomcat.util.http.fileupload.FileUtils
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.relativeTo

class LocalStorage (val root: Path, val shared: Boolean) : Storage {

    private fun resolvePath(user: String, path: StoragePath): Path = if (shared) {
        root.resolve(path.toString().substring(1))
    } else {
        root.resolve(user, path.toString().substring(1))
    }

    override fun listFiles(
        user: String,
        path: StoragePath
    ): List<StoragePath> {
        if (path.file) {
            throw IllegalArgumentException("Path references file")
        }

        val path = resolvePath(user, path)
        val dir = path.toFile()
        if (!dir.exists() || dir.isFile) {
            return emptyList()
        }

        val files = dir.listFiles() ?: return emptyList()
        return files.map {
            StoragePath("/" + it.toPath().relativeTo(root))
        }
    }

    override fun getFile(
        user: String,
        path: StoragePath
    ): InputStream {
        if (!path.file) {
            throw IllegalArgumentException("Path references folder")
        }

        val path = resolvePath(user, path)
        val file = path.toFile()
        if (!file.exists() || !file.isFile) {
            // FIXME: proper user facing error
            throw IllegalArgumentException("Path does not exists")
        }

        return Files.newInputStream(path, StandardOpenOption.READ)
    }

    override fun putFile(
        user: String,
        path: StoragePath,
        data: InputStream
    ) {
        if (!path.file) {
            throw IllegalArgumentException("Path references folder")
        }

        val path = resolvePath(user, path)

        Files.createDirectories(path.parent)
        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { fileStream ->
            data.use {
                it.transferTo(fileStream)
            }
        }
    }

    override fun delete(user: String, path: StoragePath) {
        val file = path.file
        val path = resolvePath(user, path)

        if (file) {
            Files.delete(path)
            // FIXME: error handling NoSuchFileException
        } else {
            FileUtils.deleteDirectory(path.toFile())
            // FIXME: error handling IllegalArgumentException
        }
    }
}