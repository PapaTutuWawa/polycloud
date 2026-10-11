package me.polynom.polycloud.apps.webdav.store

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryDavStoreTest {
    @Test
    fun test_root_existsAndIsACollection() {
        val store = InMemoryDavStore()

        assertTrue(store.exists(DavPath.ROOT))
        assertTrue(store.get(DavPath.ROOT) is DavCollection)
    }

    @Test
    fun test_putFile_onUnknownPath_createsIt() {
        val store = InMemoryDavStore()

        val file = store.putFile("/a.txt", "hello".toByteArray(), "text/plain")

        assertTrue(store.exists("/a.txt"))
        assertContentEquals("hello".toByteArray(), (store.get("/a.txt") as DavFile).content)
        assertEquals("text/plain", file.contentType)
    }

    @Test
    fun test_putFile_overExistingFile_overwritesContent() {
        val store = InMemoryDavStore()
        store.putFile("/a.txt", "old".toByteArray(), "text/plain")

        store.putFile("/a.txt", "new".toByteArray(), "text/plain")

        assertContentEquals("new".toByteArray(), (store.get("/a.txt") as DavFile).content)
    }

    @Test
    fun test_makeCollection_addsEmptyCollection() {
        val store = InMemoryDavStore()

        store.makeCollection("/folder")

        assertTrue(store.get("/folder") is DavCollection)
        assertEquals(emptyList(), store.children("/folder"))
    }

    @Test
    fun test_children_onlyReturnsDirectChildren() {
        val store = InMemoryDavStore()
        store.makeCollection("/a")
        store.makeCollection("/a/b")
        store.putFile("/a/b/c.txt", ByteArray(0), "text/plain")

        assertEquals(listOf("/a/b"), store.children("/a"))
        assertEquals(listOf("/a/b/c.txt"), store.children("/a/b"))
    }

    @Test
    fun test_children_onFile_isEmpty() {
        val store = InMemoryDavStore()
        store.putFile("/a.txt", ByteArray(0), "text/plain")

        assertEquals(emptyList(), store.children("/a.txt"))
    }

    @Test
    fun test_delete_onFile_removesOnlyThatFile() {
        val store = InMemoryDavStore()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        store.putFile("/b.txt", ByteArray(0), "text/plain")

        store.delete("/a.txt")

        assertFalse(store.exists("/a.txt"))
        assertTrue(store.exists("/b.txt"))
    }

    @Test
    fun test_delete_onCollection_removesDescendantsRecursively() {
        val store = InMemoryDavStore()
        store.makeCollection("/a")
        store.makeCollection("/a/b")
        store.putFile("/a/b/c.txt", ByteArray(0), "text/plain")

        store.delete("/a")

        assertFalse(store.exists("/a"))
        assertFalse(store.exists("/a/b"))
        assertFalse(store.exists("/a/b/c.txt"))
    }

    @Test
    fun test_copy_file_duplicatesContentUnderNewPath() {
        val store = InMemoryDavStore()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")

        val replaced = store.copy("/a.txt", "/b.txt")

        assertFalse(replaced)
        assertContentEquals("hello".toByteArray(), (store.get("/b.txt") as DavFile).content)
        assertTrue(store.exists("/a.txt"), "source must still exist after copy")
    }

    @Test
    fun test_copy_collectionDeep_copiesAllDescendants() {
        val store = InMemoryDavStore()
        store.makeCollection("/a")
        store.putFile("/a/f.txt", "hi".toByteArray(), "text/plain")

        store.copy("/a", "/b", shallow = false)

        assertTrue(store.get("/b") is DavCollection)
        assertContentEquals("hi".toByteArray(), (store.get("/b/f.txt") as DavFile).content)
    }

    @Test
    fun test_copy_collectionShallow_doesNotCopyDescendants() {
        val store = InMemoryDavStore()
        store.makeCollection("/a")
        store.putFile("/a/f.txt", "hi".toByteArray(), "text/plain")

        store.copy("/a", "/b", shallow = true)

        assertTrue(store.get("/b") is DavCollection)
        assertFalse(store.exists("/b/f.txt"))
    }

    @Test
    fun test_copy_ontoExistingDestination_reportsReplacedAndDropsOldDescendants() {
        val store = InMemoryDavStore()
        store.makeCollection("/old")
        store.putFile("/old/stale.txt", ByteArray(0), "text/plain")
        store.putFile("/a.txt", "fresh".toByteArray(), "text/plain")

        val replaced = store.copy("/a.txt", "/old")

        assertTrue(replaced)
        assertContentEquals("fresh".toByteArray(), (store.get("/old") as DavFile).content)
        assertFalse(store.exists("/old/stale.txt"))
    }

    @Test
    fun test_copy_missingSource_doesNothingAndReportsWhetherDestinationExisted() {
        val store = InMemoryDavStore()

        val replaced = store.copy("/missing", "/also-missing")

        assertFalse(replaced)
        assertNull(store.get("/also-missing"))
    }

    @Test
    fun test_move_relocatesNodeAndDeletesSource() {
        val store = InMemoryDavStore()
        store.putFile("/a.txt", "hello".toByteArray(), "text/plain")

        val replaced = store.move("/a.txt", "/b.txt")

        assertFalse(replaced)
        assertFalse(store.exists("/a.txt"))
        assertContentEquals("hello".toByteArray(), (store.get("/b.txt") as DavFile).content)
    }

    @Test
    fun test_move_collection_relocatesDescendantsToo() {
        val store = InMemoryDavStore()
        store.makeCollection("/a")
        store.putFile("/a/f.txt", "hi".toByteArray(), "text/plain")

        store.move("/a", "/b")

        assertFalse(store.exists("/a"))
        assertFalse(store.exists("/a/f.txt"))
        assertTrue(store.get("/b") is DavCollection)
        assertContentEquals("hi".toByteArray(), (store.get("/b/f.txt") as DavFile).content)
    }

    @Test
    fun test_copy_preservesButDoesNotShareDeadPropertiesBetweenSourceAndDestination() {
        val store = InMemoryDavStore()
        store.putFile("/a.txt", ByteArray(0), "text/plain")
        val name = DavPropertyName("urn:test", "label")
        store.get("/a.txt")!!.deadProperties[name] = "original"

        store.copy("/a.txt", "/b.txt")
        store.get("/b.txt")!!.deadProperties[name] = "changed"

        assertEquals("original", store.get("/a.txt")!!.deadProperties[name])
        assertEquals("changed", store.get("/b.txt")!!.deadProperties[name])
    }
}
