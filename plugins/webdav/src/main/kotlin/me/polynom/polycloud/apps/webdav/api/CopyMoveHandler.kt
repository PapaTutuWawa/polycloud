package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavPath
import me.polynom.polycloud.apps.webdav.store.DavStore
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLDecoder

/** Handles COPY and MOVE. */
@Component
class CopyMoveHandler(
    private val store: DavStore,
    private val lockManager: LockManager,
) {
    fun copy(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        val destination = requireDestination(request)
        if (!store.exists(path)) {
            throw DavException(HttpStatus.NOT_FOUND.value())
        }
        validateDestination(request, path, destination)

        val shallow = request.getHeader(DavHeaders.DEPTH) == "0"
        val replaced = store.copy(path, destination, shallow)
        response.status = if (replaced) HttpStatus.NO_CONTENT.value() else HttpStatus.CREATED.value()
    }

    fun move(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        val destination = requireDestination(request)
        val sourceNode = store.get(path) ?: throw DavException(HttpStatus.NOT_FOUND.value())
        if (sourceNode is DavCollection && request.getHeader(DavHeaders.DEPTH) == "0") {
            throw DavException(HttpStatus.BAD_REQUEST.value(), "MOVE of a collection requires Depth: infinity")
        }
        validateDestination(request, path, destination)
        if (lockManager.isLockedByOther(path, LockManager.parseIfHeaderTokens(request.getHeader(DavHeaders.IF)))) {
            throw DavException(HttpStatus.LOCKED.value(), "Source is locked")
        }

        val replaced = store.move(path, destination)
        response.status = if (replaced) HttpStatus.NO_CONTENT.value() else HttpStatus.CREATED.value()
    }

    private fun requireDestination(request: HttpServletRequest): String =
        destinationPath(request) ?: throw DavException(HttpStatus.BAD_REQUEST.value(), "Missing or invalid Destination header")

    private fun validateDestination(
        request: HttpServletRequest,
        source: String,
        destination: String,
    ) {
        if (destination == source) {
            throw DavException(HttpStatus.FORBIDDEN.value(), "Source and destination are the same")
        }
        if (DavPath.isAncestorOf(source, destination)) {
            throw DavException(HttpStatus.FORBIDDEN.value(), "Cannot copy or move a collection into itself")
        }
        if (store.get(DavPath.parent(destination)) !is DavCollection) {
            throw DavException(HttpStatus.CONFLICT.value(), "Destination's parent collection does not exist")
        }

        val overwrite = request.getHeader(DavHeaders.OVERWRITE)?.uppercase() != "F"
        if (store.exists(destination) && !overwrite) {
            throw DavException(HttpStatus.PRECONDITION_FAILED.value(), "Destination exists and Overwrite is F")
        }
        if (lockManager.isLockedByOther(destination, LockManager.parseIfHeaderTokens(request.getHeader(DavHeaders.IF)))) {
            throw DavException(HttpStatus.LOCKED.value(), "Destination is locked")
        }
    }

    private fun destinationPath(request: HttpServletRequest): String? {
        val header = request.getHeader(DavHeaders.DESTINATION) ?: return null
        val rawPath = runCatching { URI(header).rawPath }.getOrNull() ?: header
        val decoded = URLDecoder.decode(rawPath, Charsets.UTF_8)
        return DavPath.normalize(decoded.removePrefix(WebDavConstants.MOUNT))
    }
}
