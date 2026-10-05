package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.exceptions.PathEmptyException
import me.polynom.polycloud.apps.files.persistence.repository.UploadRepository
import me.polynom.polycloud.apps.files.storage.EntryMeta
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
    fun listFiles(
        user: String,
        path: StoragePath,
    ): List<EntryMeta> {
        // If we list a directory, also retrieve mount points in that path
        val mounts =
            if (!path.file) {
                storageService.listMounts(path).map {
                    EntryMeta(it.components.last(), true, null)
                }
            } else {
                emptyList()
            }

        // If path resolves to a real storage backend, passthrough request
        val (storage, relPath) = storageService.resolveMount(path)
        val files = storage?.list(user, relPath) ?: emptyList()

        // Return both
        val ret = mounts + files
        if (ret.isEmpty()) {
            throw PathEmptyException(user, path)
        }

        return ret
    }

    fun getFile(
        user: String,
        path: StoragePath,
    ): Resource {
        val (storage, relPath) = storageService.resolveMount(path)
        // FIXME: user facing error
        storage ?: throw IllegalArgumentException("MOOP")
        return storage.read(user, relPath)
    }

    fun putFile(slot: UUID) {
        // FIXME: exception
        val upload = uploadRepository.findById(slot).orElseThrow { PathEmptyException("", StoragePath("")) }
        if (!upload.done()) {
            // FIXME: exception
            throw PathEmptyException("", StoragePath(""))
        }

        val (storage, _) = storageService.resolveMount(StoragePath(upload.path))
        // FIXME: user facing error
        storage ?: throw IllegalArgumentException("MOOP")

        storage.finaliseUpload(upload)
        uploadRepository.delete(upload)
    }

    fun delete(
        user: String,
        path: StoragePath,
    ) {
        val (storage, relPath) = storageService.resolveMount(path)
        // FIXME: user facing error
        storage ?: throw IllegalArgumentException("MOOP")
        storage.delete(user, relPath)
    }
}
