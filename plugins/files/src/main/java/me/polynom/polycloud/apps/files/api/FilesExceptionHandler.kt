package me.polynom.polycloud.apps.files.api

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.constants.TusConstants
import me.polynom.polycloud.apps.files.constants.TusHeaders
import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageQuotaException
import me.polynom.polycloud.apps.files.exceptions.StorageReadOnlyException
import me.polynom.polycloud.apps.files.exceptions.StorageWrongTypeException
import me.polynom.polycloud.apps.files.exceptions.TusInvalidUploadMetadataException
import me.polynom.polycloud.apps.files.exceptions.TusWrongVersionException
import me.polynom.polycloud.apps.files.exceptions.UploadDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.UploadIncompleteException
import me.polynom.polycloud.apps.files.exceptions.UploadWrongOffsetException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.io.FileNotFoundException

@RestControllerAdvice(assignableTypes = [FilesApiController::class, FilesUploadApiController::class])
@PluginEnabled
class FilesExceptionHandler {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(StoragePathDoesNotExistException::class)
    fun handleStoragePathDoesNotExist() = Unit

    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    @ExceptionHandler(StorageQuotaException::class)
    fun handleStorageQuota() = Unit

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(StorageReadOnlyException::class)
    fun handleStorageReadOnly() = Unit

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(StorageWrongTypeException::class)
    fun handleStorageWrongType() = Unit

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TusInvalidUploadMetadataException::class)
    fun handleInvalidUploadMetadata() = Unit

    @ExceptionHandler(TusWrongVersionException::class)
    fun handleMissingHeader(ex: TusWrongVersionException): ResponseEntity<String> =
        ResponseEntity
            .status(HttpStatus.PRECONDITION_FAILED)
            .header(TusHeaders.TUS_VERSION, TusConstants.TUS_VERSION)
            .body(ex.message)

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UploadDoesNotExistException::class)
    fun handleUploadDoesNotExist() = Unit

    @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
    @ExceptionHandler(UploadIncompleteException::class)
    fun handleUploadIncomplete() = Unit

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UploadWrongOffsetException::class)
    fun handleUploadWrongOffset() = Unit
}
