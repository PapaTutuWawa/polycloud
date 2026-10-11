package me.polynom.polycloud.apps.webdav.xml

import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavFile
import me.polynom.polycloud.apps.webdav.store.DavPropertyName
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivePropertiesTest {
    @Test
    fun test_render_file_includesContentLengthAndType() {
        val node = DavFile(resource = ByteArrayResource("hello".toByteArray()), contentType = "text/plain")

        val props = LiveProperties.render("/a.txt", node)

        assertEquals("", props.getValue(DavPropertyName(DAV_NAMESPACE, "resourcetype")))
        assertEquals("5", props.getValue(DavPropertyName(DAV_NAMESPACE, "getcontentlength")))
        assertEquals("text/plain", props.getValue(DavPropertyName(DAV_NAMESPACE, "getcontenttype")))
        assertEquals("a.txt", props.getValue(DavPropertyName(DAV_NAMESPACE, "displayname")))
    }

    @Test
    fun test_render_collection_reportsCollectionResourcetypeAndZeroLength() {
        val node = DavCollection()

        val props = LiveProperties.render("/folder", node)

        assertEquals("<D:collection/>", props.getValue(DavPropertyName(DAV_NAMESPACE, "resourcetype")))
        assertEquals("0", props.getValue(DavPropertyName(DAV_NAMESPACE, "getcontentlength")))
        assertFalse(props.containsKey(DavPropertyName(DAV_NAMESPACE, "getcontenttype")))
    }

    @Test
    fun test_render_root_usesSlashAsDisplayName() {
        val props = LiveProperties.render("/", DavCollection())

        assertEquals("/", props.getValue(DavPropertyName(DAV_NAMESPACE, "displayname")))
    }

    @Test
    fun test_render_includesEtagMatchingNodeEtag() {
        val node = DavFile(resource = ByteArrayResource(ByteArray(0)), contentType = "text/plain")

        val props = LiveProperties.render("/a.txt", node)

        assertTrue(props.getValue(DavPropertyName(DAV_NAMESPACE, "getetag")).isNotBlank())
        assertEquals(node.etag, props.getValue(DavPropertyName(DAV_NAMESPACE, "getetag")))
    }
}
