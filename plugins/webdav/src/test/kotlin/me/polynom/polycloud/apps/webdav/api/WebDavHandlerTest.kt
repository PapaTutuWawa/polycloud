package me.polynom.polycloud.apps.webdav.api

import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.InMemoryDavStore
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebDavHandlerTest {
    private fun newHandler(store: InMemoryDavStore = InMemoryDavStore()): WebDavHandler {
        val lockManager = LockManager()
        return WebDavHandler(
            ResourceHandler(store, lockManager),
            CopyMoveHandler(store, lockManager),
            PropertyHandler(store, lockManager),
            LockHandler(store, lockManager),
        )
    }

    @Test
    fun test_handle_unsupportedMethod_isMethodNotAllowed() {
        val handler = newHandler()
        val request = MockHttpServletRequest("TRACE", "/dav/a.txt")
        val response = MockHttpServletResponse()

        handler.handle(request, response)

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.status)
    }

    @Test
    fun test_handle_missingPathInfo_resolvesToRoot() {
        val handler = newHandler()
        val request = MockHttpServletRequest("PROPFIND", "/dav")
        request.pathInfo = null
        val response = MockHttpServletResponse()

        handler.handle(request, response)

        assertEquals(HttpStatus.MULTI_STATUS.value(), response.status)
        assertTrue(response.contentAsString.contains("<D:href>/dav/</D:href>"))
    }

    @Test
    fun test_handle_dispatchesByMethodCaseInsensitively() {
        val store = InMemoryDavStore()
        val handler = newHandler(store)
        val request = MockHttpServletRequest("mkcol", "/dav/folder")
        request.pathInfo = "/folder"
        val response = MockHttpServletResponse()

        handler.handle(request, response)

        assertEquals(HttpStatus.CREATED.value(), response.status)
        assertTrue(store.exists("/folder"))
    }

    @Test
    fun test_handle_convertsDavExceptionFromHandlerIntoResponseStatus() {
        val handler = newHandler()
        val request = MockHttpServletRequest("PUT", "/dav")
        request.pathInfo = "/"
        val response = MockHttpServletResponse()

        handler.handle(request, response)

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.status)
    }

    @Test
    fun test_handle_optionsRequest_delegatesToResourceHandler() {
        val handler = newHandler()
        val request = MockHttpServletRequest("OPTIONS", "/dav/")
        request.pathInfo = "/"
        val response = MockHttpServletResponse()

        handler.handle(request, response)

        assertEquals(HttpStatus.OK.value(), response.status)
        assertEquals(WebDavConstants.ALLOWED_METHODS, response.getHeader("Allow"))
    }
}
