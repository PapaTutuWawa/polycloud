package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.store.DavPath
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/** Dispatches a WebDAV request to the handler responsible for its HTTP method. */
@Component
class WebDavHandler(
    resourceHandler: ResourceHandler,
    copyMoveHandler: CopyMoveHandler,
    propertyHandler: PropertyHandler,
    lockHandler: LockHandler,
) {
    private val dispatch: Map<String, (HttpServletRequest, HttpServletResponse, String) -> Unit> =
        mapOf(
            "OPTIONS" to { _, response, _ -> resourceHandler.options(response) },
            "GET" to { _, response, path -> resourceHandler.get(path, response, includeBody = true) },
            "HEAD" to { _, response, path -> resourceHandler.get(path, response, includeBody = false) },
            "PUT" to resourceHandler::put,
            "DELETE" to resourceHandler::delete,
            "MKCOL" to resourceHandler::mkcol,
            "COPY" to copyMoveHandler::copy,
            "MOVE" to copyMoveHandler::move,
            "PROPFIND" to propertyHandler::propfind,
            "PROPPATCH" to propertyHandler::proppatch,
            "LOCK" to lockHandler::lock,
            "UNLOCK" to lockHandler::unlock,
        )

    fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val action = dispatch[request.method.uppercase()]
        if (action == null) {
            fail(response, HttpStatus.METHOD_NOT_ALLOWED.value())
            return
        }

        val path = DavPath.normalize(request.pathInfo ?: DavPath.ROOT)
        try {
            action(request, response, path)
        } catch (e: DavException) {
            fail(response, e.status, e.message)
        }
    }
}
