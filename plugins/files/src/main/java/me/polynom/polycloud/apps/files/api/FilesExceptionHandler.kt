package me.polynom.polycloud.apps.files.api

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.constants.TusConstants
import me.polynom.polycloud.apps.files.constants.TusHeaders
import me.polynom.polycloud.apps.files.exceptions.InvalidUploadMetadataException
import me.polynom.polycloud.apps.files.exceptions.PathEmptyException
import me.polynom.polycloud.apps.files.exceptions.WrongTusVersionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [FilesApiController::class, FilesUploadApiController::class])
@PluginEnabled
class FilesExceptionHandler {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PathEmptyException::class)
    fun handlePathEmpty() = Unit

    @ExceptionHandler(WrongTusVersionException::class)
    fun handleMissingHeader(ex: WrongTusVersionException): ResponseEntity<String> =
        ResponseEntity
            .status(HttpStatus.PRECONDITION_FAILED)
            .header(TusHeaders.TUS_VERSION, TusConstants.TUS_VERSION)
            .body(ex.message)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidUploadMetadataException::class)
    fun handleInvalidUploadMetadata() = Unit
}
