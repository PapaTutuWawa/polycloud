package me.polynom.polycloud.apps.files.api

import jakarta.servlet.http.HttpServletRequest
import me.polynom.polycloud.apps.files.api.dto.UploadSlotDto
import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.mappers.EntryMetaMapper
import me.polynom.polycloud.apps.files.service.FilesService
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.apache.tomcat.util.http.fileupload.FileUpload
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRange
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.UUID

@RestController
@PluginEnabled
@RequestMapping("/api/apps/files")
class FilesApiController(
    val filesService: FilesService,
    val entryMetaMapper: EntryMetaMapper,
) {
    @GetMapping("/users/{user}/{*path}")
    fun listFiles(
        @PathVariable("user") user: String,
        @PathVariable("path") path: StoragePath,
    ) = filesService.listFiles(user, path).map { entryMetaMapper.entryMetaToEntryMetaDto(it) }

    @DeleteMapping("/users/{user}/{*path}")
    fun delete(
        @PathVariable("user") user: String,
        @PathVariable("path") path: StoragePath,
    ) = filesService.delete(user, path)

    @PutMapping("/users/{user}/{*path}")
    fun putFile(
        @PathVariable("user") user: String,
        @PathVariable("path") path: StoragePath,
        @RequestBody slot: UploadSlotDto,
    ) = filesService.putFile(user, path, UUID.fromString(slot.slot))

    @GetMapping("/download/{user}/{*path}")
    fun download(
        @PathVariable("user") user: String,
        @PathVariable("path") path: StoragePath,
    ): ResponseEntity<Resource> {
        val resource: Resource = filesService.getFile(user, path)
        val lastModified = resource.lastModified()
        val etag = "${lastModified.toString(16)}-${resource.contentLength().toString(16)}"

        val contentDisposition =
            ContentDisposition
                .attachment()
                .filename(path.components.last())
                .build()

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .eTag(etag)
            .body(resource)
    }
}
