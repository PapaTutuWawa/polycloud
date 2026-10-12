package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.Utils.into
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
        private val PATH_ROOT = "/".into()
        private val PATH_MOUNT1 = "/mount1/".into()
        private val PATH_MOUNT1_NESTED = "/mount1/nested/".into()
        private val PATH_MOUNT2 = "/mount2/".into()
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
        val (storage, relPath) = simpleStorage.resolveMount("/file/test.txt".into())

        assertNotNull(storage)
        assertEquals(PATH_ROOT, storage.mount)
        assertEquals("/file/test.txt".into(), relPath)
    }

    @Test
    fun test_resolveMount_rootless_success() {
        val (storage1, relPath1) = rootlessStorage.resolveMount("/".into())
        assertNull(storage1)
        assertEquals(relPath1, "/".into())

        val (storage2, relPath2) = rootlessStorage.resolveMount("/mount1/something/test.mp4".into())
        assertNotNull(storage2)
        assertEquals(PATH_MOUNT1, storage2.mount)
        assertEquals("/something/test.mp4".into(), relPath2)

        val (storage3, relPath3) = rootlessStorage.resolveMount("/mount2/yessir/".into())
        assertNotNull(storage3)
        assertEquals(PATH_MOUNT2, storage3.mount)
        assertEquals("/yessir/".into(), relPath3)

        assertNotEquals(storage2, storage3)
    }

    @Test
    fun test_resolveMount_nested_success() {
        val (storage1, relPath1) = nestedStorage.resolveMount("/test.txt".into())
        assertNull(storage1)
        assertEquals(relPath1, "/test.txt".into())

        val (storage2, relPath2) = nestedStorage.resolveMount("/mount1/test.txt".into())
        assertNotNull(storage2)
        assertEquals(PATH_MOUNT1, storage2.mount)
        assertEquals(relPath2, "/test.txt".into())

        val (storage3, relPath3) = nestedStorage.resolveMount("/mount1/nested/test.txt".into())
        assertNotNull(storage3)
        assertEquals(PATH_MOUNT1_NESTED, storage3.mount)
        assertEquals(relPath3, "/test.txt".into())

        assertNotEquals(storage2, storage3)
    }

    @Test
    fun test_resolveMount_rootless_noMatch() {
        val (storage, relPath) = rootlessStorage.resolveMount("/totally/unrelated/deep/path.txt".into())

        assertNull(storage)
        assertEquals("/totally/unrelated/deep/path.txt".into(), relPath)
    }

    @Test
    fun test_resolveMount_mountBoundary_fileVsFolder() {
        val (storage1, relPath1) = rootlessStorage.resolveMount("/mount1/".into())
        assertNotNull(storage1)
        assertEquals(PATH_MOUNT1, storage1.mount)
        assertEquals("/".into(), relPath1)

        val (storage2, relPath2) = rootlessStorage.resolveMount("/mount1".into())
        assertNull(storage2)
        assertEquals("/mount1".into(), relPath2)
    }

    @Test
    fun test_resolveMount_emptyConfig() {
        val (storage, relPath) = storageService().resolveMount("/some/path.txt".into())

        assertNull(storage)
        assertEquals("/some/path.txt".into(), relPath)
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
        assertEquals(setOf(), simpleStorage.listMounts("/".into()))
    }

    @Test
    fun test_listMounts_rootless() {
        assertEquals(setOf(PATH_MOUNT1, PATH_MOUNT2), rootlessStorage.listMounts("/".into()))
        assertEquals(setOf(PATH_MOUNT1, PATH_MOUNT2), rootlessStorage.listMounts("/does_not_exist".into()))
        assertEquals(setOf(), rootlessStorage.listMounts(PATH_MOUNT1))
        assertEquals(setOf(), rootlessStorage.listMounts("/random_folder/".into()))
    }

    @Test
    fun test_listMounts_nested() {
        assertEquals(setOf(PATH_MOUNT1), nestedStorage.listMounts("/".into()))
    }

    @Test
    fun test_listMounts_simpleNested() {
        assertEquals(
            setOf(PATH_MOUNT1),
            storageService(
                LocalStorageConfig(Path("."), PATH_MOUNT1_NESTED),
            ).listMounts("/".into()),
        )
    }

    @Test
    fun test_listMounts_emptyConfig() {
        assertEquals(setOf(), storageService().listMounts("/".into()))
    }

    @Test
    fun test_listMounts_complex() {
        val storage =
            storageService(
                LocalStorageConfig(Path("folder1"), "/a/mount1/".into()),
                LocalStorageConfig(Path("folder2"), "/a/mount2/".into()),
                LocalStorageConfig(Path("folder3"), "/b/yes1/".into()),
                LocalStorageConfig(Path("folder4"), "/b/yes2/".into()),
                LocalStorageConfig(Path("folder4"), "/b/yes2/nested/".into()),
            )
        assertEquals(setOf("/mount1/".into(), "/mount2/".into()), storage.listMounts("/a/".into()))
        assertEquals(setOf("/yes1/".into(), "/yes2/".into()), storage.listMounts("/b/yes2".into()))
        assertEquals(setOf("/nested/".into()), storage.listMounts("/b/yes2/".into()))
    }
}
