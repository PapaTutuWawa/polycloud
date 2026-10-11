package me.polynom.polycloud.apps.webdav.api

import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.InMemoryDavStore
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebDavServletTest {
    @Test
    fun test_service_delegatesToHandler() {
        val store = InMemoryDavStore()
        val lockManager = LockManager()
        val handler =
            WebDavHandler(
                ResourceHandler(store, lockManager),
                CopyMoveHandler(store, lockManager),
                PropertyHandler(store, lockManager),
                LockHandler(store, lockManager),
            )
        val servlet = WebDavServlet(handler)
        val request = MockHttpServletRequest("MKCOL", "/dav/folder")
        request.pathInfo = "/folder"
        val response = MockHttpServletResponse()

        servlet.service(request, response)

        assertEquals(HttpStatus.CREATED.value(), response.status)
        assertTrue(store.exists("/folder"))
    }
}
