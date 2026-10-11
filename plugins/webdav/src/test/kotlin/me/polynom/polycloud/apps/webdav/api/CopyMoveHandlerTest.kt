package me.polynom.polycloud.apps.webdav.api

import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.DavFile
import me.polynom.polycloud.apps.webdav.store.InMemoryDavStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CopyMoveHandlerTest {
    private fun newHandler(): Pair<CopyMoveHandler, InMemoryDavStore> {
        val store = InMemoryDavStore()
        return Pair(CopyMoveHandler(store, LockManager()), store)
    }

    private fun request(
        method: String,
        path: String,
        destination: String? = null,
        overwrite: String? = null,
        depth: String? = null,
    ): MockHttpServletRequest {
        val request = MockHttpServletRequest(method, "/dav$path")
        destination?.let { request.addHeader(DavHeaders.DESTINATION, "http://localhost/dav$it") }
        overwrite?.let { request.addHeader(DavHeaders.OVERWRITE, it) }
        depth?.let { request.addHeader(DavHeaders.DEPTH, it) }
        return request
    }

    @Test
    fun test_copy_file_createsAtDestinationAndKeepsSource() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")
        val response = MockHttpServletResponse()

        handler.copy(request("COPY", "/a.txt", destination = "/b.txt"), response, "/a.txt")

        assertEquals(HttpStatus.CREATED.value(), response.status)
        assertTrue(store.exists("/a.txt"))
        assertContentEquals("hello".toByteArray(), (store.get("/b.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_copy_missingDestinationHeader_isBadRequest() {
        val (handler) = newHandler()
        val response = MockHttpServletResponse()

        val exception = assertThrows<DavException> { handler.copy(request("COPY", "/a.txt"), response, "/a.txt") }

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status)
    }

    @Test
    fun test_copy_missingSource_is404() {
        val (handler) = newHandler()
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.copy(request("COPY", "/missing", destination = "/b.txt"), response, "/missing")
            }

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status)
    }

    @Test
    fun test_copy_sameSourceAndDestination_isForbidden() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.copy(request("COPY", "/a.txt", destination = "/a.txt"), response, "/a.txt")
            }

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.status)
    }

    @Test
    fun test_copy_collectionIntoItself_isForbidden() {
        val (handler, store) = newHandler()
        store.makeCollection("/a")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.copy(request("COPY", "/a", destination = "/a/sub"), response, "/a")
            }

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.status)
    }

    @Test
    fun test_copy_destinationParentMissing_isConflict() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.copy(request("COPY", "/a.txt", destination = "/missing/b.txt"), response, "/a.txt")
            }

        assertEquals(HttpStatus.CONFLICT.value(), exception.status)
    }

    @Test
    fun test_copy_destinationExistsWithOverwriteF_isPreconditionFailed() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        store.putFile("/b.txt", ByteArray(0), "text/plain")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.copy(request("COPY", "/a.txt", destination = "/b.txt", overwrite = "F"), response, "/a.txt")
            }

        assertEquals(HttpStatus.PRECONDITION_FAILED.value(), exception.status)
    }

    @Test
    fun test_copy_destinationExistsWithDefaultOverwrite_replacesAndReturns204() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", "new".toByteArray(), "text/plain")
        store.putFile("/b.txt", "old".toByteArray(), "text/plain")
        val response = MockHttpServletResponse()

        handler.copy(request("COPY", "/a.txt", destination = "/b.txt"), response, "/a.txt")

        assertEquals(HttpStatus.NO_CONTENT.value(), response.status)
        assertContentEquals("new".toByteArray(), (store.get("/b.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_copy_collectionWithDepthZero_doesNotCopyChildren() {
        val (handler, store) = newHandler()
        store.makeCollection("/a")
        store.putFile("/a/f.txt", ByteArray(0), "text/plain")
        val response = MockHttpServletResponse()

        handler.copy(request("COPY", "/a", destination = "/b", depth = "0"), response, "/a")

        assertTrue(store.exists("/b"))
        assertFalse(store.exists("/b/f.txt"))
    }

    @Test
    fun test_move_relocatesResourceAndRemovesSource() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")
        val response = MockHttpServletResponse()

        handler.move(request("MOVE", "/a.txt", destination = "/b.txt"), response, "/a.txt")

        assertEquals(HttpStatus.CREATED.value(), response.status)
        assertFalse(store.exists("/a.txt"))
        assertContentEquals("hello".toByteArray(), (store.get("/b.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_move_collectionWithDepthZero_isBadRequest() {
        val (handler, store) = newHandler()
        store.makeCollection("/a")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.move(request("MOVE", "/a", destination = "/b", depth = "0"), response, "/a")
            }

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status)
    }

    @Test
    fun test_move_missingDestinationHeader_isBadRequest() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val response = MockHttpServletResponse()

        val exception = assertThrows<DavException> { handler.move(request("MOVE", "/a.txt"), response, "/a.txt") }

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status)
    }
}
