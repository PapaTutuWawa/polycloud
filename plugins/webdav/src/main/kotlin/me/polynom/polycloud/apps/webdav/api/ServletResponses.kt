package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavNode
import me.polynom.polycloud.apps.webdav.store.DavPath

/** Reads the full request body as a UTF-8 string. */
fun readBody(request: HttpServletRequest): String = request.inputStream.readBytes().decodeToString()

/** Sets [status] and, if [message] is given, writes it as a plain-text body. */
fun fail(
    response: HttpServletResponse,
    status: Int,
    message: String? = null,
) {
    response.status = status
    if (message != null) {
        response.contentType = "text/plain; charset=UTF-8"
        response.writer.write(message)
    }
}

/** Sets [status] and writes [xml] as an `application/xml` body. */
fun writeXml(
    response: HttpServletResponse,
    status: Int,
    xml: String,
) {
    response.status = status
    response.contentType = "application/xml; charset=UTF-8"
    response.writer.write(xml)
}

/** The `D:href` for [path], with a trailing slash when [node] is a collection. */
fun hrefFor(
    path: String,
    node: DavNode,
): String {
    val base = if (path == DavPath.ROOT) "${WebDavConstants.MOUNT}/" else "${WebDavConstants.MOUNT}$path"
    return if (node is DavCollection && !base.endsWith("/")) "$base/" else base
}
