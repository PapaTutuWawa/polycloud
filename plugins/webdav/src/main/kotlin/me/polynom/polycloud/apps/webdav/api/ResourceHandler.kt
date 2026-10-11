package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.ContentTypes
import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavFile
import me.polynom.polycloud.apps.webdav.store.DavPath
import me.polynom.polycloud.apps.webdav.store.DavStore
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/** Handles OPTIONS, GET, HEAD, PUT, DELETE and MKCOL. */
@Component
class ResourceHandler(
    private val store: DavStore,
    private val lockManager: LockManager,
) {
    fun options(response: HttpServletResponse) {
        response.setHeader("DAV", "1, 2")
        response.setHeader(HttpHeaders.ALLOW, WebDavConstants.ALLOWED_METHODS)
        response.status = HttpStatus.OK.value()
    }

    fun get(
        path: String,
        response: HttpServletResponse,
        includeBody: Boolean,
    ) {
        val node = store.get(path)
        if (node == null) {
            fail(response, HttpStatus.NOT_FOUND.value())
            return
        }

        response.setHeader(HttpHeaders.ETAG, node.etag)
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, node.lastModified.toEpochMilli())
        response.status = HttpStatus.OK.value()
        when (node) {
            is DavFile -> {
                response.contentType = node.contentType
                response.setContentLengthLong(node.resource.contentLength())
                if (includeBody) {
                    node.resource.inputStream.use { it.copyTo(response.outputStream) }
                }
            }

            is DavCollection -> {
                response.contentType = "text/html; charset=UTF-8"
                if (includeBody) {
                    response.writer.write(renderCollectionListing(path))
                }
            }
        }
    }

    fun put(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        if (path == DavPath.ROOT) {
            throw DavException(HttpStatus.METHOD_NOT_ALLOWED.value(), "Cannot PUT the root collection")
        }
        if (store.get(path) is DavCollection) {
            throw DavException(HttpStatus.CONFLICT.value(), "Cannot PUT onto a collection")
        }
        if (store.get(DavPath.parent(path)) !is DavCollection) {
            throw DavException(HttpStatus.CONFLICT.value(), "Parent collection does not exist")
        }
        val existingNode = store.get(path)
        checkPrecondition(lockManager, request, path, existingNode?.etag)

        val existed = existingNode != null
        val contentType = ContentTypes.resolve(path, request.contentType)
        val file = store.putFile(path, request.inputStream.readBytes(), contentType)

        response.setHeader(HttpHeaders.ETAG, file.etag)
        response.status = if (existed) HttpStatus.NO_CONTENT.value() else HttpStatus.CREATED.value()
    }

    fun delete(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        if (path == DavPath.ROOT) {
            throw DavException(HttpStatus.FORBIDDEN.value(), "Cannot delete the root collection")
        }
        val node = store.get(path) ?: throw DavException(HttpStatus.NOT_FOUND.value())
        checkPrecondition(lockManager, request, path, node.etag)

        store.delete(path)
        response.status = HttpStatus.NO_CONTENT.value()
    }

    fun mkcol(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        if (request.contentLengthLong > 0) {
            throw DavException(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "MKCOL does not support a request body")
        }
        if (store.exists(path)) {
            throw DavException(HttpStatus.METHOD_NOT_ALLOWED.value(), "A resource already exists at this path")
        }
        if (store.get(DavPath.parent(path)) !is DavCollection) {
            throw DavException(HttpStatus.CONFLICT.value(), "Parent collection does not exist")
        }

        store.makeCollection(path)
        response.status = HttpStatus.CREATED.value()
    }

    private fun renderCollectionListing(path: String): String {
        val items =
            store
                .children(path)
                .mapNotNull { childPath -> store.get(childPath)?.let { childPath to it } }
                .joinToString(separator = "") { (childPath, childNode) ->
                    val suffix = if (childNode is DavCollection) "/" else ""
                    "<li><a href=\"${hrefFor(childPath, childNode)}\">${escapeHtml(DavPath.name(childPath))}$suffix</a></li>"
                }
        return "<!DOCTYPE html><html><body><ul>$items</ul></body></html>"
    }

    private fun escapeHtml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
