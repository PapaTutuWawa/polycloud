package me.polynom.polycloud.apps.files.api

import me.polynom.polycloud.apps.files.Utils.into
import me.polynom.polycloud.apps.files.constants.TusHeaders
import me.polynom.polycloud.apps.files.converters.UploadMetadataConverter
import me.polynom.polycloud.apps.files.exceptions.UploadDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.UploadWrongOffsetException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.service.UploadService
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.Base64
import java.util.UUID
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class FilesUploadApiControllerTest {
    @Mock
    private lateinit var uploadService: UploadService

    private lateinit var mockMvc: MockMvc

    private val tusVersion = "1.0.0"
    private val tusExtensions = "creation,termination"

    @BeforeEach
    fun setUp() {
        uploadService = mock()

        val conversionService =
            DefaultFormattingConversionService().apply {
                addConverter(UploadMetadataConverter())
            }

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(FilesUploadApiController(uploadService))
                .setControllerAdvice(TusVersionHeaderAdvice(), FilesExceptionHandler())
                .setConversionService(conversionService)
                .build()
    }

    private fun metadataHeader(vararg pairs: Pair<String, String>): String =
        pairs.joinToString(",") { (key, value) ->
            "$key ${Base64.getEncoder().encodeToString(value.toByteArray())}"
        }

    private fun requestFor(
        method: String,
        slot: UUID = UUID.randomUUID(),
    ) = request(HttpMethod.valueOf(method), "/api/apps/files/upload/{slot}", slot).apply {
        if (method == "PATCH") {
            contentType("application/offset+octet-stream")
            content(ByteArray(0))
        }
    }

    @Nested
    inner class CoreProtocol {
        @Test
        fun test_options_withoutTusResumableHeader_succeeds() {
            // "The Client SHOULD NOT include the Tus-Resumable header in the request
            // and the Server MUST ignore the header."
            val result = mockMvc.perform(options("/api/apps/files/upload")).andReturn()

            assertTrue(result.response.status == 200 || result.response.status == 204)
        }

        @Test
        fun test_options_withWrongTusResumableHeader_isIgnoredAndStillSucceeds() {
            val result =
                mockMvc
                    .perform(options("/api/apps/files/upload").header(TusHeaders.TUS_RESUMABLE, "not-a-real-version"))
                    .andReturn()

            assertTrue(result.response.status == 200 || result.response.status == 204)
        }

        @Test
        fun test_options_responseIncludesTusVersionHeader() {
            mockMvc
                .perform(options("/api/apps/files/upload"))
                .andExpect(header().exists(TusHeaders.TUS_VERSION))
        }

        @Test
        fun test_options_responseIncludesTusResumableHeader() {
            mockMvc
                .perform(options("/api/apps/files/upload"))
                .andExpect(header().exists(TusHeaders.TUS_RESUMABLE))
        }

        @Test
        fun test_options_responseIncludesTusExtensions() {
            mockMvc
                .perform(options("/api/apps/files/upload"))
                .andExpect(header().string(TusHeaders.TUS_EXTENSION, tusExtensions))
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["HEAD", "PATCH"])
        fun test_missingTusResumableHeader_returns412WithTusVersionHeader(method: String) {
            // "The Tus-Resumable header MUST be included in every request and response
            // except for OPTIONS requests." + "If the version specified by the Client
            // is not supported by the Server, it MUST respond with the 412 Precondition
            // Failed status" and include the Tus-Version header.
            mockMvc
                .perform(requestFor(method))
                .andExpect(status().isPreconditionFailed)
                .andExpect(header().exists(TusHeaders.TUS_VERSION))
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["HEAD", "PATCH"])
        fun test_wrongTusResumableVersion_returns412WithTusVersionHeader(method: String) {
            mockMvc
                .perform(requestFor(method).header(TusHeaders.TUS_RESUMABLE, "0.2.1"))
                .andExpect(status().isPreconditionFailed)
                .andExpect(header().exists(TusHeaders.TUS_VERSION))
        }

        @Test
        fun test_head_existingUpload_alwaysIncludesUploadOffsetHeaderEvenWhenZero() {
            // "The Server MUST always include the Upload-Offset header in the response
            // for a HEAD request, even if the offset is 0, or the upload is already
            // considered completed."
            val slot = UUID.randomUUID()
            whenever(uploadService.retrieveOffset(slot)).thenReturn(0L)

            mockMvc
                .perform(head("/api/apps/files/upload/{slot}", slot).header(TusHeaders.TUS_RESUMABLE, tusVersion))
                .andExpect(header().string(TusHeaders.UPLOAD_OFFSET, "0"))
        }

        @Test
        fun test_head_existingUpload_succeedsWithOkOrNoContent() {
            val slot = UUID.randomUUID()
            whenever(uploadService.retrieveOffset(slot)).thenReturn(5L)

            val result =
                mockMvc
                    .perform(head("/api/apps/files/upload/{slot}", slot).header(TusHeaders.TUS_RESUMABLE, tusVersion))
                    .andReturn()

            assertTrue(result.response.status == 200 || result.response.status == 204)
        }

        @Test
        fun test_head_mustNotBeCached() {
            // "the Server MUST ... and the response MUST include the Cache-Control: no-store header."
            val slot = UUID.randomUUID()
            whenever(uploadService.retrieveOffset(slot)).thenReturn(5L)

            mockMvc
                .perform(head("/api/apps/files/upload/{slot}", slot).header(TusHeaders.TUS_RESUMABLE, tusVersion))
                .andExpect(header().string("Cache-Control", "no-store"))
        }

        @Test
        fun test_head_nonExistentUpload_returnsNotFoundFamilyStatusWithoutUploadOffsetHeader() {
            // "If the resource is not found, the Server SHOULD return either the
            // 404 Not Found, 410 Gone or 403 Forbidden status without the Upload-Offset header."
            val slot = UUID.randomUUID()
            whenever(uploadService.retrieveOffset(slot)).thenThrow(UploadDoesNotExistException(slot))

            val result =
                mockMvc
                    .perform(head("/api/apps/files/upload/{slot}", slot).header(TusHeaders.TUS_RESUMABLE, tusVersion))
                    .andReturn()

            assertTrue(result.response.status in setOf(404, 410, 403))
            assertNull(result.response.getHeader(TusHeaders.UPLOAD_OFFSET))
        }

        @Test
        fun test_patch_wrongContentType_returns415UnsupportedMediaType() {
            // "All PATCH requests MUST use Content-Type: application/offset+octet-stream,
            // otherwise the server SHOULD return a 415 Unsupported Media Type status."
            val slot = UUID.randomUUID()

            mockMvc
                .perform(
                    patch("/api/apps/files/upload/{slot}", slot)
                        .header(TusHeaders.TUS_RESUMABLE, tusVersion)
                        .header(TusHeaders.UPLOAD_OFFSET, "0")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(ByteArray(0)),
                ).andExpect(status().isUnsupportedMediaType)
        }

        @Test
        fun test_patch_successfulUpload_returns204WithUpdatedUploadOffsetHeader() {
            // "The Server MUST acknowledge successful PATCH requests with the 204 No
            // Content status." + "It MUST include the Upload-Offset header containing
            // the new offset."
            val slot = UUID.randomUUID()
            val content = "hello".toByteArray()
            whenever(uploadService.continueUpload(eq(slot), eq(0L), any())).thenReturn(content.size.toLong())

            mockMvc
                .perform(
                    patch("/api/apps/files/upload/{slot}", slot)
                        .header(TusHeaders.TUS_RESUMABLE, tusVersion)
                        .header(TusHeaders.UPLOAD_OFFSET, "0")
                        .contentType("application/offset+octet-stream")
                        .content(content),
                ).andExpect(status().isNoContent)
                .andExpect(header().string(TusHeaders.UPLOAD_OFFSET, content.size.toString()))
        }

        @Test
        fun test_patch_offsetMismatch_returns409Conflict() {
            // "If the offsets do not match, the Server MUST respond with the
            // 409 Conflict status without modifying the upload resource."
            val slot = UUID.randomUUID()
            val upload = Upload(10L, "bob", "/report.pdf".into(), 5L).apply { id = slot }
            whenever(uploadService.continueUpload(eq(slot), eq(0L), any())).thenThrow(UploadWrongOffsetException(upload, 0L))

            mockMvc
                .perform(
                    patch("/api/apps/files/upload/{slot}", slot)
                        .header(TusHeaders.TUS_RESUMABLE, tusVersion)
                        .header(TusHeaders.UPLOAD_OFFSET, "0")
                        .contentType("application/offset+octet-stream")
                        .content(ByteArray(0)),
                ).andExpect(status().isConflict)
        }

        @Test
        fun test_patch_nonExistentUpload_returns404NotFound() {
            val slot = UUID.randomUUID()
            whenever(uploadService.continueUpload(eq(slot), any(), any())).thenThrow(UploadDoesNotExistException(slot))

            mockMvc
                .perform(
                    patch("/api/apps/files/upload/{slot}", slot)
                        .header(TusHeaders.TUS_RESUMABLE, tusVersion)
                        .header(TusHeaders.UPLOAD_OFFSET, "0")
                        .contentType("application/offset+octet-stream")
                        .content(ByteArray(0)),
                ).andExpect(status().isNotFound)
        }
    }

    @Nested
    inner class CreationExtension {
        @Test
        fun test_options_tusExtensionHeaderAnnouncesCreationSupport() {
            // "If this extension is supported by the Server, it MUST be announced by
            // adding its name to the Tus-Extension header" (creation extension supports POST).
            mockMvc
                .perform(options("/api/apps/files/upload"))
                .andExpect(header().string(TusHeaders.TUS_EXTENSION, Matchers.containsString("creation")))
        }

        @Test
        fun test_post_missingTusResumableHeader_returns412PreconditionFailed() {
            mockMvc
                .perform(post("/api/apps/files/upload"))
                .andExpect(status().isPreconditionFailed)
        }

        @Test
        fun test_post_validRequest_returns201CreatedWithLocationHeader() {
            // "The Server MUST acknowledge a successful upload creation with the
            // 201 Created status." + "The Server MUST set the Location header to the
            // URL of the created resource."
            val createdId = UUID.randomUUID()
            whenever(uploadService.createUpload(any()))
                .thenReturn(Upload(10L, "alice", "/doc.pdf".into()).apply { id = createdId })

            val result =
                mockMvc
                    .perform(
                        post("/api/apps/files/upload")
                            .header(TusHeaders.TUS_RESUMABLE, tusVersion)
                            .header(TusHeaders.UPLOAD_LENGTH, "10")
                            .header(TusHeaders.UPLOAD_METADATA, metadataHeader("user" to "alice", "path" to "/doc.pdf")),
                    ).andExpect(status().isCreated)
                    .andReturn()

            assertTrue(!result.response.getHeader("Location").isNullOrBlank())
        }

        @Test
        fun test_post_uploadLengthZero_isAccepted() {
            // "The Upload-Length header MAY be set to 0, indicating that the Client
            // wants to upload an empty file."
            whenever(uploadService.createUpload(any()))
                .thenReturn(Upload(0L, "alice", "/empty.txt".into()).apply { id = UUID.randomUUID() })

            mockMvc
                .perform(
                    post("/api/apps/files/upload")
                        .header(TusHeaders.TUS_RESUMABLE, tusVersion)
                        .header(TusHeaders.UPLOAD_LENGTH, "0")
                        .header(TusHeaders.UPLOAD_METADATA, metadataHeader("user" to "alice", "path" to "/empty.txt")),
                ).andExpect(status().isCreated)
        }

        @Test
        fun test_post_successfulResponse_includesTusResumableHeader() {
            whenever(uploadService.createUpload(any()))
                .thenReturn(Upload(10L, "alice", "/doc.pdf".into()).apply { id = UUID.randomUUID() })

            mockMvc
                .perform(
                    post("/api/apps/files/upload")
                        .header(TusHeaders.TUS_RESUMABLE, tusVersion)
                        .header(TusHeaders.UPLOAD_LENGTH, "10")
                        .header(TusHeaders.UPLOAD_METADATA, metadataHeader("user" to "alice", "path" to "/doc.pdf")),
                ).andExpect(header().string(TusHeaders.TUS_RESUMABLE, tusVersion))
        }
    }

    @Nested
    inner class TerminationExtension {
        @Test
        fun test_options_tusExtensionHeaderAnnouncesTerminationSupport() {
            // "If this extension is supported by the Server, it MUST be announced by
            // adding termination to the Tus-Extension header."
            mockMvc
                .perform(options("/api/apps/files/upload"))
                .andExpect(header().string(TusHeaders.TUS_EXTENSION, Matchers.containsString("termination")))
        }

        @Test
        fun test_delete_missingTusResumableHeader_returns412PreconditionFailed() {
            mockMvc
                .perform(delete("/api/apps/files/upload/{slot}", UUID.randomUUID()))
                .andExpect(status().isPreconditionFailed)
        }

        @Test
        fun test_delete_existingUpload_returns204NoContent() {
            // "the Server SHOULD free associated resources and MUST respond with the
            // 204 No Content status."
            val slot = UUID.randomUUID()

            mockMvc
                .perform(delete("/api/apps/files/upload/{slot}", slot).header(TusHeaders.TUS_RESUMABLE, tusVersion))
                .andExpect(status().isNoContent)
        }

        @Test
        fun test_delete_successfulResponse_includesTusResumableHeader() {
            val slot = UUID.randomUUID()

            mockMvc
                .perform(delete("/api/apps/files/upload/{slot}", slot).header(TusHeaders.TUS_RESUMABLE, tusVersion))
                .andExpect(header().string(TusHeaders.TUS_RESUMABLE, tusVersion))
        }

        @Test
        fun test_delete_nonExistentUpload_returnsNotFoundFamilyStatus() {
            // "For all future requests to this URL, the Server SHOULD respond with the
            // 404 Not Found or 410 Gone status."
            val slot = UUID.randomUUID()
            whenever(uploadService.deleteUpload(slot)).thenThrow(UploadDoesNotExistException(slot))

            val result =
                mockMvc
                    .perform(delete("/api/apps/files/upload/{slot}", slot).header(TusHeaders.TUS_RESUMABLE, tusVersion))
                    .andReturn()

            assertTrue(result.response.status in setOf(404, 410))
        }
    }
}
