package me.polynom.polycloud.apps.webdav.xml

import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.lock.LockScope
import me.polynom.polycloud.apps.webdav.store.DavPropertyName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DavXmlTest {
    @Test
    fun test_escapeXml_escapesAmpersandAngleBrackets() {
        assertEquals("a &amp; b &lt;tag&gt;", DavXml.escapeXml("a & b <tag>"))
    }

    @Test
    fun test_buildMultiStatus_singleEntryWithDavProps_usesDPrefix() {
        val entry =
            DavResponseEntry(
                href = "/dav/a.txt",
                foundProps = mapOf(DavPropertyName(DAV_NAMESPACE, "displayname") to "a.txt"),
            )

        val xml = DavXml.buildMultiStatus(listOf(entry))

        assertTrue(xml.contains("<D:multistatus xmlns:D=\"DAV:\">"))
        assertTrue(xml.contains("<D:href>/dav/a.txt</D:href>"))
        assertTrue(xml.contains("<D:displayname>a.txt</D:displayname>"))
        assertTrue(xml.contains("<D:status>HTTP/1.1 200 OK</D:status>"))
    }

    @Test
    fun test_buildMultiStatus_customNamespaceProp_declaresAndUsesGeneratedPrefix() {
        val entry =
            DavResponseEntry(
                href = "/dav/a.txt",
                foundProps = mapOf(DavPropertyName("urn:test", "label") to "hello"),
            )

        val xml = DavXml.buildMultiStatus(listOf(entry))

        assertTrue(xml.contains("xmlns:ns1=\"urn:test\""))
        assertTrue(xml.contains("<ns1:label>hello</ns1:label>"))
    }

    @Test
    fun test_buildMultiStatus_emptyNamespaceProp_rendersUnprefixedWithoutAnIllegalXmlnsDeclaration() {
        // `xmlns:foo=""` is illegal XML - an unnamespaced property must render with no prefix
        // at all, relying on there being no default namespace declared on <D:multistatus>.
        val entry =
            DavResponseEntry(
                href = "/dav/a.txt",
                foundProps = mapOf(DavPropertyName("", "nonamespace") to "hello"),
            )

        val xml = DavXml.buildMultiStatus(listOf(entry))

        assertTrue(xml.contains("<nonamespace>hello</nonamespace>"))
        assertEquals(false, xml.contains("xmlns:ns1"))
        assertEquals(false, xml.contains("xmlns=\"\""))
    }

    @Test
    fun test_buildMultiStatus_missingProps_areReportedAs404() {
        val entry =
            DavResponseEntry(
                href = "/dav/a.txt",
                missingProps = listOf(DavPropertyName(DAV_NAMESPACE, "nope")),
            )

        val xml = DavXml.buildMultiStatus(listOf(entry))

        assertTrue(xml.contains("<D:nope/>"))
        assertTrue(xml.contains("<D:status>HTTP/1.1 404 Not Found</D:status>"))
    }

    @Test
    fun test_buildMultiStatus_emptyValue_rendersSelfClosingTag() {
        val entry =
            DavResponseEntry(
                href = "/dav/a",
                foundProps = mapOf(DavPropertyName(DAV_NAMESPACE, "resourcetype") to ""),
            )

        val xml = DavXml.buildMultiStatus(listOf(entry))

        assertTrue(xml.contains("<D:resourcetype/>"))
    }

    @Test
    fun test_buildLockDiscovery_includesTokenDepthAndOwner() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.INFINITY, scope = LockScope.EXCLUSIVE, owner = "alice")

        val xml = DavXml.buildLockDiscovery(lock, "/dav/a")

        assertTrue(xml.contains("<D:depth>infinity</D:depth>"))
        assertTrue(xml.contains("<D:owner>alice</D:owner>"))
        assertTrue(xml.contains("<D:href>${lock.token}</D:href>"))
        assertTrue(xml.contains("<D:href>/dav/a</D:href>"))
    }

    @Test
    fun test_buildLockDiscovery_withoutOwner_omitsOwnerElement() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)

        val xml = DavXml.buildLockDiscovery(lock, "/dav/a")

        assertTrue(xml.contains("<D:depth>0</D:depth>"))
        assertEquals(false, xml.contains("<D:owner>"))
    }
}
