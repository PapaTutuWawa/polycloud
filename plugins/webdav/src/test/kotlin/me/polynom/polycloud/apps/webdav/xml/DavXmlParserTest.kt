package me.polynom.polycloud.apps.webdav.xml

import me.polynom.polycloud.apps.webdav.store.DavPropertyName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DavXmlParserTest {
    @Test
    fun test_parsePropfind_emptyBody_defaultsToAllProp() {
        assertEquals(PropfindRequest.AllProp, DavXmlParser.parsePropfind(""))
    }

    @Test
    fun test_parsePropfind_allpropElement_isAllProp() {
        val body = """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""

        assertEquals(PropfindRequest.AllProp, DavXmlParser.parsePropfind(body))
    }

    @Test
    fun test_parsePropfind_propnameElement_isPropName() {
        val body = """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:propname/></D:propfind>"""

        assertEquals(PropfindRequest.PropName, DavXmlParser.parsePropfind(body))
    }

    @Test
    fun test_parsePropfind_namedDavProps_extractsNamesWithDavNamespace() {
        val body =
            """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:prop><D:displayname/><D:getetag/></D:prop></D:propfind>"""

        val result = assertIs<PropfindRequest.Named>(DavXmlParser.parsePropfind(body))

        assertEquals(
            listOf(DavPropertyName(DAV_NAMESPACE, "displayname"), DavPropertyName(DAV_NAMESPACE, "getetag")),
            result.names,
        )
    }

    @Test
    fun test_parsePropfind_namedCustomNamespaceProp_preservesNamespace() {
        val body =
            """<?xml version="1.0"?><D:propfind xmlns:D="DAV:" xmlns:x="urn:test"><D:prop><x:label/></D:prop></D:propfind>"""

        val result = assertIs<PropfindRequest.Named>(DavXmlParser.parsePropfind(body))

        assertEquals(listOf(DavPropertyName("urn:test", "label")), result.names)
    }

    @Test
    fun test_parseProppatch_setAndRemove_areSeparated() {
        val body =
            """
            <?xml version="1.0"?>
            <D:propertyupdate xmlns:D="DAV:" xmlns:x="urn:test">
              <D:set><D:prop><x:label>hello</x:label></D:prop></D:set>
              <D:remove><D:prop><x:obsolete/></D:prop></D:remove>
            </D:propertyupdate>
            """.trimIndent()

        val result = DavXmlParser.parseProppatch(body)

        assertEquals(listOf(DavPropertyName("urn:test", "label") to "hello"), result.sets)
        assertEquals(listOf(DavPropertyName("urn:test", "obsolete")), result.removes)
    }

    @Test
    fun test_parseLockOwner_withOwnerElement_returnsTrimmedText() {
        val body = """<?xml version="1.0"?><D:lockinfo xmlns:D="DAV:"><D:owner>  alice  </D:owner></D:lockinfo>"""

        assertEquals("alice", DavXmlParser.parseLockOwner(body))
    }

    @Test
    fun test_parseLockOwner_emptyBody_isNull() {
        assertNull(DavXmlParser.parseLockOwner(""))
    }

    @Test
    fun test_parseLockOwner_withoutOwnerElement_isNull() {
        val body = """<?xml version="1.0"?><D:lockinfo xmlns:D="DAV:"><D:lockscope><D:exclusive/></D:lockscope></D:lockinfo>"""

        assertNull(DavXmlParser.parseLockOwner(body))
    }

    @Test
    fun test_parsePropfind_doesNotResolveExternalEntities() {
        val body =
            """<?xml version="1.0"?><!DOCTYPE D:propfind [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>""" +
                """<D:propfind xmlns:D="DAV:"><D:prop><D:displayname>&xxe;</D:displayname></D:prop></D:propfind>"""

        assertThrowsOnDoctype(body)
    }

    private fun assertThrowsOnDoctype(body: String) {
        org.junit.jupiter.api.assertThrows<Exception> {
            DavXmlParser.parsePropfind(body)
        }
    }
}
