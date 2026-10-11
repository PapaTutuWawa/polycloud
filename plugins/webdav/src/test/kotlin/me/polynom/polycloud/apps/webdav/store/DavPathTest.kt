package me.polynom.polycloud.apps.webdav.store

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DavPathTest {
    @Test
    fun test_normalize_collapsesRepeatedSlashesAndTrimsTrailingSlash() {
        assertEquals("/a/b", DavPath.normalize("//a//b/"))
    }

    @Test
    fun test_normalize_root_staysRoot() {
        assertEquals("/", DavPath.normalize("/"))
        assertEquals("/", DavPath.normalize(""))
    }

    @Test
    fun test_parent_ofTopLevelPath_isRoot() {
        assertEquals("/", DavPath.parent("/a"))
    }

    @Test
    fun test_parent_ofNestedPath_isImmediateParent() {
        assertEquals("/a/b", DavPath.parent("/a/b/c"))
    }

    @Test
    fun test_parent_ofRoot_isRoot() {
        assertEquals("/", DavPath.parent("/"))
    }

    @Test
    fun test_name_ofNestedPath_isLastComponent() {
        assertEquals("c", DavPath.name("/a/b/c"))
    }

    @Test
    fun test_name_ofRoot_isEmpty() {
        assertEquals("", DavPath.name("/"))
    }

    @Test
    fun test_join_withRootParent_doesNotDoubleSlash() {
        assertEquals("/a", DavPath.join("/", "a"))
    }

    @Test
    fun test_join_withNestedParent_appendsSlash() {
        assertEquals("/a/b", DavPath.join("/a", "b"))
    }

    @Test
    fun test_isAncestorOf_strictDescendant_isTrue() {
        assertTrue(DavPath.isAncestorOf("/a", "/a/b"))
    }

    @Test
    fun test_isAncestorOf_samePath_isFalse() {
        assertFalse(DavPath.isAncestorOf("/a", "/a"))
    }

    @Test
    fun test_isAncestorOf_siblingWithSharedPrefix_isFalse() {
        assertFalse(DavPath.isAncestorOf("/a", "/ab"))
    }

    @Test
    fun test_isAncestorOf_rootIsAncestorOfAnythingButItself() {
        assertTrue(DavPath.isAncestorOf("/", "/a"))
        assertFalse(DavPath.isAncestorOf("/", "/"))
    }
}
