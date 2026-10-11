package me.polynom.polycloud.apps.files.storage

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StoragePathTest {
    @Test
    fun test_constructor_filePath_parsesComponentsAndMarksAsFile() {
        val path = StoragePath("/alice/documents/report.txt")

        assertEquals(listOf("alice", "documents", "report.txt"), path.components)
        assertTrue(path.file)
    }

    @Test
    fun test_constructor_folderPath_parsesComponentsAndMarksAsFolder() {
        val path = StoragePath("/alice/documents/")

        assertEquals(listOf("alice", "documents"), path.components)
        assertFalse(path.file)
    }

    @Test
    fun test_constructor_root_isEmptyFolder() {
        val path = StoragePath("/")

        assertEquals(emptyList(), path.components)
        assertFalse(path.file)
    }

    @Test
    fun test_constructor_collapsesRepeatedSlashesAndTrimsWhitespace() {
        val path = StoragePath("//alice// documents /report.txt")

        assertEquals(listOf("alice", "documents", "report.txt"), path.components)
        assertTrue(path.file)
    }

    @Test
    fun test_constructor_missingLeadingSlash_throws() {
        assertThrows<IllegalArgumentException> {
            StoragePath("alice/report.txt")
        }
    }

    @Test
    fun test_constructor_emptyRootFile_throws() {
        assertThrows<IllegalArgumentException> {
            StoragePath(emptyList(), true)
        }
    }

    @Test
    fun test_split_middleIndex_splitsFolderAndFile() {
        val path = StoragePath("/alice/documents/report.txt")

        val (left, right) = path.split(1)

        assertEquals(StoragePath(listOf("alice"), false), left)
        assertEquals(StoragePath(listOf("documents", "report.txt"), true), right)
    }

    @Test
    fun test_split_zeroIndex_leftIsRoot() {
        val path = StoragePath("/alice/report.txt")

        val (left, right) = path.split(0)

        assertEquals(StoragePath(emptyList(), false), left)
        assertEquals(path, right)
    }

    @Test
    fun test_split_fullLength_rightEmptyFolder() {
        val path = StoragePath("/alice/folder1/")

        val (left, right) = path.split(path.components.size)

        assertEquals(path, left)
        assertEquals(StoragePath("/"), right)
    }

    @Test
    fun test_split_fullLength_rightEmptyFile() {
        val path = StoragePath("/alice/report.txt")

        assertThrows<IllegalArgumentException> {
            path.split(path.components.size)
        }
    }

    @Test
    fun test_merge_folderWithFile_appendsComponentsAndAdoptsFileFlag() {
        val base = StoragePath("/alice/")
        val addition = StoragePath("/documents/report.txt")

        val merged = base.merge(addition)

        assertEquals(StoragePath("/alice/documents/report.txt"), merged)
    }

    @Test
    fun test_merge_folderWithFolder_staysFolder() {
        val base = StoragePath("/alice/")
        val addition = StoragePath("/documents/")

        val merged = base.merge(addition)

        assertEquals(StoragePath("/alice/documents/"), merged)
    }

    @Test
    fun test_merge_leftIsFile_throws() {
        val base = StoragePath("/alice/report.txt")
        val addition = StoragePath("/more.txt")

        assertThrows<IllegalArgumentException> {
            base.merge(addition)
        }
    }

    @Test
    fun test_folderDepth_filePath_excludesFileNameFromDepth() {
        val path = StoragePath("/alice/documents/report.txt")

        assertEquals(2, path.folderDepth())
    }

    @Test
    fun test_folderDepth_folderPath_includesAllComponents() {
        val path = StoragePath("/alice/documents/")

        assertEquals(2, path.folderDepth())
    }

    @Test
    fun test_folderDepth_rootFile_isZero() {
        val path = StoragePath("/report.txt")

        assertEquals(0, path.folderDepth())
    }

    @Test
    fun test_folder_filePath_returnsContainingFolder() {
        val path = StoragePath("/alice/documents/report.txt")

        assertEquals(StoragePath("/alice/documents/"), path.folder())
    }

    @Test
    fun test_folder_folderPath_returnsItself() {
        val path = StoragePath("/alice/documents/")

        assertEquals(path, path.folder())
    }

    @Test
    fun test_toString_filePath_hasNoTrailingSlash() {
        assertEquals("/alice/report.txt", StoragePath("/alice/report.txt").toString())
    }

    @Test
    fun test_toString_folderPath_hasTrailingSlash() {
        assertEquals("/alice/documents/", StoragePath("/alice/documents/").toString())
    }

    @Test
    fun test_toString_root_isSingleSlash() {
        assertEquals("/", StoragePath("/").toString())
    }
}
