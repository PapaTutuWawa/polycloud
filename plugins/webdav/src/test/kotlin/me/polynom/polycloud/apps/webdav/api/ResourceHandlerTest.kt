package me.polynom.polycloud.apps.webdav.api

import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.DavFile
import me.polynom.polycloud.apps.webdav.store.InMemoryDavStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResourceHandlerTest {
    private fun newHandler(): Triple<ResourceHandler, InMemoryDavStore, LockManager> {
        val store = InMemoryDavStore()
        val lockManager = LockManager()
        return Triple(ResourceHandler(store, lockManager), store, lockManager)
    }

    @Test
    fun test_options_advertisesDavComplianceAndAllowedMethods() {
        val (handler) = newHandler()
        val response = MockHttpServletResponse()

        handler.options(response)

        assertEquals(HttpStatus.OK.value(), response.status)
        assertEquals("1, 2", response.getHeader("DAV"))
        assertEquals(WebDavConstants.ALLOWED_METHODS, response.getHeader("Allow"))
    }

    @Test
    fun test_get_existingFile_returnsContentAndHeaders() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")

        val response = MockHttpServletResponse()
        handler.get("/a.txt", response, includeBody = true)

        assertEquals(HttpStatus.OK.value(), response.status)
        assertEquals("text/plain", response.contentType)
        assertEquals("hello", response.contentAsString)
        assertNotNull(response.getHeader("ETag"))
    }

    @Test
    fun test_get_head_omitsBodyButKeepsHeaders() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")

        val response = MockHttpServletResponse()
        handler.get("/a.txt", response, includeBody = false)

        assertEquals(HttpStatus.OK.value(), response.status)
        assertEquals(0, response.contentAsByteArray.size)
    }

    @Test
    fun test_get_collection_rendersHtmlListing() {
        val (handler, store) = newHandler()
        store.makeCollection("/folder")
        store.putFile("/folder/a.txt", ByteArray(0), "text/plain")

        val response = MockHttpServletResponse()
        handler.get("/folder", response, includeBody = true)

        assertEquals(HttpStatus.OK.value(), response.status)
        assertTrue(response.contentAsString.contains("a.txt"))
    }

    @Test
    fun test_get_missing_returns404() {
        val (handler) = newHandler()
        val response = MockHttpServletResponse()

        handler.get("/missing", response, includeBody = true)

        assertEquals(HttpStatus.NOT_FOUND.value(), response.status)
    }

    @Test
    fun test_put_newFile_returns201AndStoresContent() {
        val (handler, store) = newHandler()
        val request = MockHttpServletRequest("PUT", "/dav/a.txt")
        request.contentType = "text/plain"
        request.setContent("hello".toByteArray())
        val response = MockHttpServletResponse()

        handler.put(request, response, "/a.txt")

        assertEquals(HttpStatus.CREATED.value(), response.status)
        assertTrue(store.exists("/a.txt"))
    }

    @Test
    fun test_put_withoutContentType_infersFromExtensionSoFileBrowsersRecognizeIt() {
        val (handler, store) = newHandler()
        val request = MockHttpServletRequest("PUT", "/dav/report.pdf")
        request.setContent(ByteArray(0))
        val response = MockHttpServletResponse()

        handler.put(request, response, "/report.pdf")

        assertEquals("application/pdf", (store.get("/report.pdf") as DavFile).contentType)
    }

    @Test
    fun test_put_withGenericOctetStreamContentType_infersFromExtension() {
        val (handler, store) = newHandler()
        val request = MockHttpServletRequest("PUT", "/dav/report.pdf")
        request.contentType = "application/octet-stream"
        request.setContent(ByteArray(0))
        val response = MockHttpServletResponse()

        handler.put(request, response, "/report.pdf")

        assertEquals("application/pdf", (store.get("/report.pdf") as DavFile).contentType)
    }

    @Test
    fun test_get_afterPutWithoutContentType_servesCorrectlyInferredContentType() {
        val (handler) = newHandler()
        val putRequest = MockHttpServletRequest("PUT", "/dav/report.pdf")
        putRequest.setContent(ByteArray(0))
        handler.put(putRequest, MockHttpServletResponse(), "/report.pdf")

        val response = MockHttpServletResponse()
        handler.get("/report.pdf", response, includeBody = false)

        assertEquals("application/pdf", response.contentType)
    }

    @Test
    fun test_put_overExistingFile_returns204() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", "old".toByteArray(), "text/plain")
        val request = MockHttpServletRequest("PUT", "/dav/a.txt")
        request.setContent("new".toByteArray())
        val response = MockHttpServletResponse()

        handler.put(request, response, "/a.txt")

        assertEquals(HttpStatus.NO_CONTENT.value(), response.status)
    }

    @Test
    fun test_put_ontoRoot_isMethodNotAllowed() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("PUT", "/dav/")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.put(request, response, "/")
            }

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), exception.status)
    }

    @Test
    fun test_put_ontoCollection_isConflict() {
        val (handler, store) = newHandler()
        store.makeCollection("/folder")
        val request = MockHttpServletRequest("PUT", "/dav/folder")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.put(request, response, "/folder")
            }

        assertEquals(HttpStatus.CONFLICT.value(), exception.status)
    }

    @Test
    fun test_put_withMissingParentCollection_isConflict() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("PUT", "/dav/missing/a.txt")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.put(request, response, "/missing/a.txt")
            }

        assertEquals(HttpStatus.CONFLICT.value(), exception.status)
    }

    @Test
    fun test_put_lockedByAnotherClient_isLocked() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, owner = null)
        val request = MockHttpServletRequest("PUT", "/dav/a.txt")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.put(request, response, "/a.txt")
            }

        assertEquals(HttpStatus.LOCKED.value(), exception.status)
    }

    @Test
    fun test_put_withOwnLockToken_succeeds() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val lock = lockManager.lock("/a.txt", LockDepth.ZERO, owner = null)
        val request = MockHttpServletRequest("PUT", "/dav/a.txt")
        request.addHeader("If", "(<${lock.token}>)")
        val response = MockHttpServletResponse()

        handler.put(request, response, "/a.txt")

        assertEquals(HttpStatus.NO_CONTENT.value(), response.status)
    }

    @Test
    fun test_delete_existingFile_returns204() {
        val (handler, store) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val request = MockHttpServletRequest("DELETE", "/dav/a.txt")
        val response = MockHttpServletResponse()

        handler.delete(request, response, "/a.txt")

        assertEquals(HttpStatus.NO_CONTENT.value(), response.status)
        assertTrue(!store.exists("/a.txt"))
    }

    @Test
    fun test_delete_missing_is404() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("DELETE", "/dav/missing")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.delete(request, response, "/missing")
            }

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status)
    }

    @Test
    fun test_delete_root_isForbidden() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("DELETE", "/dav/")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.delete(request, response, "/")
            }

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.status)
    }

    @Test
    fun test_delete_locked_isLocked() {
        val (handler, store, lockManager) = newHandler()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        lockManager.lock("/a.txt", LockDepth.ZERO, owner = null)
        val request = MockHttpServletRequest("DELETE", "/dav/a.txt")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.delete(request, response, "/a.txt")
            }

        assertEquals(HttpStatus.LOCKED.value(), exception.status)
    }

    @Test
    fun test_mkcol_newCollection_returns201() {
        val (handler, store) = newHandler()
        val request = MockHttpServletRequest("MKCOL", "/dav/folder")
        val response = MockHttpServletResponse()

        handler.mkcol(request, response, "/folder")

        assertEquals(HttpStatus.CREATED.value(), response.status)
        assertTrue(store.exists("/folder"))
    }

    @Test
    fun test_mkcol_onExistingResource_isMethodNotAllowed() {
        val (handler, store) = newHandler()
        store.makeCollection("/folder")
        val request = MockHttpServletRequest("MKCOL", "/dav/folder")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.mkcol(request, response, "/folder")
            }

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), exception.status)
    }

    @Test
    fun test_mkcol_withMissingParent_isConflict() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("MKCOL", "/dav/missing/folder")
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.mkcol(request, response, "/missing/folder")
            }

        assertEquals(HttpStatus.CONFLICT.value(), exception.status)
    }

    @Test
    fun test_mkcol_withRequestBody_isUnsupportedMediaType() {
        val (handler) = newHandler()
        val request = MockHttpServletRequest("MKCOL", "/dav/folder")
        request.setContent("<unexpected/>".toByteArray())
        val response = MockHttpServletResponse()

        val exception =
            assertThrows<DavException> {
                handler.mkcol(request, response, "/folder")
            }

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), exception.status)
    }
}
