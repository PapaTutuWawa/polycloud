package me.polynom.polycloud.apps.files.storage.impls

import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageWrongTypeException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.storage.EntryMeta
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import java.io.BufferedOutputStream
import java.io.IOError
import java.io.IOException
import java.io.InputStream
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class LocalStorage(
    override val mount: StoragePath,
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

    @Suppress("ReturnCount")
    override fun list(
        user: String,
        path: StoragePath,
    ): List<EntryMeta> {
        if (path.file) {
            throw StorageWrongTypeException(path, true)
        }

        // Convert storage path to real file on the fs
        val fsPath = resolvePath(user, path)
        if (!fsPath.exists() || !fsPath.isDirectory()) {
            throw StoragePathDoesNotExistException(path)
        }

        return fsPath
            .listDirectoryEntries()
            .map {
                val size = if (it.isRegularFile()) it.fileSize() else null
                EntryMeta(it.name, !it.isRegularFile(), size)
            }
    }

    override fun read(
        user: String,
        path: StoragePath,
    ): Resource {
        if (!path.file) {
            throw StorageWrongTypeException(path, true)
        }

        val fsPath = resolvePath(user, path)
        if (!fsPath.exists() || !fsPath.isRegularFile()) {
            throw StoragePathDoesNotExistException(path)
        }

        return FileSystemResource(fsPath)
    }

    override fun delete(
        user: String,
        path: StoragePath,
    ) {
        val fsPath = resolvePath(user, path)

        if (!fsPath.exists() || fsPath.isRegularFile() != path.file) {
            throw StoragePathDoesNotExistException(path)
        }

        if (path.file) {
            deleteFile(path, fsPath)
        } else {
            deleteFolderRecursively(fsPath)
        }
    }

    fun deleteFile(
        path: StoragePath,
        fsPath: Path,
    ) {
        try {
            fsPath.deleteExisting()
        } catch (_: NoSuchFileException) {
            throw StoragePathDoesNotExistException(path)
        } catch (ex: AccessDeniedException) {
            throw IOError(ex)
        }
    }

    fun deleteFolderRecursively(fsPath: Path) {
        // FIXME: this is very suboptimal right now, also needs more tests
        if (!fsPath.toFile().deleteRecursively()) {
            throw IOException()
        }
    }

    override fun prepareUpload(upload: Upload) {
        val path = root.resolve("uploads", upload.id!!.toString())
        Files.createDirectories(path.parent)
        Files.createFile(path)
    }

    override fun continueUpload(
        upload: Upload,
        stream: InputStream,
    ): Long {
        val path = root.resolve("uploads", upload.id!!.toString())
        val fos = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        val bos = BufferedOutputStream(fos)
        bos.use { fileStream ->
            stream.use {
                // FIXME: what if client sends more data than we expect?
                // error out please
                return it.transferTo(fileStream)
            }
        }
    }

    override fun finaliseUpload(upload: Upload) {
        val path = vfsToRelative(StoragePath(upload.path))
        if (!path.file) {
            throw IllegalArgumentException("Path references folder")
        }
        val targetPath = resolvePath(upload.user, path)
        val sourcePath = root.resolve("uploads", upload.id!!.toString())
        Files.createDirectories(targetPath.parent)
        Files.move(sourcePath, targetPath)
    }

    override fun deleteUpload(upload: Upload) {
        val path = root.resolve("uploads", upload.id!!.toString())
        Files.delete(path)
    }
}
