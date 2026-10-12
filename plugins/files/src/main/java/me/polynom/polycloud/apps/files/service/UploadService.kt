package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.UploadDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.UploadWrongOffsetException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.persistence.repository.UploadRepository
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.UUID

/**
 * Manages resumable uploads and tracks current upload progress in database.
 */
@Service
@PluginEnabled
class UploadService(
    val uploadRepository: UploadRepository,
    val storageService: StorageService,
) {
    /**
     * Registers/Creates a new upload in the database and prepares the storage backends to receive data.
     * @param upload the newly created upload object to be prepared and saved in db.
     * @throws StoragePathDoesNotExistException if path points to no storage backend.
     * @throws * any exceptions defined in {@link Storage#prepareUpload}
     * @return the new upload object.
     */
    fun createUpload(upload: Upload): Upload {
        val (storage, _) = storageService.resolveMount(upload.path)
        storage ?: throw StoragePathDoesNotExistException(upload.path)

        val ret = uploadRepository.save(upload)
        storage.prepareUpload(upload)
        return ret
    }

    /**
     * Delete this upload object.
     * If the storage backend of the upload silently disappeared between restarts
     * this method won't fail and just deletes the db entry of the upload.
     * @param id of the upload.
     * @throws UploadDoesNotExistException if id is an invalid upload id.
     * @throws * any exceptions defined in {@link Storage#deleteUpload}
     */
    fun deleteUpload(id: UUID) {
        val upload =
            uploadRepository.findById(id).orElseThrow {
                UploadDoesNotExistException(id)
            }
        val (storage, _) = storageService.resolveMount(upload.path)
        storage?.deleteUpload(upload)
        uploadRepository.delete(upload)
    }

    /**
     * Retrieves the offset the specified upload.
     * New upload data should be sent starting from this offset.
     * @param id of the upload.
     * @throws UploadDoesNotExistException if id is an invalid upload id.
     * @return the current byte offset of the upload.
     */
    fun retrieveOffset(id: UUID): Long {
        val upload =
            uploadRepository.findById(id).orElseThrow {
                UploadDoesNotExistException(id)
            }
        return upload.offset
    }

    /**
     * Continues a file upload at a specified offset.
     * @param id of the upload to continue.
     * @param offset byte-offset to start data upload at.
     * @param body the actual data.
     * @throws UploadDoesNotExistException if id is an invalid upload id.
     * @throws UploadWrongOffsetException if the provided offset does not match the expected byte offset.
     * @throws StoragePathDoesNotExistException if the storage backend of the upload disappeared.
     * @throws * any exceptions defined in {@link Storage#continueUpload}
     * @return the new byte-offset for future data uploads, i.e. an indication how many byte where committed to storage.
     */
    fun continueUpload(
        id: UUID,
        offset: Long,
        body: InputStream,
    ): Long {
        val upload =
            uploadRepository.findById(id).orElseThrow {
                throw UploadDoesNotExistException(id)
            }
        if (upload.offset != offset) {
            throw UploadWrongOffsetException(upload, offset)
        }

        val (storage, _) = storageService.resolveMount(upload.path)
        storage ?: throw StoragePathDoesNotExistException(upload.path)

        body.use { body ->
            storage.continueUpload(upload, body)
        }
        uploadRepository.save(upload)

        return upload.offset
    }
}
