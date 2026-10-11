package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.Utils
import me.polynom.polycloud.apps.files.Utils.createUpload
import me.polynom.polycloud.apps.files.Utils.into
import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageReadOnlyException
import me.polynom.polycloud.apps.files.exceptions.StorageWrongTypeException
import me.polynom.polycloud.apps.files.persistence.repository.UploadRepository
import me.polynom.polycloud.apps.files.storage.EntryMeta
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.mockito.verification.VerificationMode
import org.springframework.core.io.ByteArrayResource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class FilesServiceTest {
    @Mock
    lateinit var storage: Storage

    @Mock
    lateinit var storageService: StorageService

    @Mock
    lateinit var uploadRepository: UploadRepository

    val filesService: FilesService
        get() = FilesService(storageService, uploadRepository)

    @Test
    fun test_list_throwOnFolderPath() {
        assertThrows<StorageWrongTypeException> {
            filesService.list("test", "/folder/path".into())
        }

        verifyNoInteractions(storage)
        verifyNoInteractions(storageService)
        verifyNoInteractions(uploadRepository)
    }

    @Test
    fun test_list_mountsOnNullRoot() {
        whenever(storageService.listMounts("/".into()))
            .thenReturn(
                setOf(
                    "/nested1/".into(),
                    "/nested2/".into(),
                ),
            )

        // null backend
        whenever(storageService.resolveMount("/".into()))
            .thenReturn(Pair(null, "/".into()))

        assertEquals(
            listOf(
                EntryMeta("nested1", true, null),
                EntryMeta("nested2", true, null),
            ).sortedBy { it.name },
            filesService.list("bob", "/".into()).sortedBy { it.name },
        )
    }

    @Test
    fun test_list_throwOnEmptyEverything() {
        whenever(storageService.listMounts("/".into()))
            .thenReturn(setOf())

        // null backend
        whenever(storageService.resolveMount("/".into()))
            .thenReturn(Pair(null, "/".into()))

        assertThrows<StoragePathDoesNotExistException> {
            filesService.list("test", "/".into())
        }

        verifyNoInteractions(uploadRepository)
    }

    @Test
    fun test_list_throwOnFolderDoesNotExist() {
        whenever(storageService.listMounts("/sub/".into()))
            .thenReturn(setOf())

        whenever(storageService.resolveMount("/sub/".into()))
            .thenReturn(Pair(storage, "/sub/".into()))

        whenever(storage.list("alice", "/sub/".into()))
            .thenThrow(StoragePathDoesNotExistException("/sub/".into()))

        assertThrows<StoragePathDoesNotExistException> {
            filesService.list("alice", "/sub/".into())
        }
    }

    @Test
    fun test_list_someMountsButNoFolder() {
        whenever(storageService.listMounts("/sub/".into()))
            .thenReturn(
                setOf(
                    "/sub/nested1/".into(),
                    "/sub/nested2/".into(),
                ),
            )

        whenever(storageService.resolveMount("/sub/".into()))
            .thenReturn(Pair(storage, "/sub/".into()))

        whenever(storage.list("alice", "/sub/".into()))
            .thenThrow(StoragePathDoesNotExistException("/sub/".into()))

        assertEquals(
            listOf(
                EntryMeta("nested1", true, null),
                EntryMeta("nested2", true, null),
            ).sortedBy { it.name },
            filesService.list("alice", "/sub/".into()).sortedBy { it.name },
        )
    }

    @Test
    fun test_list_mountsAndFolderExists() {
        whenever(storageService.listMounts("/sub/".into()))
            .thenReturn(
                setOf(
                    "/sub/nested1/".into(),
                    "/sub/nested2/".into(),
                ),
            )

        whenever(storageService.resolveMount("/sub/".into()))
            .thenReturn(Pair(storage, "/sub/".into()))

        whenever(storage.list("alice", "/sub/".into()))
            .thenReturn(
                listOf(
                    EntryMeta("file1", false, 5L),
                    EntryMeta("folder", true, null),
                ),
            )

        assertEquals(
            listOf(
                EntryMeta("file1", false, 5L),
                EntryMeta("folder", true, null),
                EntryMeta("nested1", true, null),
                EntryMeta("nested2", true, null),
            ).sortedBy { it.name },
            filesService.list("alice", "/sub/".into()).sortedBy { it.name },
        )
    }

    @Test
    fun test_list_doNotDuplicate() {
        whenever(storageService.listMounts("/".into()))
            .thenReturn(setOf("/test/".into()))

        whenever(storageService.resolveMount("/".into()))
            .thenReturn(Pair(storage, "/".into()))

        whenever(storage.list("alice", "/".into()))
            .thenReturn(listOf(EntryMeta("test", true, null)))

        assertEquals(listOf(EntryMeta("test", true, null)), filesService.list("alice", "/".into()))
    }

    @Test
    fun test_list_doNotThrowOnEmptyFolderAndNoMounts() {
        whenever(storageService.listMounts("/sub/".into()))
            .thenReturn(setOf())

        whenever(storageService.resolveMount("/sub/".into()))
            .thenReturn(Pair(storage, "/sub/".into()))

        whenever(storage.list("alice", "/sub/".into()))
            .thenReturn(listOf())

        assertEquals(listOf(), filesService.list("alice", "/sub/".into()))
    }

    @Test
    fun test_read_simple() {
        val content = "Cool file contents".toByteArray()
        whenever(storageService.resolveMount("/something/something/test.mp4".into()))
            .thenReturn(Pair(storage, "/test.mp4".into()))
        whenever(storage.read("bob", "/test.mp4".into()))
            .thenReturn(ByteArrayResource(content))

        val res = filesService.read("bob", "/something/something/test.mp4".into())
        assertContentEquals(content, res.contentAsByteArray)

        verify(storage).read("bob", "/test.mp4".into())
        verifyNoInteractions(uploadRepository)
    }

    @Test
    fun test_read_nullStorage() {
        whenever(storageService.resolveMount("/should_not_exist.jpg".into()))
            .thenReturn(Pair(null, "/should_not_exist.jpg".into()))

        assertThrows<StoragePathDoesNotExistException> {
            filesService.read("user", "/should_not_exist.jpg".into())
        }

        verifyNoInteractions(storage)
        verifyNoInteractions(uploadRepository)
    }

    /*@Test
    fun test_put_simple() {
        val content = "Cool file contents".toByteArray()
        val upload = Utils.createUpload("bob", "/file.md".into(), content)
        whenever(uploadRepository.findById(upload.id!!)).thenReturn(upload)
        whenever()
    }*/

    // TODO: putFile

    @Test
    fun test_delete_simple() {
        whenever(storageService.resolveMount("/my_test/file.txt".into())).thenReturn(
            Pair(storage, "/file.txt".into()),
        )

        filesService.delete("user", "/my_test/file.txt".into())

        verify(storage).delete("user", "/file.txt".into())
        verifyNoInteractions(uploadRepository)
    }

    @Test
    fun test_delete_nullStorage() {
        whenever(storageService.resolveMount("/to_delete.md".into())).thenReturn(
            Pair(null, "/to_delete.md".into()),
        )

        assertThrows<StoragePathDoesNotExistException> {
            filesService.delete("user", "/to_delete.md".into())
        }

        verifyNoInteractions(storage)
        verifyNoInteractions(uploadRepository)
    }
}
