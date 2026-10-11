package me.polynom.polycloud.apps.webdav.api

import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.lock.LockScope
import me.polynom.polycloud.apps.webdav.store.InMemoryDavStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    fun test_lock_missingResource_createsLockNullResourceAndReturns201() {
        // RFC 4918 section 7.3: LOCK on an unmapped URL creates an empty "lock-null" resource.
        val (handler, store) = newHandler()
        val request = MockHttpServletRequest("LOCK", "/dav/missing")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/missing")

        assertEquals(HttpStatus.CREATED.value(), response.status)
        assertTrue(store.exists("/missing"))
        assertNotNull(response.getHeader(DavHeaders.LOCK_TOKEN))
    }

    @Test
    fun test_lock_missingResourceWithMissingParent_isConflict() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("LOCK", "/dav/missing-parent/child")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/missing-parent/child")

        assertEquals(HttpStatus.CONFLICT.value(), response.status)
    }

    @Test
    fun test_lock_alreadyLockedByAnother_isLocked() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = "bob")
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
        val lock = lockManager.lock("/a.txt", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)
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
        val lock = lockManager.lock("/a.txt", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)
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
        lockManager.lock("/a.txt", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)
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

    @Test
    fun test_lock_sharedScopeRequested_isGrantedAndReportedInDiscoveryBody() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        request.setContent(
            """
            <?xml version="1.0"?>
            <D:lockinfo xmlns:D="DAV:">
              <D:lockscope><D:shared/></D:lockscope>
              <D:locktype><D:write/></D:locktype>
            </D:lockinfo>
            """.trimIndent().toByteArray(),
        )
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a.txt")

        assertEquals(HttpStatus.OK.value(), response.status)
        assertTrue(response.contentAsString.contains("<D:shared/>"))
    }

    @Test
    fun test_lock_secondSharedLockRequest_isAlsoGranted() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, scope = LockScope.SHARED, owner = "alice")
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        request.setContent(
            """<?xml version="1.0"?><D:lockinfo xmlns:D="DAV:"><D:lockscope><D:shared/></D:lockscope></D:lockinfo>""".toByteArray(),
        )
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a.txt")

        assertEquals(HttpStatus.OK.value(), response.status)
        assertEquals(2, lockManager.locksCovering("/a.txt").size)
    }

    @Test
    fun test_lock_exclusiveBlockedByExistingSharedLock() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, scope = LockScope.SHARED, owner = "alice")
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/a.txt")

        assertEquals(HttpStatus.LOCKED.value(), response.status)
    }

    @Test
    fun test_lock_indirectRefreshViaChildPathCoveredByInfiniteLock_succeeds() {
        // Refreshing a collection's lock by sending LOCK to a child URL it covers, per litmus's
        // "indirect_refresh" - the lock covers the child, it doesn't have to equal it exactly.
        val (handler, store, lockManager) = newHandler()
        store.makeCollection("/coll")
        store.putFile("/coll/child.txt", ByteArray(0), "text/plain")
        val lock = lockManager.lock("/coll", LockDepth.INFINITY, scope = LockScope.EXCLUSIVE, owner = null)
        val request = MockHttpServletRequest("LOCK", "/dav/coll/child.txt")
        request.addHeader(DavHeaders.IF, "(<${lock.token}>)")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/coll/child.txt")

        assertEquals(HttpStatus.OK.value(), response.status)
    }

    @Test
    fun test_lock_refreshWithTokenNotCoveringThisPath_isPreconditionFailed() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        store.putFile("/b.txt", ByteArray(0), "text/plain")
        val lock = lockManager.lock("/a.txt", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)
        val request = MockHttpServletRequest("LOCK", "/dav/b.txt")
        request.addHeader(DavHeaders.IF, "(<${lock.token}>)")
        val response = MockHttpServletResponse()

        handler.lock(request, response, "/b.txt")

        assertEquals(HttpStatus.PRECONDITION_FAILED.value(), response.status)
    }

    @Test
    fun test_lock_malformedXmlBody_is400NotA500() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("LOCK", "/dav/a.txt")
        request.setContent("<not well formed".toByteArray())
        val response = MockHttpServletResponse()

        val exception = assertThrows<DavException> { handler.lock(request, response, "/a.txt") }

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status)
    }
}
