package me.polynom.polycloud.apps.files.converters

import me.polynom.polycloud.apps.files.exceptions.TusInvalidUploadMetadataException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class UploadMetadataConverterTest {
    private val converter = UploadMetadataConverter()

    @Test
    fun test_convert_emptyString_returnsEmptyMetadata() {
        val result = converter.convert("")

        assertEquals(emptyMap(), result.toMap())
    }

    @Test
    fun test_convert_blankString_returnsEmptyMetadata() {
        val result = converter.convert("   ")

        assertEquals(emptyMap(), result.toMap())
    }

    @Test
    fun test_convert_singleKeyWithBase64Value_decodesValue() {
        val result = converter.convert("filename YWxpY2U=")

        assertEquals(mapOf("filename" to "alice"), result.toMap())
    }

    @Test
    fun test_convert_multipleKeys_decodesAll() {
        val result = converter.convert("filename d29ybGRfZG9taW5hdGlvbl9wbGFuLnBkZg==,user YWxpY2U=")

        assertEquals(
            mapOf(
                "filename" to "world_domination_plan.pdf",
                "user" to "alice",
            ),
            result.toMap(),
        )
    }

    @Test
    fun test_convert_keyWithoutValue_valueIsEmptyString() {
        val result = converter.convert("is_confidential")

        assertEquals(mapOf("is_confidential" to ""), result.toMap())
    }

    @Test
    fun test_convert_mixOfValuedAndValuelessKeys_decodesBoth() {
        // Example straight from the tus resumable upload protocol spec.
        val result = converter.convert("filename d29ybGRfZG9taW5hdGlvbl9wbGFuLnBkZg==,is_confidential")

        assertEquals(
            mapOf(
                "filename" to "world_domination_plan.pdf",
                "is_confidential" to "",
            ),
            result.toMap(),
        )
    }

    @Test
    fun test_convert_trimsWhitespaceAroundPairs() {
        val result = converter.convert(" filename YWxpY2U= , user YWxpY2U= ")

        assertEquals(
            mapOf(
                "filename" to "alice",
                "user" to "alice",
            ),
            result.toMap(),
        )
    }

    @Test
    fun test_convert_duplicateKey_throws() {
        assertThrows<TusInvalidUploadMetadataException> {
            converter.convert("filename YWxpY2U=,filename YWxpY2U=")
        }
    }

    @Test
    fun test_convert_trailingComma_throwsOnEmptyPair() {
        assertThrows<TusInvalidUploadMetadataException> {
            converter.convert("filename YWxpY2U=,")
        }
    }

    @Test
    fun test_convert_consecutiveCommas_throwsOnEmptyPair() {
        assertThrows<TusInvalidUploadMetadataException> {
            converter.convert("filename YWxpY2U=,,user YWxpY2U=")
        }
    }

    @Test
    fun test_convert_pairWithMultipleSpaces_throws() {
        assertThrows<TusInvalidUploadMetadataException> {
            converter.convert("filename YWxp Y2U=")
        }
    }

    @Test
    fun test_convert_keyWithNonAsciiCharacter_throws() {
        assertThrows<TusInvalidUploadMetadataException> {
            converter.convert("filenamé")
        }
    }

    @Test
    fun test_convert_malformedBase64Value_throws() {
        assertThrows<TusInvalidUploadMetadataException> {
            converter.convert("filename !!!not-base64!!!")
        }
    }
}
