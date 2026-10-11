package me.polynom.polycloud.apps.webdav.store

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ContentTypesTest {
    @Test
    fun test_resolve_specificDeclaredType_isUsedAsIs() {
        assertEquals("text/plain", ContentTypes.resolve("/a.txt", "text/plain"))
    }

    @Test
    fun test_resolve_specificDeclaredTypeWithCharset_isKeptIntact() {
        assertEquals("text/html; charset=UTF-8", ContentTypes.resolve("/a.html", "text/html; charset=UTF-8"))
    }

    @Test
    fun test_resolve_missingDeclaredType_infersFromExtension() {
        assertEquals("application/pdf", ContentTypes.resolve("/report.pdf", null))
    }

    @Test
    fun test_resolve_genericOctetStreamDeclared_infersFromExtension() {
        assertEquals("application/pdf", ContentTypes.resolve("/report.pdf", "application/octet-stream"))
    }

    @Test
    fun test_resolve_genericWithCharsetDeclared_stillInfersFromExtension() {
        assertEquals("application/pdf", ContentTypes.resolve("/report.pdf", "application/octet-stream; charset=binary"))
    }

    @Test
    fun test_resolve_unknownExtensionAndNoDeclaredType_fallsBackToOctetStream() {
        assertEquals("application/octet-stream", ContentTypes.resolve("/mystery.unknownext", null))
    }

    @Test
    fun test_resolve_unknownExtensionWithGenericDeclaredType_keepsDeclaredGenericType() {
        assertEquals("application/octet-stream", ContentTypes.resolve("/mystery.unknownext", "application/octet-stream"))
    }

    @Test
    fun test_resolve_blankDeclaredType_infersFromExtension() {
        assertEquals("application/pdf", ContentTypes.resolve("/report.pdf", ""))
    }
}
