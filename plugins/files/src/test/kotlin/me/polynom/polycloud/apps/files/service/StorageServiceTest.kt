package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.config.LocalStorageConfig
import me.polynom.polycloud.apps.files.config.StorageConfig
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StorageServiceTest {
    companion object {
        private val PATH_ROOT = StoragePath("/")
        private val PATH_MOUNT1 = StoragePath("/mount1/")
        private val PATH_MOUNT1_NESTED = StoragePath("/mount1/nested/")
        private val PATH_MOUNT2 = StoragePath("/mount2/")
    }

    private fun storageService(vararg locals: LocalStorageConfig) = StorageService(StorageConfig(locals.asList()))

    private val simpleStorage =
        storageService(
            LocalStorageConfig(Path("."), PATH_ROOT),
        )

    private val rootlessStorage =
        storageService(
            LocalStorageConfig(Path("."), PATH_MOUNT1),
            LocalStorageConfig(Path("."), PATH_MOUNT2),
        )

    private val nestedStorage =
        storageService(
            LocalStorageConfig(Path("."), PATH_MOUNT1),
            LocalStorageConfig(Path("."), PATH_MOUNT1_NESTED),
        )

    @Test
    fun test_resolveMount_simple_success() {
        val (storage, relPath) = simpleStorage.resolveMount(StoragePath("/file/test.txt"))

        assertNotNull(storage)
        assertEquals(PATH_ROOT, storage.mount)
        assertEquals(StoragePath("/file/test.txt"), relPath)
    }

    @Test
    fun test_resolveMount_rootless_success() {
        val (storage1, relPath1) = rootlessStorage.resolveMount(StoragePath("/"))
        assertNull(storage1)
        assertEquals(relPath1, StoragePath("/"))

        val (storage2, relPath2) = rootlessStorage.resolveMount(StoragePath("/mount1/something/test.mp4"))
        assertNotNull(storage2)
        assertEquals(PATH_MOUNT1, storage2.mount)
        assertEquals(StoragePath("/something/test.mp4"), relPath2)

        val (storage3, relPath3) = rootlessStorage.resolveMount(StoragePath("/mount2/yessir/"))
        assertNotNull(storage3)
        assertEquals(PATH_MOUNT2, storage3.mount)
        assertEquals(StoragePath("/yessir/"), relPath3)

        assertNotEquals(storage2, storage3)
    }

    @Test
    fun test_resolveMount_nested_success() {
        val (storage1, relPath1) = nestedStorage.resolveMount(StoragePath("/test.txt"))
        assertNull(storage1)
        assertEquals(relPath1, StoragePath("/test.txt"))

        val (storage2, relPath2) = nestedStorage.resolveMount(StoragePath("/mount1/test.txt"))
        assertNotNull(storage2)
        assertEquals(PATH_MOUNT1, storage2.mount)
        assertEquals(relPath2, StoragePath("/test.txt"))

        val (storage3, relPath3) = nestedStorage.resolveMount(StoragePath("/mount1/nested/test.txt"))
        assertNotNull(storage3)
        assertEquals(PATH_MOUNT1_NESTED, storage3.mount)
        assertEquals(relPath3, StoragePath("/test.txt"))

        assertNotEquals(storage2, storage3)
    }

    @Test
    fun test_resolveMount_rootless_noMatch() {
        val (storage, relPath) = rootlessStorage.resolveMount(StoragePath("/totally/unrelated/deep/path.txt"))

        assertNull(storage)
        assertEquals(StoragePath("/totally/unrelated/deep/path.txt"), relPath)
    }

    @Test
    fun test_resolveMount_mountBoundary_fileVsFolder() {
        val (storage1, relPath1) = rootlessStorage.resolveMount(StoragePath("/mount1/"))
        assertNotNull(storage1)
        assertEquals(PATH_MOUNT1, storage1.mount)
        assertEquals(StoragePath("/"), relPath1)

        val (storage2, relPath2) = rootlessStorage.resolveMount(StoragePath("/mount1"))
        assertNull(storage2)
        assertEquals(StoragePath("/mount1"), relPath2)
    }

    @Test
    fun test_resolveMount_emptyConfig() {
        val (storage, relPath) = storageService().resolveMount(StoragePath("/some/path.txt"))

        assertNull(storage)
        assertEquals(StoragePath("/some/path.txt"), relPath)
    }

    @Test
    fun test_init_config_bogus1() {
        assertThrows<IllegalArgumentException> {
            storageService(
                LocalStorageConfig(Path("folder1"), PATH_ROOT),
                LocalStorageConfig(Path("folder2"), PATH_ROOT),
            )
        }
    }

    @Test
    fun test_init_config_bogus2() {
        assertThrows<IllegalArgumentException> {
            storageService(
                LocalStorageConfig(Path("folder1"), PATH_MOUNT1),
                LocalStorageConfig(Path("folder2"), PATH_MOUNT1_NESTED),
                LocalStorageConfig(Path("folder2"), PATH_MOUNT1),
            )
        }
    }

    @Test
    fun test_listMounts_simple() {
        assertEquals(listOf(), simpleStorage.listMounts(StoragePath("/")))
    }

    @Test
    fun test_listMounts_rootless() {
        assertEquals(listOf(PATH_MOUNT1, PATH_MOUNT2), rootlessStorage.listMounts(StoragePath("/")))
        assertEquals(listOf(PATH_MOUNT1, PATH_MOUNT2), rootlessStorage.listMounts(StoragePath("/does_not_exist")))
        assertEquals(listOf(), rootlessStorage.listMounts(PATH_MOUNT1))
        assertEquals(listOf(), rootlessStorage.listMounts(StoragePath("/random_folder/")))
    }

    @Test
    fun test_listMounts_emptyConfig() {
        assertEquals(listOf(), storageService().listMounts(StoragePath("/")))
    }

    @Test
    fun test_listMounts_complex() {
        val storage =
            storageService(
                LocalStorageConfig(Path("folder1"), StoragePath("/a/mount1/")),
                LocalStorageConfig(Path("folder2"), StoragePath("/a/mount2/")),
                LocalStorageConfig(Path("folder3"), StoragePath("/b/yes1/")),
                LocalStorageConfig(Path("folder4"), StoragePath("/b/yes2/")),
                LocalStorageConfig(Path("folder4"), StoragePath("/b/yes2/nested/")),
            )
        assertEquals(listOf(StoragePath("/mount1/"), StoragePath("/mount2/")), storage.listMounts(StoragePath("/a/")))
        assertEquals(listOf(StoragePath("/yes1/"), StoragePath("/yes2/")), storage.listMounts(StoragePath("/b/yes2")))
        assertEquals(listOf(StoragePath("/nested/")), storage.listMounts(StoragePath("/b/yes2/")))
    }
}
