package me.polynom.polycloud.apps.files.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.constants.TusConstants
import me.polynom.polycloud.apps.files.constants.TusHeaders
import me.polynom.polycloud.apps.files.exceptions.WrongTusVersionException
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [FilesUploadApiController::class])
@PluginEnabled
class TusVersionHeaders {
    @ModelAttribute
    fun checkTusVersion(request: HttpServletRequest, response: HttpServletResponse) {
        // We only support one version, so make sure to set that header
        response.setHeader(TusHeaders.TUS_RESUMABLE, TusConstants.TUS_VERSION)

        // Versioning ignored in OPTIONS request
        if (request.method == RequestMethod.OPTIONS.toString()) {
            return;
        }
        // If the client did not send that exact version, throw
        val clientVersion = request.getHeader(TusHeaders.TUS_RESUMABLE) ?: ""
        if (clientVersion != TusConstants.TUS_VERSION) {
            throw WrongTusVersionException(clientVersion)
        }
    }
}