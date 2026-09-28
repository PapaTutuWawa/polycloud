package me.polynom.polycloud.apps.files.storage.impls

import me.polynom.polycloud.apps.files.exceptions.PathEmptyException
import me.polynom.polycloud.apps.files.storage.EntryMeta
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import java.io.BufferedOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class LocalStorage(
    val root: Path,
    val shared: Boolean,
) : Storage {
    private fun resolvePath(
        user: String,
        path: StoragePath,
    ): Path =
        if (shared) {
            root.resolve(path.toString().substring(1))
        } else {
            root.resolve(user, path.toString().substring(1))
        }

    override fun listFiles(
        user: String,
        path: StoragePath,
    ): List<EntryMeta> {
        // Convert storage path to real file on the fs
        val fsPath = resolvePath(user, path)
        val file = fsPath.toFile()
        // If file/directory does not exist return empty list
        if (!file.exists()) {
            return emptyList()
        }

        // If file exists, return singular item
        // If directory exists, return all files inside
        if (file.isFile) {
            return listOf(EntryMeta(file.name, false, file.length()))
        } else {
            val files = file.listFiles() ?: return emptyList()
            return files.map {
                val size = if (file.isFile) it.length() else null;
                EntryMeta(file.name, file.isDirectory, size)
            }
        }
    }

    override fun getFile(
        user: String,
        path: StoragePath,
    ): Resource {
        if (!path.file) {
            throw IllegalArgumentException("Path references folder")
        }

        val fsPath = resolvePath(user, path)
        val file = fsPath.toFile()
        if (!file.exists() || !file.isFile) {
            throw PathEmptyException(user, path)
        }

        return FileSystemResource(file)
    }

    override fun putFile(
        user: String,
        path: StoragePath,
        data: InputStream,
    ) {
        if (!path.file) {
            throw IllegalArgumentException("Path references folder")
        }

        val path = resolvePath(user, path)

        Files.createDirectories(path.parent)
        val fos = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        val bos = BufferedOutputStream(fos)
        bos.use { fileStream ->
            data.use {
                it.transferTo(fileStream)
            }
        }
    }

    override fun delete(
        user: String,
        path: StoragePath,
    ) {
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
