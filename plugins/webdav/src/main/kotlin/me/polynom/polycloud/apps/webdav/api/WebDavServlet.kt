package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.autoconfigure.PluginEnabled
import org.springframework.stereotype.Component

/**
 * Raw servlet fronting the WebDAV surface. A plain [HttpServlet] is used instead of a Spring MVC
 * `@RestController` because WebDAV's extension HTTP methods (PROPFIND, MKCOL, COPY, MOVE, LOCK,
 * UNLOCK, ...) and its XML/header-heavy semantics are a poor fit for annotation-based routing.
 */
@PluginEnabled
@Component
class WebDavServlet(
    private val handler: WebDavHandler,
) : HttpServlet() {
    override fun service(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) = handler.handle(request, response)
}
