package me.polynom.polycloud.apps.files.storage

import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageQuotaException
import me.polynom.polycloud.apps.files.exceptions.StorageReadOnlyException
import me.polynom.polycloud.apps.files.exceptions.StorageWrongTypeException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import org.springframework.core.io.Resource
import java.io.InputStream

@Suppress("TooManyFunctions")
interface Storage {
    /**
     * The path in the VFS where this backend is mounted.
     */
    val mount: StoragePath

    /**
     * List non-recursively all files and folders in the given path.
     * @param user which user is listing his files.
     * @param path backend-relative path of the folder.
     * @throws StoragePathDoesNotExistException if path does not exist.
     * @throws StorageWrongTypeException if path is a file.
     * @throws java.io.IOError on io errors.
     * @return a list of metaobjects describing the files and directories.
     */
    fun list(
        user: String,
        path: StoragePath,
    ): List<EntryMeta>

    /**
     * Returns a resource that can be used to stream the file's contents.
     * @param user which user is reading a file.
     * @param path backend-relative path of the file.
     * @throws StoragePathDoesNotExistException if path does not exist.
     * @throws StorageWrongTypeException if path is a folder.
     * @throws java.io.IOError on io errors.
     * @return the resource describing the file.
     */
    fun read(
        user: String,
        path: StoragePath,
    ): Resource

    /**
     * Deletes a file *or* folder in the backend.
     * @param user which user is deleting a file or folder.
     * @param path backend-relative path of the file or folder.
     * @throws StoragePathDoesNotExistException if path does not exist
     * @throws StorageReadOnlyException if backend is read-only
     * @throws java.io.IOError on io errors
     */
    fun delete(
        user: String,
        path: StoragePath,
    )

    /**
     * Prepares an upload (by e.g. creating a temporary file).
     * Needs to be called before continueUpload()/deleteUpload().
     * If method fails, deleteUpload() must not be called.
     * @param upload metadata object.
     * @throws StorageQuotaException if storage quota is reached
     * @throws StorageReadOnlyException if backend is read-only
     * @throws StorageWrongTypeException if path is folder path.
     * @throws java.io.IOError on io errors
     * @throws IllegalStateException if upload is already prepared.
     */
    fun prepareUpload(upload: Upload)

    /**
     * Uploads the file via providing data in streams.
     * The caller is responsible for closing the input stream.
     * @param upload metadata object.
     * @param stream a stream containing file data.
     * @return how many bytes were committed to this backend. 0 if the upload is already done.
     * @throws IllegalStateException if called before prepareUpload()
     * @throws java.io.IOError on io errors
     */
    fun continueUpload(
        upload: Upload,
        stream: InputStream,
    ): Long

    /**
     * Finalizes an upload by moving the file to its intended location.
     * @param upload metadata object.
     * @throws IllegalStateException if called before prepareUpload() or upload is not done
     * @throws java.io.IOError on io errors
     */
    fun finaliseUpload(upload: Upload)

    /**
     * Deletes a pending upload, removing any temporary objects/files.
     * @param upload metadata object.
     * @throws IllegalStateException if called before prepareUpload()
     * @throws java.io.IOError on io errors
     */
    fun deleteUpload(upload: Upload)

    /**
     * Probes [path] without assuming beforehand whether it is a file or a folder (unlike [list]
     * and [read], which require the caller to already know). The `file` flag on [path] is ignored.
     * @param user which user is probing the path.
     * @param path backend-relative path to probe.
     * @return metadata describing whatever is at [path], or null if nothing exists there.
     */
    fun stat(
        user: String,
        path: StoragePath,
    ): StorageStat?

    /**
     * Creates an empty folder at [path].
     * @param user which user is creating the folder.
     * @param path backend-relative path of the new folder.
     * @throws StoragePathDoesNotExistException if the parent folder does not exist.
     * @throws StorageReadOnlyException if backend is read-only.
     */
    fun createDirectory(
        user: String,
        path: StoragePath,
    )

    /**
     * Copies the file or folder (recursively) at [source] to [destination], within this backend.
     * Any existing file/folder at [destination] is replaced.
     * @param user which user is copying.
     * @throws StoragePathDoesNotExistException if source, or destination's parent, does not exist.
     * @throws StorageReadOnlyException if backend is read-only.
     */
    fun copy(
        user: String,
        source: StoragePath,
        destination: StoragePath,
    )

    /**
     * Moves the file or folder at [source] to [destination], within this backend. Any existing
     * file/folder at [destination] is replaced.
     * @param user which user is moving.
     * @throws StoragePathDoesNotExistException if source, or destination's parent, does not exist.
     * @throws StorageReadOnlyException if backend is read-only.
     */
    fun move(
        user: String,
        source: StoragePath,
        destination: StoragePath,
    )

    /**
     * Converts a path in the vfs to a relative path that the backend can use to manage files.
     * @throws IllegalArgumentException if path is not a subpath of `mount`.
     */
    fun vfsToRelative(path: StoragePath): StoragePath {
        val (base, relative) = path.split(mount.folderDepth())

        if (base != mount) {
            throw IllegalArgumentException("$path is not a subpath of $mount")
        }

        return relative
    }
}
