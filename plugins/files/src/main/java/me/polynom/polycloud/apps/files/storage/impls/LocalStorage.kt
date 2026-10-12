package me.polynom.polycloud.apps.files.storage.impls

import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageQuotaException
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
import java.nio.file.FileAlreadyExistsException
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
import kotlin.math.min

private const val UPLOADS_FOLDER = ".me.polynom.polycloud.uploads"

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

    private fun resolveUpload(upload: Upload): Path = root.resolve(UPLOADS_FOLDER, upload.id!!.toString())

    private fun isUploadsPath(path: StoragePath) = path.components.isNotEmpty() && path.components[0] == UPLOADS_FOLDER

    override fun list(
        user: String,
        path: StoragePath,
    ): List<EntryMeta> {
        if (path.file) {
            throw StorageWrongTypeException(path, false)
        }

        // Convert storage path to real file on the fs
        val fsPath = resolvePath(user, path)
        if (!fsPath.exists() || !fsPath.isDirectory() || isUploadsPath(path)) {
            throw StoragePathDoesNotExistException(path)
        }

        return fsPath
            .listDirectoryEntries()
            .filter {
                !it.startsWith(root.resolve(UPLOADS_FOLDER))
            }.map {
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
        if (!fsPath.exists() || !fsPath.isRegularFile() || isUploadsPath(path)) {
            throw StoragePathDoesNotExistException(path)
        }

        return FileSystemResource(fsPath)
    }

    override fun delete(
        user: String,
        path: StoragePath,
    ) {
        val fsPath = resolvePath(user, path)

        if (!fsPath.exists() || fsPath.isRegularFile() != path.file || isUploadsPath(path)) {
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
        val vfsPath = upload.path
        if (!upload.path.file) {
            throw StorageWrongTypeException(vfsPath, true)
        }

        val relPath = vfsToRelative(vfsPath)
        if (isUploadsPath(relPath)) {
            throw StorageQuotaException(upload, 0)
        }

        val path = resolveUpload(upload)
        Files.createDirectories(path.parent)
        try {
            Files.createFile(path)
            upload.offset = 0
        } catch (ex: FileAlreadyExistsException) {
            throw IllegalStateException(ex)
        }
    }

    override fun continueUpload(
        upload: Upload,
        stream: InputStream,
    ): Long {
        val path = resolveUpload(upload)
        // Throw here to make sure we close the streams
        if (!path.exists()) {
            throw IllegalStateException("Upload has not been prepared.")
        }

        val fos = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        val bos = BufferedOutputStream(fos)

        // We make sure to only read as much data as the upload object is expecting.
        // If the user supplies more data, we just ignore it.
        bos.use { fileStream ->
            // FIXME: DoS, what if user spams parallel uploads? -> a lot of buffer allocations, have a user pool?
            val buffer = ByteArray(4096 * 16)
            var tx = 0L
            while (true) {
                val remaining = min(buffer.size.toLong(), upload.size - upload.offset)
                val read = stream.read(buffer, 0, remaining.toInt())
                if (read <= 0) {
                    break
                }

                fileStream.write(buffer, 0, read)

                upload.offset += read.toLong()
                tx += read.toLong()
            }

            return tx
        }
    }

    override fun finaliseUpload(upload: Upload) {
        val path = vfsToRelative(upload.path)
        val targetPath = resolvePath(upload.user, path)
        val sourcePath = resolveUpload(upload)

        if (!upload.done() || !sourcePath.exists()) {
            throw IllegalStateException("Upload is not done or upload has not been prepared")
        }

        Files.createDirectories(targetPath.parent)
        Files.move(sourcePath, targetPath)
    }

    override fun deleteUpload(upload: Upload) {
        val path = resolveUpload(upload)
        if (!path.exists()) {
            throw IllegalStateException("Upload has not been prepared")
        }
        Files.delete(path)
    }
}
