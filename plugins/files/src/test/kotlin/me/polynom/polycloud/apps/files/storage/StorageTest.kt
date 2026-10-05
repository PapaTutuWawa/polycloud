package me.polynom.polycloud.apps.files.storage

import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageWrongTypeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

interface StorageTest<out T : Storage> {
    fun createStorage(vararg files: Triple<String, StoragePath, ByteArray>): T

    @Test
    fun test_list_simpleFile() {
        val content = "Hello World".toByteArray()
        val storage =
            createStorage(
                Triple("ostylk", StoragePath("/test.txt"), content),
            )

        val ls = storage.list("ostylk", StoragePath("/"))
        assertEquals(listOf(EntryMeta("test.txt", false, content.size.toLong())), ls)
    }

    @Test
    fun test_list_filesAndFolders() {
        val content = "Hello World".toByteArray()
        val storage =
            createStorage(
                Triple("ostylk", StoragePath("/test/"), ByteArray(0)),
                Triple("ostylk", StoragePath("/test.txt"), content),
            )

        val ls = storage.list("ostylk", StoragePath("/"))
        assertEquals(
            listOf(
                EntryMeta("test", true, null),
                EntryMeta("test.txt", false, content.size.toLong()),
            ).toSet(),
            ls.toSet(),
        )
    }

    @Test
    fun test_list_fileFoldersUniqueToUsers() {
        val content1 = "Test Data 12345".toByteArray()
        val content2 = "Yoo Ragebait file".toByteArray()
        val storage =
            createStorage(
                Triple("ostylk", StoragePath("/secret/donotlook/secretfolder/"), ByteArray(0)),
                Triple("ostylk", StoragePath("/secret/donotlook/content.png"), content1),
                Triple("bob", StoragePath("/secret/donotlook/secretfolder/"), ByteArray(0)),
                Triple("bob", StoragePath("/secret/donotlook/cia.md"), content2),
            )

        val expectToplevel = listOf(EntryMeta("secret", true, null))
        val expectMiddle = listOf(EntryMeta("donotlook", true, null))

        assertEquals(expectToplevel, storage.list("ostylk", StoragePath("/")))
        assertEquals(expectToplevel, storage.list("bob", StoragePath("/")))
        assertEquals(expectMiddle, storage.list("ostylk", StoragePath("/secret/")))
        assertEquals(expectMiddle, storage.list("bob", StoragePath("/secret/")))
        assertEquals(
            listOf(
                EntryMeta("content.png", false, content1.size.toLong()),
                EntryMeta("secretfolder", true, null),
            ).toSet(),
            storage.list("ostylk", StoragePath("/secret/donotlook/")).toSet(),
        )
        assertEquals(
            listOf(
                EntryMeta("cia.md", false, content2.size.toLong()),
                EntryMeta("secretfolder", true, null),
            ).toSet(),
            storage.list("bob", StoragePath("/secret/donotlook/")).toSet(),
        )
    }

    @Test
    fun test_list_throwOnFilePath() {
        val storage =
            createStorage(
                Triple("bob", StoragePath("/file.txt"), ByteArray(0)),
                Triple("bob", StoragePath("/secret/"), ByteArray(0)),
            )

        assertThrows<StorageWrongTypeException> {
            storage.list("bob", StoragePath("/file.txt"))
        }

        assertThrows<StorageWrongTypeException> {
            storage.list("bob", StoragePath("/secret/doesnot/exist.txt"))
        }

        assertEquals(listOf(), storage.list("bob", StoragePath("/secret/")))
    }

    @Test
    fun test_list_throwOnNonExistentFolder() {
        val storage =
            createStorage(
                Triple("bob", StoragePath("/still/exists/"), ByteArray(0)),
            )

        assertThrows<StoragePathDoesNotExistException> {
            storage.list("bob", StoragePath("/still/exists/not/anymore/"))
        }
    }

    @Test
    fun test_read_simple() {
        val content = "According to all known laws of aviation...".toByteArray()
        val storage =
            createStorage(
                Triple("alice", StoragePath("/test.txt"), content),
            )

        val res = storage.read("alice", StoragePath("/test.txt"))
        assertContentEquals(content, res.contentAsByteArray)
    }

    @Test
    fun test_read_doNotConfuseUsers() {
        val content1 = "According to all known laws of aviation...".toByteArray()
        val content2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.".toByteArray()
        val storage =
            createStorage(
                Triple("alice", StoragePath("/important"), content1),
                Triple("bob", StoragePath("/important"), content2),
            )

        val res1 = storage.read("alice", StoragePath("/important"))
        assertContentEquals(content1, res1.contentAsByteArray)
        val res2 = storage.read("bob", StoragePath("/important"))
        assertContentEquals(content2, res2.contentAsByteArray)
    }

    @Test
    fun test_read_throwsOnFolderPath() {
        val content = "According to all known laws of aviation...".toByteArray()
        val storage =
            createStorage(
                Triple("alice", StoragePath("/test.txt"), content),
            )

        assertThrows<StorageWrongTypeException> {
            storage.read("alice", StoragePath("/"))
        }
        assertThrows<StorageWrongTypeException> {
            storage.read("alice", StoragePath("/test.txt/"))
        }
    }

    @Test
    fun test_read_throwsOnNonExistentFile() {
        val content = "According to all known laws of aviation...".toByteArray()
        val storage =
            createStorage(
                Triple("alice", StoragePath("/folderlol/"), ByteArray(0)),
                Triple("alice", StoragePath("/test.txt"), content),
            )

        assertThrows<StoragePathDoesNotExistException> {
            storage.read("alice", StoragePath("/folderlol"))
        }
        assertThrows<StoragePathDoesNotExistException> {
            storage.read("alice", StoragePath("/test.txtt"))
        }
    }

    @Test
    fun test_delete_simple() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/file1.txt"), ByteArray(0)),
                Triple("alice", StoragePath("/file2.txt"), ByteArray(0)),
            )

        assertEquals(
            listOf(
                EntryMeta("file1.txt", false, 0),
                EntryMeta("file2.txt", false, 0),
            ).toSet(),
            storage.list("alice", StoragePath("/")).toSet(),
        )
        storage.delete("alice", StoragePath("/file1.txt"))
        assertEquals(
            listOf(
                EntryMeta("file2.txt", false, 0),
            ),
            storage.list("alice", StoragePath("/")),
        )
    }

    @Test
    fun test_delete_doNotConfuseUsers() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/important"), ByteArray(0)),
                Triple("bob", StoragePath("/important"), ByteArray(0)),
            )

        val meta = EntryMeta("important", false, 0)

        assertEquals(listOf(meta), storage.list("alice", StoragePath("/")))
        assertEquals(listOf(meta), storage.list("bob", StoragePath("/")))
        storage.delete("bob", StoragePath("/important"))
        assertEquals(listOf(meta), storage.list("alice", StoragePath("/")))
        assertEquals(listOf(), storage.list("bob", StoragePath("/")))
    }

    @Test
    fun test_delete_throwsOnNonExistentFile() {
        val storage =
            createStorage(
                Triple("alice", StoragePath("/file1.txt"), ByteArray(0)),
            )

        assertThrows<StoragePathDoesNotExistException> {
            storage.delete("alice", StoragePath("/does/not/ever/exist/what.pdf"))
        }
    }

    @Test
    fun test_delete_throwsOnFilePathVsRealTypeConfusion() {
        val storage =
            createStorage(
                Triple("a", StoragePath("/folder/"), ByteArray(0)),
                Triple("a", StoragePath("/file"), ByteArray(0)),
            )

        assertEquals(
            listOf(
                EntryMeta("folder", true, null),
                EntryMeta("file", false, 0),
            ).toSet(),
            storage.list("a", StoragePath("/")).toSet(),
        )

        // First try deleting the folder but supply the path as file path.
        assertThrows<StoragePathDoesNotExistException> {
            storage.delete("a", StoragePath("/folder"))
        }
        assertEquals(
            listOf(
                EntryMeta("folder", true, null),
                EntryMeta("file", false, 0),
            ).toSet(),
            storage.list("a", StoragePath("/")).toSet(),
        )
        // Now try deleting it for real
        storage.delete("a", StoragePath("/folder/"))
        assertEquals(
            listOf(
                EntryMeta("file", false, 0),
            ),
            storage.list("a", StoragePath("/")),
        )

        // Also the other way around
        assertThrows<StoragePathDoesNotExistException> {
            storage.delete("a", StoragePath("/file/"))
        }
        assertEquals(
            listOf(
                EntryMeta("file", false, 0),
            ),
            storage.list("a", StoragePath("/")),
        )
        storage.delete("a", StoragePath("/file"))
        assertEquals(listOf(), storage.list("a", StoragePath("/")))
    }
}
