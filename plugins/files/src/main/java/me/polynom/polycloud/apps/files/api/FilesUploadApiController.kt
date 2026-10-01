package me.polynom.polycloud.apps.files.api

import jakarta.validation.constraints.PositiveOrZero
import me.polynom.polycloud.apps.files.api.dto.TusUploadMetadata
import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.constants.TusConstants
import me.polynom.polycloud.apps.files.constants.TusHeaders
import me.polynom.polycloud.apps.files.exceptions.InvalidUploadMetadataException
import me.polynom.polycloud.apps.files.service.UploadService
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream
import java.net.URI
import java.util.UUID

@RestController
@PluginEnabled
@RequestMapping("/api/apps/files")
class FilesUploadApiController(
    val uploadService: UploadService,
) {
    @PostMapping("/upload")
    fun createUpload(
        @RequestHeader(TusHeaders.UPLOAD_LENGTH)
        @PositiveOrZero(message = "Upload-Length must be a non-negative integer.")
        uploadLength: Long,
        @RequestHeader(TusHeaders.UPLOAD_METADATA)
        metadata: TusUploadMetadata,
    ) = ResponseEntity
        .created(uploadService.createUpload(uploadLength, metadata))
        .build<Unit>()

    @RequestMapping("/upload", method = [RequestMethod.OPTIONS])
    fun optUpload(): ResponseEntity<Unit> =
        ResponseEntity
            .noContent()
            .header(TusHeaders.TUS_VERSION, TusConstants.TUS_VERSION)
            .header(TusHeaders.TUS_EXTENSION, TusConstants.TUS_EXTENSIONS)
            .build()

    @RequestMapping("/upload/{slot}", method = [RequestMethod.OPTIONS])
    fun optUploadSlot() = optUpload()

    @RequestMapping("/upload/{slot}", method = [RequestMethod.HEAD])
    fun statUpload(
        @PathVariable("slot") slot: UUID,
    ): ResponseEntity<Unit> =
        ResponseEntity
            .noContent()
            .header(TusHeaders.UPLOAD_OFFSET, uploadService.retrieveOffset(slot).toString())
            .cacheControl(CacheControl.noStore())
            .build()

    @PatchMapping("/upload/{slot}", consumes = ["application/offset+octet-stream"])
    fun doUpload(
        @PathVariable("slot")
        slot: UUID,
        @RequestHeader(TusHeaders.UPLOAD_OFFSET)
        offset: Long,
        body: InputStream,
    ): ResponseEntity<Unit> =
        // FIXME: validate offset value is as expected (otherwise 409 Conflicted)
        ResponseEntity
            .noContent()
            .header(TusHeaders.UPLOAD_OFFSET, uploadService.patchUpload(slot, offset, body).toString())
            .build()

    @DeleteMapping("/upload/{slot}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUpload(
        @PathVariable("slot") slot: UUID,
    ) = uploadService.deleteUpload(slot)
}
