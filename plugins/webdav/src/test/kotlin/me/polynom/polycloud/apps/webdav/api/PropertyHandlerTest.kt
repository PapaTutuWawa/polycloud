package me.polynom.polycloud.apps.webdav.api

import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.InMemoryDavStore
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertyHandlerTest {
    private fun newHandler(): Pair<PropertyHandler, InMemoryDavStore> {
        val store = InMemoryDavStore()
        return Pair(PropertyHandler(store, LockManager()), store)
    }

    @Test
    fun test_propfind_missingResource_is404() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("PROPFIND", "/dav/missing")
        val response = MockHttpServletResponse()

        handler.propfind(request, response, "/missing")

        assertEquals(HttpStatus.NOT_FOUND.value(), response.status)
    }

    @Test
    fun test_propfind_allpropDefault_returnsMultiStatusWithLiveProps() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")
        val request = MockHttpServletRequest("PROPFIND", "/dav/a.txt")
        val response = MockHttpServletResponse()

        handler.propfind(request, response, "/a.txt")

        assertEquals(HttpStatus.MULTI_STATUS.value(), response.status)
        assertTrue(response.contentType?.startsWith("application/xml") == true)
        assertTrue(response.contentAsString.contains("<D:href>/dav/a.txt</D:href>"))
        assertTrue(response.contentAsString.contains("<D:getcontentlength>5</D:getcontentlength>"))
    }

    @Test
    fun test_propfind_depthZeroOnCollection_doesNotIncludeChildren() {
        val (handler, store) = newHandler()
        store.makeCollection("/a")
        store.putFile("/a/f.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("PROPFIND", "/dav/a")
        request.addHeader(DavHeaders.DEPTH, "0")
        val response = MockHttpServletResponse()

        handler.propfind(request, response, "/a")

        assertEquals(1, countOccurrences(response.contentAsString, "<D:response>"))
    }

    @Test
    fun test_propfind_depthOneOnCollection_includesDirectChildrenOnly() {
        val (handler, store) = newHandler()
        store.makeCollection("/a")
        store.makeCollection("/a/sub")
        store.putFile("/a/sub/deep.txt", ByteArray(0), "text/plain")
        store.putFile("/a/f.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("PROPFIND", "/dav/a")
        request.addHeader(DavHeaders.DEPTH, "1")
        val response = MockHttpServletResponse()

        handler.propfind(request, response, "/a")

        val body = response.contentAsString
        assertEquals(3, countOccurrences(body, "<D:response>"))
        assertFalse(body.contains("deep.txt"))
    }

    @Test
    fun test_propfind_depthInfinityOnCollection_includesAllDescendants() {
        val (handler, store) = newHandler()
        store.makeCollection("/a")
        store.makeCollection("/a/sub")
        store.putFile("/a/sub/deep.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("PROPFIND", "/dav/a")
        val response = MockHttpServletResponse()

        handler.propfind(request, response, "/a")

        assertTrue(response.contentAsString.contains("deep.txt"))
    }

    @Test
    fun test_propfind_namedUnknownProp_isReportedAs404WithinMultistatus() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("PROPFIND", "/dav/a.txt")
        request.setContent(
            """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:prop><D:nonexistent/></D:prop></D:propfind>""".toByteArray(),
        )
        val response = MockHttpServletResponse()

        handler.propfind(request, response, "/a.txt")

        val body = response.contentAsString
        assertTrue(body.contains("<D:nonexistent/>"))
        assertTrue(body.contains("HTTP/1.1 404 Not Found"))
    }

    @Test
    fun test_proppatch_setThenPropfind_reflectsCustomProperty() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val proppatchRequest = MockHttpServletRequest("PROPPATCH", "/dav/a.txt")
        proppatchRequest.setContent(
            """
            <?xml version="1.0"?>
            <D:propertyupdate xmlns:D="DAV:" xmlns:x="urn:test">
              <D:set><D:prop><x:label>hello</x:label></D:prop></D:set>
            </D:propertyupdate>
            """.trimIndent().toByteArray(),
        )
        val proppatchResponse = MockHttpServletResponse()

        handler.proppatch(proppatchRequest, proppatchResponse, "/a.txt")

        assertEquals(HttpStatus.MULTI_STATUS.value(), proppatchResponse.status)

        val propfindRequest = MockHttpServletRequest("PROPFIND", "/dav/a.txt")
        val propfindResponse = MockHttpServletResponse()
        handler.propfind(propfindRequest, propfindResponse, "/a.txt")

        assertTrue(propfindResponse.contentAsString.contains("<ns1:label>hello</ns1:label>"))
    }

    @Test
    fun test_proppatch_remove_dropsPreviouslySetProperty() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        setLabel(handler, "/a.txt")

        val removeRequest = MockHttpServletRequest("PROPPATCH", "/dav/a.txt")
        removeRequest.setContent(
            """
            <?xml version="1.0"?>
            <D:propertyupdate xmlns:D="DAV:" xmlns:x="urn:test">
              <D:remove><D:prop><x:label/></D:prop></D:remove>
            </D:propertyupdate>
            """.trimIndent().toByteArray(),
        )
        handler.proppatch(removeRequest, MockHttpServletResponse(), "/a.txt")

        val propfindRequest = MockHttpServletRequest("PROPFIND", "/dav/a.txt")
        val propfindResponse = MockHttpServletResponse()
        handler.propfind(propfindRequest, propfindResponse, "/a.txt")

        assertFalse(propfindResponse.contentAsString.contains("label"))
    }

    @Test
    fun test_proppatch_missingResource_is404() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("PROPPATCH", "/dav/missing")
        request.setContent(ByteArray(0))
        val response = MockHttpServletResponse()

        handler.proppatch(request, response, "/missing")

        assertEquals(HttpStatus.NOT_FOUND.value(), response.status)
    }

    @Test
    fun test_proppatch_lockedByAnotherClient_isLocked() {
        val store = InMemoryDavStore()
        val lockManager = LockManager()
        val handler = PropertyHandler(store, lockManager)
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, owner = null)
        val request = MockHttpServletRequest("PROPPATCH", "/dav/a.txt")
        request.setContent(ByteArray(0))
        val response = MockHttpServletResponse()

        handler.proppatch(request, response, "/a.txt")

        assertEquals(HttpStatus.LOCKED.value(), response.status)
    }

    private fun setLabel(
        handler: PropertyHandler,
        path: String,
    ) {
        val request = MockHttpServletRequest("PROPPATCH", "/dav$path")
        request.setContent(
            """
            <?xml version="1.0"?>
            <D:propertyupdate xmlns:D="DAV:" xmlns:x="urn:test">
              <D:set><D:prop><x:label>hello</x:label></D:prop></D:set>
            </D:propertyupdate>
            """.trimIndent().toByteArray(),
        )
        handler.proppatch(request, MockHttpServletResponse(), path)
    }

    private fun countOccurrences(
        haystack: String,
        needle: String,
    ): Int = haystack.split(needle).size - 1
}
