package me.polynom.polycloud.apps.webdav.api

import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.InMemoryDavStore
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LockHandlerTest {
    private fun newHandler(): Triple<LockHandler, InMemoryDavStore, LockManager> {
        val store = InMemoryDavStore()
        val lockManager = LockManager()
        return Triple(LockHandler(store, lockManager), store, lockManager)
    }

    @Test
    fun test_lock_existingResource_returnsLockTokenAndDiscoveryBody() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a.txt")

        assertEquals(HttpStatus.OK.value(), response.status)
        assertNotNull(response.getHeader(DavHeaders.LOCK_TOKEN))
        assertTrue(response.contentAsString.contains("<D:lockdiscovery>"))
    }

    @Test
    fun test_lock_missingResource_is404() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("LOCK", "/dav/missing")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/missing")

        assertEquals(HttpStatus.NOT_FOUND.value(), response.status)
    }

    @Test
    fun test_lock_alreadyLockedByAnother_isLocked() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, owner = "bob")
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a.txt")

        assertEquals(HttpStatus.LOCKED.value(), response.status)
    }

    @Test
    fun test_lock_parsesOwnerFromBody() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        request.setContent(
            """<?xml version="1.0"?><D:lockinfo xmlns:D="DAV:"><D:owner>alice</D:owner></D:lockinfo>""".toByteArray(),
        )
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a.txt")

        assertEquals("alice", lockManager.lockCovering("/a.txt")?.owner)
    }

    @Test
    fun test_lock_refreshViaIfHeader_extendsExistingLockAndReturnsSameToken() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val lock = lockManager.lock("/a.txt", LockDepth.ZERO, owner = null)
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        request.addHeader(DavHeaders.IF, "(<${lock.token}>)")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a.txt")

        assertEquals(HttpStatus.OK.value(), response.status)
        assertEquals("<${lock.token}>", response.getHeader(DavHeaders.LOCK_TOKEN))
    }

    @Test
    fun test_unlock_withCorrectToken_removesLock() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val lock = lockManager.lock("/a.txt", LockDepth.ZERO, owner = null)
        val request = MockHttpServletRequest("UNLOCK", "/dav/a.txt")
        request.addHeader(DavHeaders.LOCK_TOKEN, "<${lock.token}>")
        val response = MockHttpServletResponse()

        handler.unlock(request, response, "/a.txt")

        assertEquals(HttpStatus.NO_CONTENT.value(), response.status)
        assertNull(lockManager.lockCovering("/a.txt"))
    }

    @Test
    fun test_unlock_withoutLockTokenHeader_isBadRequest() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("UNLOCK", "/dav/a.txt")
        val response = MockHttpServletResponse()

        handler.unlock(request, response, "/a.txt")

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status)
    }

    @Test
    fun test_unlock_withWrongToken_isConflict() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, owner = null)
        val request = MockHttpServletRequest("UNLOCK", "/dav/a.txt")
        request.addHeader(DavHeaders.LOCK_TOKEN, "<urn:uuid:wrong>")
        val response = MockHttpServletResponse()

        handler.unlock(request, response, "/a.txt")

        assertEquals(HttpStatus.CONFLICT.value(), response.status)
    }

    @Test
    fun test_lock_withDepthZero_doesNotCoverChildren() {
        val (handler, store, lockManager) = newHandler()
        store.makeCollection("/a")
        val request = MockHttpServletRequest("LOCK", "/dav/a")
        request.addHeader(DavHeaders.DEPTH, "0")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a")

        assertNull(lockManager.lockCovering("/a/child"))
    }
}
