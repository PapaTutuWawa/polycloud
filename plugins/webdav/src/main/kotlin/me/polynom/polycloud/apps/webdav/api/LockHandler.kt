package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavPath
import me.polynom.polycloud.apps.webdav.store.DavStore
import me.polynom.polycloud.apps.webdav.xml.DavXml
import me.polynom.polycloud.apps.webdav.xml.DavXmlParser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Duration

/** Handles LOCK and UNLOCK. */
@Component
class LockHandler(
    private val store: DavStore,
    private val lockManager: LockManager,
) {
    fun lock(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        val refreshToken = LockManager.parseIfHeaderTokens(request.getHeader(DavHeaders.IF)).firstOrNull()
        if (refreshToken != null) {
            refreshLock(response, path, refreshToken)
            return
        }

        createLock(request, response, path)
    }

    fun unlock(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        val token = LockManager.parseLockTokenHeader(request.getHeader(DavHeaders.LOCK_TOKEN))
        if (token == null) {
            fail(response, HttpStatus.BAD_REQUEST.value(), "Missing Lock-Token header")
            return
        }
        if (!lockManager.unlock(path, token)) {
            fail(response, HttpStatus.CONFLICT.value(), "No such lock")
            return
        }

        response.status = HttpStatus.NO_CONTENT.value()
    }

    private fun refreshLock(
        response: HttpServletResponse,
        path: String,
        token: String,
    ) {
        val refreshed = lockManager.refresh(token)
        val node = store.get(path)
        if (refreshed == null || node == null || !refreshed.covers(path, DavPath::isAncestorOf)) {
            fail(response, HttpStatus.PRECONDITION_FAILED.value(), "No such lock")
            return
        }

        response.setHeader(DavHeaders.LOCK_TOKEN, "<${refreshed.token}>")
        writeXml(response, HttpStatus.OK.value(), DavXml.buildLockDiscovery(refreshed, hrefFor(path, node)))
    }

    private fun createLock(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        val existed = store.exists(path)
        if (!existed && !createLockNullResource(path)) {
            fail(response, HttpStatus.CONFLICT.value(), "Parent collection does not exist")
            return
        }
        val node = store.get(path) ?: error("Resource at $path vanished right after being created")

        val body = readBody(request)
        val scope = parseXmlBody { DavXmlParser.parseLockScope(body) }
        if (!lockManager.canAcquire(path, scope)) {
            fail(response, HttpStatus.LOCKED.value())
            return
        }

        val owner = parseXmlBody { DavXmlParser.parseLockOwner(body) }
        val depth = if (request.getHeader(DavHeaders.DEPTH) == "0") LockDepth.ZERO else LockDepth.INFINITY
        val lock = lockManager.lock(path, depth, scope, owner, parseTimeout(request))

        response.setHeader(DavHeaders.LOCK_TOKEN, "<${lock.token}>")
        val status = if (existed) HttpStatus.OK.value() else HttpStatus.CREATED.value()
        writeXml(response, status, DavXml.buildLockDiscovery(lock, hrefFor(path, node)))
    }

    /** LOCK on an unmapped URL creates an empty "lock-null" resource (RFC 4918 section 7.3). */
    private fun createLockNullResource(path: String): Boolean {
        if (store.get(DavPath.parent(path)) !is DavCollection) {
            return false
        }

        store.putFile(path, ByteArray(0), "application/octet-stream")
        return true
    }

    private fun parseTimeout(request: HttpServletRequest): Duration {
        val header = request.getHeader(DavHeaders.TIMEOUT) ?: return LockManager.DEFAULT_TIMEOUT
        val seconds =
            header
                .split(",")
                .first()
                .trim()
                .removePrefix("Second-")
                .toLongOrNull()
        return if (seconds != null) Duration.ofSeconds(seconds) else LockManager.MAX_TIMEOUT
    }
}
