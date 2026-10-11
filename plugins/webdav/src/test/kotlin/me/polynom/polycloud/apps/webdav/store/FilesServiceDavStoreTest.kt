package me.polynom.polycloud.apps.webdav.store

import me.polynom.polycloud.apps.files.config.LocalStorageConfig
import me.polynom.polycloud.apps.files.config.StorageConfig
import me.polynom.polycloud.apps.files.persistence.repository.UploadRepository
import me.polynom.polycloud.apps.files.service.FilesService
import me.polynom.polycloud.apps.files.service.StorageService
import me.polynom.polycloud.apps.files.storage.StoragePath
import me.polynom.polycloud.apps.webdav.api.DavException
import me.polynom.polycloud.plugin.auth.UserContext
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilesServiceDavStoreTest {
    @TempDir
    lateinit var rootDir: Path

    private class FixedUserContext(
        private val username: String?,
    ) : UserContext {
        override fun getUser(): AuthVerificationResult? = username?.let { AuthVerificationResult(it, listOf("user")) }
    }

    private fun storeWithMountAtRoot(username: String = "alice"): FilesServiceDavStore {
        val storageService = StorageService(StorageConfig(listOf(LocalStorageConfig(rootDir, StoragePath("/")))))
        val filesService = FilesService(storageService, mock<UploadRepository>())
        return FilesServiceDavStore(filesService, storageService, FixedUserContext(username))
    }

    private fun storeWithTwoSeparateMounts(username: String = "alice"): FilesServiceDavStore {
        val mountA = rootDir.resolve("a-backend").createDirectories()
        val mountB = rootDir.resolve("b-backend").createDirectories()
        val storageService =
            StorageService(
                StorageConfig(
                    listOf(
                        LocalStorageConfig(mountA, StoragePath("/a/")),
                        LocalStorageConfig(mountB, StoragePath("/b/")),
                    ),
                ),
            )
        val filesService = FilesService(storageService, mock<UploadRepository>())
        return FilesServiceDavStore(filesService, storageService, FixedUserContext(username))
    }

    /** Mirrors application-ostylk.yaml's topology: a shared mount with real content, and a
     *  separate, non-shared mount nested one path segment deeper, whose per-user folder nobody
     *  has written to yet. */
    private fun storeWithNestedSeparateMount(username: String = "alice"): FilesServiceDavStore {
        val sharedBackend = rootDir.resolve("shared-backend").createDirectories()
        sharedBackend.resolve("existing.txt").also { it.toFile().writeText("hi") }
        val nestedBackend = rootDir.resolve("nested-backend").createDirectories()
        val storageService =
            StorageService(
                StorageConfig(
                    listOf(
                        LocalStorageConfig(nestedBackend, StoragePath("/fuck/cock/")),
                        LocalStorageConfig(sharedBackend, StoragePath("/fuck/"), shared = true),
                    ),
                ),
            )
        val filesService = FilesService(storageService, mock<UploadRepository>())
        return FilesServiceDavStore(filesService, storageService, FixedUserContext(username))
    }

    @Test
    fun test_children_and_get_includeASeparateMountNestedUnderAnotherMount() {
        val store = storeWithNestedSeparateMount()

        val children = store.children("/fuck")
        assertTrue(children.contains("/fuck/cock"), "expected /fuck/cock among $children")

        // This is the actual regression: PROPFIND Depth 1 calls get() on every child children()
        // reports, and a nested, never-written-to backend used to vanish from that response.
        assertNotNull(store.get("/fuck/cock"), "a freshly configured, unwritten nested mount must still exist")
        assertTrue(store.get("/fuck/cock") is DavCollection)
    }

    @Test
    fun test_get_root_isCollection() {
        val store = storeWithMountAtRoot()

        assertTrue(store.get("/") is DavCollection)
    }

    @Test
    fun test_get_virtualRoot_isCollectionEvenWithoutADirectMount() {
        val store = storeWithTwoSeparateMounts()

        assertTrue(store.get("/") is DavCollection)
        assertTrue(store.exists("/"))
    }

    @Test
    fun test_get_missingResource_isNull() {
        val store = storeWithMountAtRoot()

        assertNull(store.get("/missing.txt"))
        assertFalse(store.exists("/missing.txt"))
    }

    @Test
    fun test_putFile_thenGet_roundTripsContentAndInfersContentType() {
        val store = storeWithMountAtRoot()

        store.putFile("/report.pdf", "hello".toByteArray(), "application/octet-stream")
        val node = store.get("/report.pdf")

        assertNotNull(node)
        val file = node as DavFile
        assertEquals("application/pdf", file.contentType)
        assertContentEquals("hello".toByteArray(), file.resource.inputStream.readBytes())
    }

    @Test
    fun test_putFile_overExisting_overwritesContent() {
        val store = storeWithMountAtRoot()
        store.putFile("/a.txt", "old".toByteArray(), "text/plain")

        store.putFile("/a.txt", "new".toByteArray(), "text/plain")

        val file = store.get("/a.txt") as DavFile
        assertContentEquals("new".toByteArray(), file.resource.inputStream.readBytes())
    }

    @Test
    fun test_makeCollection_createsEmptyFolderVisibleViaChildren() {
        val store = storeWithMountAtRoot()

        store.makeCollection("/folder")

        assertTrue(store.get("/folder") is DavCollection)
        assertEquals(emptyList(), store.children("/folder"))
    }

    @Test
    fun test_children_listsFilesAndFolders() {
        val store = storeWithMountAtRoot()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        store.makeCollection("/sub")

        assertEquals(setOf("/a.txt", "/sub"), store.children("/").toSet())
    }

    @Test
    fun test_children_root_includesMountPointsWhenNotMountedDirectly() {
        val store = storeWithTwoSeparateMounts()

        assertEquals(setOf("/a", "/b"), store.children("/").toSet())
    }

    @Test
    fun test_children_onFile_isEmpty() {
        val store = storeWithMountAtRoot()
        store.putFile("/a.txt", ByteArray(0), "text/plain")

        assertEquals(emptyList(), store.children("/a.txt"))
    }

    @Test
    fun test_delete_removesFile() {
        val store = storeWithMountAtRoot()
        store.putFile("/a.txt", ByteArray(0), "text/plain")

        store.delete("/a.txt")

        assertFalse(store.exists("/a.txt"))
    }

    @Test
    fun test_delete_removesFolderRecursively() {
        val store = storeWithMountAtRoot()
        store.makeCollection("/folder")
        store.putFile("/folder/a.txt", ByteArray(0), "text/plain")

        store.delete("/folder")

        assertFalse(store.exists("/folder"))
        assertFalse(store.exists("/folder/a.txt"))
    }

    @Test
    fun test_delete_missing_throws404() {
        val store = storeWithMountAtRoot()

        val exception = assertThrows<DavException> { store.delete("/missing.txt") }

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status)
    }

    @Test
    fun test_copy_sameBackendFile_duplicatesContentAndKeepsSource() {
        val store = storeWithMountAtRoot()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")

        val replaced = store.copy("/a.txt", "/b.txt")

        assertFalse(replaced)
        assertTrue(store.exists("/a.txt"))
        assertContentEquals("hello".toByteArray(), (store.get("/b.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_copy_sameBackendFolderDeep_copiesDescendants() {
        val store = storeWithMountAtRoot()
        store.makeCollection("/a")
        store.putFile("/a/f.txt", "hi".toByteArray(), "text/plain")

        store.copy("/a", "/b", shallow = false)

        assertContentEquals("hi".toByteArray(), (store.get("/b/f.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_copy_sameBackendFolderShallow_doesNotCopyDescendants() {
        val store = storeWithMountAtRoot()
        store.makeCollection("/a")
        store.putFile("/a/f.txt", ByteArray(0), "text/plain")

        store.copy("/a", "/b", shallow = true)

        assertTrue(store.exists("/b"))
        assertFalse(store.exists("/b/f.txt"))
    }

    @Test
    fun test_copy_crossBackendFile_duplicatesContent() {
        val store = storeWithTwoSeparateMounts()
        store.putFile("/a/x.txt", "hello".toByteArray(), "text/plain")

        store.copy("/a/x.txt", "/b/x.txt")

        assertContentEquals("hello".toByteArray(), (store.get("/b/x.txt") as DavFile).resource.inputStream.readBytes())
        assertTrue(store.exists("/a/x.txt"))
    }

    @Test
    fun test_copy_crossBackendFolder_isNotImplemented() {
        val store = storeWithTwoSeparateMounts()
        store.makeCollection("/a/sub")

        val exception = assertThrows<DavException> { store.copy("/a/sub", "/b/sub") }

        assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), exception.status)
    }

    @Test
    fun test_move_sameBackend_relocatesAndRemovesSource() {
        val store = storeWithMountAtRoot()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")

        val replaced = store.move("/a.txt", "/b.txt")

        assertFalse(replaced)
        assertFalse(store.exists("/a.txt"))
        assertContentEquals("hello".toByteArray(), (store.get("/b.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_move_ontoExistingDestination_reportsReplaced() {
        val store = storeWithMountAtRoot()
        store.putFile("/a.txt", "new".toByteArray(), "text/plain")
        store.putFile("/b.txt", "old".toByteArray(), "text/plain")

        val replaced = store.move("/a.txt", "/b.txt")

        assertTrue(replaced)
        assertContentEquals("new".toByteArray(), (store.get("/b.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_move_crossBackendFile_relocatesContent() {
        val store = storeWithTwoSeparateMounts()
        store.putFile("/a/x.txt", "hello".toByteArray(), "text/plain")

        store.move("/a/x.txt", "/b/x.txt")

        assertFalse(store.exists("/a/x.txt"))
        assertContentEquals("hello".toByteArray(), (store.get("/b/x.txt") as DavFile).resource.inputStream.readBytes())
    }

    @Test
    fun test_unauthenticated_throwsUnauthorized() {
        val storageService = StorageService(StorageConfig(listOf(LocalStorageConfig(rootDir, StoragePath("/")))))
        val filesService = FilesService(storageService, mock<UploadRepository>())
        val store = FilesServiceDavStore(filesService, storageService, FixedUserContext(null))

        val exception = assertThrows<DavException> { store.get("/a.txt") }

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.status)
    }
}
