package me.polynom.polycloud.apps.files.api

import jakarta.servlet.http.HttpServletRequest
import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.mappers.EntryMetaMapper
import me.polynom.polycloud.apps.files.service.FilesService
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.apache.tomcat.util.http.fileupload.FileUpload
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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

    @PutMapping("/users/{user}/{*path}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @PathVariable("user") user: String,
        @PathVariable("path") path: StoragePath,
        request: HttpServletRequest,
    ) {
        val items = FileUpload().getItemIterator(ServletRequestContext(request))
        while (items.hasNext()) {
            val item = items.next()
            if (!item.isFormField && item.fieldName == "file") {
                filesService.putFile(user, path, item.openStream())
                return
            }
        }
        // FIXME: user facing error
        throw IllegalArgumentException("Missing file part")
    }

    @DeleteMapping("/users/{user}/{*path}")
    fun delete(
        @PathVariable("user") user: String,
        @PathVariable("path") path: StoragePath,
    ) {
        filesService.delete(user, path)
    }
}
