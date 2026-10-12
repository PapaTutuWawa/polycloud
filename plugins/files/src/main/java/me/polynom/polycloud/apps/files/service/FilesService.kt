package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageWrongTypeException
import me.polynom.polycloud.apps.files.exceptions.UploadDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.UploadIncompleteException
import me.polynom.polycloud.apps.files.persistence.repository.UploadRepository
import me.polynom.polycloud.apps.files.storage.EntryMeta
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@PluginEnabled
class FilesService(
    val storageService: StorageService,
    val uploadRepository: UploadRepository,
) {
    /**
     * List non-recursively all files and folders in the given path.
     * @param user which user is listing his files.
     * @param path vfs path of the folder.
     * @throws StoragePathDoesNotExistException if path is empty and no mounts are inside.
     * @throws StorageWrongTypeException if path is a file.
     * @throws * any exceptions defined in {@link Storage#list}
     * @return a list of metaobjects describing the files and directories.
     */
    fun list(
        user: String,
        path: StoragePath,
    ): List<EntryMeta> {
        if (path.file) {
            throw StorageWrongTypeException(path, false)
        }

        // If we list a directory, also retrieve mount points in that path
        val mounts =
            storageService
                .listMounts(path)
                .map {
                    EntryMeta(it.components.last(), true, null)
                }.toSet()

        // If path resolves to a real storage backend, passthrough request
        val (storage, relPath) = storageService.resolveMount(path)

        // List files in that folder
        val files = listBackend(storage, user, relPath, hasMounts = mounts.isNotEmpty())
        // Combine both sets
        return (mounts + files).toList()
    }

    private fun listBackend(
        storage: Storage?,
        user: String,
        path: StoragePath,
        hasMounts: Boolean,
    ): Set<EntryMeta> =
        try {
            (
                storage?.list(user, path)
                    ?: throw StoragePathDoesNotExistException(path)
            ).toSet()
        } catch (ex: StoragePathDoesNotExistException) {
            if (hasMounts) emptySet() else throw ex
        }

    /**
     * Returns a resource that can be used to stream the file's contents.
     * @param user which user is reading a file.
     * @param path vfs path of the file.
     * @throws StoragePathDoesNotExistException if path is on no backend.
     * @throws * any exceptions defined in {@link Storage#read}
     * @return the resource describing the file.
     */
    fun read(
        user: String,
        path: StoragePath,
    ): Resource {
        val (storage, relPath) = storageService.resolveMount(path)
        storage ?: throw StoragePathDoesNotExistException(path)
        return storage.read(user, relPath)
    }

    /**
     * Finalizes an upload by putting the uploaded file in the correct place.
     * @param slot the UUID of the upload to finalize.
     * @throws UploadDoesNotExistException if slot is not a valid upload.
     * @throws UploadIncompleteException if the upload has not received all data yet.
     * @throws StoragePathDoesNotExistException if path is on no backend.
     * @throws * any exceptions defined in {@link Storage#finaliseUpload}
     */
    fun finaliseUpload(slot: UUID) {
        val upload =
            uploadRepository.findById(slot).orElseThrow {
                UploadDoesNotExistException(slot)
            }

        if (!upload.done()) {
            throw UploadIncompleteException(upload)
        }

        val (storage, _) = storageService.resolveMount(upload.path)
        storage ?: throw StoragePathDoesNotExistException(upload.path)

        storage.finaliseUpload(upload)
        uploadRepository.delete(upload)
    }

    /**
     * Deletes a file or folder in the VFS.
     * @param user which user is deleting a file or folder.
     * @param path vfs path of the file or folder.
     * @throws StoragePathDoesNotExistException if path is on no storage backend.
     * @throws * any exceptions defined in {@link Storage#delete}
     */
    fun delete(
        user: String,
        path: StoragePath,
    ) {
        val (storage, relPath) = storageService.resolveMount(path)
        storage ?: throw StoragePathDoesNotExistException(path)
        storage.delete(user, relPath)
    }
}
