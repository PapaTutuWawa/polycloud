package me.polynom.polycloud.apps.files.api

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.constants.TusConstants
import me.polynom.polycloud.apps.files.constants.TusHeaders
import me.polynom.polycloud.apps.files.exceptions.TusInvalidUploadMetadataException
import me.polynom.polycloud.apps.files.exceptions.TusWrongVersionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [FilesApiController::class, FilesUploadApiController::class])
@PluginEnabled
class FilesExceptionHandler {
    @ExceptionHandler(TusWrongVersionException::class)
    fun handleMissingHeader(ex: TusWrongVersionException): ResponseEntity<String> =
        ResponseEntity
            .status(HttpStatus.PRECONDITION_FAILED)
            .header(TusHeaders.TUS_VERSION, TusConstants.TUS_VERSION)
            .body(ex.message)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TusInvalidUploadMetadataException::class)
    fun handleInvalidUploadMetadata() = Unit
}
