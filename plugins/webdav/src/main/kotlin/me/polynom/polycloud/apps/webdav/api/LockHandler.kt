package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.DavStore
import me.polynom.polycloud.apps.webdav.xml.DavXml
import me.polynom.polycloud.apps.webdav.xml.DavXmlParser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Duration

/** Handles LOCK and UNLOCK (exclusive write locks only; shared locks and lock-null resources are not supported). */
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
        if (refreshed == null || refreshed.path != path || node == null) {
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
        val node = store.get(path)
        if (node == null) {
            fail(response, HttpStatus.NOT_FOUND.value(), "Lock-null resources are not supported")
            return
        }
        if (lockManager.isLockedByOther(path, emptySet())) {
            fail(response, HttpStatus.LOCKED.value())
            return
        }

        val owner = DavXmlParser.parseLockOwner(readBody(request))
        val depth = if (request.getHeader(DavHeaders.DEPTH) == "0") LockDepth.ZERO else LockDepth.INFINITY
        val lock = lockManager.lock(path, depth, owner, parseTimeout(request))

        response.setHeader(DavHeaders.LOCK_TOKEN, "<${lock.token}>")
        writeXml(response, HttpStatus.OK.value(), DavXml.buildLockDiscovery(lock, hrefFor(path, node)))
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
