package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.api.dto.TusUploadMetadata
import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.exceptions.InvalidUploadMetadataException
import me.polynom.polycloud.apps.files.exceptions.PathEmptyException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.persistence.repository.UploadRepository
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.springframework.stereotype.Service
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.InputStream
import java.net.URI
import java.util.UUID

@Service
@PluginEnabled
class UploadService(
    val uploadRepository: UploadRepository,
    val storageService: StorageService,
) {
    @Suppress("ThrowsCount")
    fun createUpload(
        uploadLength: Long,
        metadata: TusUploadMetadata,
    ): URI {
        // FIXME: validate user :) permissions? yes, we don't have 'em
        val user = metadata["user"] ?: throw InvalidUploadMetadataException("No user provided in metadata")
        val rawPath = metadata["path"] ?: throw InvalidUploadMetadataException("No path provided in metadata")
        // Convert to StoragePath for validation
        val path = StoragePath(rawPath)

        val (storage, _) = storageService.resolveMount(path)
        storage ?: throw PathEmptyException(user, path)

        val entity =
            Upload(
                user = user,
                path = path.toString(),
                size = uploadLength,
                offset = 0,
            )
        uploadRepository.save(entity)

        storage.stageUpload(entity)
        return URI("/api/apps/files/upload/${entity.id!!}")
    }

    fun deleteUpload(id: UUID) {
        // FIXME: exception
        val upload = uploadRepository.findById(id).orElseThrow { PathEmptyException("", StoragePath("")) }
        val (storage, _) = storageService.resolveMount(StoragePath(upload.path))
        storage?.deleteUpload(upload)
        uploadRepository.delete(upload)
    }

    fun retrieveOffset(id: UUID): Long {
        // FIXME: better exception
        val upload =
            uploadRepository.findById(id).orElseThrow {
                PathEmptyException("", StoragePath(""))
            }

        return upload.offset
    }

    fun patchUpload(
        id: UUID,
        offset: Long,
        body: InputStream,
    ): Long {
        // FIXME: exception
        val upload = uploadRepository.findById(id).orElseThrow { PathEmptyException("", StoragePath("")) }
        if (upload.offset != offset) {
            // FIXME: exception
            throw PathEmptyException("", StoragePath(""))
        }

        val (storage, _) = storageService.resolveMount(StoragePath(upload.path))
        // FIXME: exception
        storage ?: throw PathEmptyException(upload.user, StoragePath(upload.path))

        val transferred = storage.patchUpload(upload, body)
        upload.offset += transferred
        uploadRepository.save(upload)

        return upload.offset
    }
}
