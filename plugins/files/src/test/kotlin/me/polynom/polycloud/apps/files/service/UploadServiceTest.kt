package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.Utils.into
import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.UploadDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.UploadWrongOffsetException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.persistence.repository.UploadRepository
import me.polynom.polycloud.apps.files.storage.Storage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class UploadServiceTest {
    @Mock
    lateinit var storage: Storage

    @Mock
    lateinit var storageService: StorageService

    @Mock
    lateinit var uploadRepository: UploadRepository

    val uploadService: UploadService
        get() = UploadService(uploadRepository, storageService)

    @Test
    fun test_createUpload_throwOnNullBackends() {
        val upload = Upload(12L, "alice", "/huhu.c".into())
        whenever(storageService.resolveMount(upload.path))
            .thenReturn(Pair(null, "/huhu.c".into()))

        assertThrows<StoragePathDoesNotExistException> {
            uploadService.createUpload(upload)
        }
    }

    @Test
    fun test_createUpload_success() {
        val upload = Upload(12L, "alice", "/huhu.c".into())
        whenever(storageService.resolveMount(upload.path))
            .thenReturn(Pair(storage, "/huhu.c".into()))
        whenever(uploadRepository.save(upload))
            .thenReturn(upload.apply { id = UUID.randomUUID() })

        val entity = uploadService.createUpload(upload)

        // Make sure the upload has been saved and the storage backend notified.
        verify(uploadRepository).save(upload)
        verify(storage).prepareUpload(upload)
        assertNotNull(entity.id)
    }

    @Test
    fun test_deleteUpload_throwOnMissingUpload() {
        whenever(uploadRepository.findById(any()))
            .thenReturn(Optional.empty())

        assertThrows<UploadDoesNotExistException> {
            uploadService.deleteUpload(UUID.randomUUID())
        }
    }

    @Test
    fun test_deleteUpload_noBackend() {
        val upload = Upload(6L, "bob", "/test.mp4".into()).apply { id = UUID.randomUUID() }
        whenever(uploadRepository.findById(upload.id!!))
            .thenReturn(Optional.of(upload))

        whenever(storageService.resolveMount(upload.path))
            .thenReturn(Pair(null, "/test.mp4".into()))

        uploadService.deleteUpload(upload.id!!)

        verify(uploadRepository).delete(upload)
    }

    @Test
    fun test_deleteUpload_success() {
        val upload = Upload(6L, "bob", "/test.mp4".into()).apply { id = UUID.randomUUID() }
        whenever(uploadRepository.findById(upload.id!!))
            .thenReturn(Optional.of(upload))

        whenever(storageService.resolveMount(upload.path))
            .thenReturn(Pair(storage, "/test.mp4".into()))

        uploadService.deleteUpload(upload.id!!)

        verify(storage).deleteUpload(upload)
        verify(uploadRepository).delete(upload)
    }

    @Test
    fun test_retrieveOffset_throwOnMissingUpload() {
        whenever(uploadRepository.findById(any()))
            .thenReturn(Optional.empty())

        assertThrows<UploadDoesNotExistException> {
            uploadService.retrieveOffset(UUID.randomUUID())
        }
    }

    @Test
    fun test_retrieveOffset_success() {
        val upload = Upload(128L, "alice", "/what.txt".into(), offset = 42L).apply { id = UUID.randomUUID() }
        whenever(uploadRepository.findById(upload.id!!))
            .thenReturn(Optional.of(upload))

        assertEquals(42L, uploadService.retrieveOffset(upload.id!!))
    }

    @Test
    fun test_continueUpload_throwOnMissingUpload() {
        whenever(uploadRepository.findById(any()))
            .thenReturn(Optional.empty())

        assertThrows<UploadDoesNotExistException> {
            uploadService.continueUpload(UUID.randomUUID(), 4L, ByteArrayInputStream(ByteArray(0)))
        }
    }

    @Test
    fun test_continueUpload_throwOnWrongOffset() {
        val upload = Upload(256L, "bob", "/yooo.txt".into(), offset = 42L).apply { id = UUID.randomUUID() }
        whenever(uploadRepository.findById(upload.id!!))
            .thenReturn(Optional.of(upload))

        assertThrows<UploadWrongOffsetException> {
            uploadService.continueUpload(upload.id!!, 4L, ByteArrayInputStream(ByteArray(0)))
        }
    }

    @Test
    fun test_continueUpload_throwOnNullBackend() {
        val upload = Upload(256L, "bob", "/yooo.txt".into(), offset = 42L).apply { id = UUID.randomUUID() }
        whenever(uploadRepository.findById(upload.id!!))
            .thenReturn(Optional.of(upload))

        whenever(storageService.resolveMount(upload.path))
            .thenReturn(Pair(null, "/yooo.txt".into()))

        assertThrows<StoragePathDoesNotExistException> {
            uploadService.continueUpload(upload.id!!, upload.offset, ByteArrayInputStream(ByteArray(0)))
        }
    }

    @Test
    fun test_continueUpload_success() {
        val upload = Upload(256L, "bob", "/yooo.txt".into(), offset = 42L).apply { id = UUID.randomUUID() }
        whenever(uploadRepository.findById(upload.id!!))
            .thenReturn(Optional.of(upload))

        whenever(storageService.resolveMount(upload.path))
            .thenReturn(Pair(storage, "/yooo.txt".into()))

        whenever(storage.continueUpload(eq(upload), any()))
            .then { mock ->
                val up = mock.getArgument<Upload>(0)
                val rx = mock.getArgument<InputStream>(1)

                up.offset += rx.available().toLong()
                return@then up.offset
            }

        assertEquals(
            42L + 8L,
            uploadService.continueUpload(
                upload.id!!,
                upload.offset,
                ByteArrayInputStream(ByteArray(8)),
            ),
        )
        assertEquals(42L + 8L, upload.offset)

        verify(uploadRepository).save(upload)
    }
}
