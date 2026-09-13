package me.polynom.polycloud.auth

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the {@link PathTrie} class.
 */
class PathTrieTests {
    @Test
    fun testTrieAddingAndTraversal() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/a/b/c", true)

        val trie = pathTrie.traverse("/a/b/c")
        assertNotNull(trie)
    }

    @Test
    fun testTrieAddingAndTraversalNotFound() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/a/b/c", true)

        val trie = pathTrie.traverse("/a/b/c/d")
        assertNull(trie)
    }

    @Test
    fun testSingleWildcardTraversal() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/a/*/c", true)

        assertNotNull(pathTrie.traverse("/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/b/c"))
        assertNotNull(pathTrie.traverse("/a/c/c"))
        assertNull(pathTrie.traverse("/a/c/c/d"))
        assertNull(pathTrie.traverse("/a"))
    }

    @Test
    fun testSingleWildcardTraversalWithOtherPath() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/a/*/c", true)
        pathTrie.addPath("/a/b/e", true)

        assertNotNull(pathTrie.traverse("/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/b/e"))
    }

    @Test
    fun testMultiWildcardTraversal() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/a/**/c", true)

        pathTrie.debug()

        assertNotNull(pathTrie.traverse("/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/a/a/a/c"))
        assertNull(pathTrie.traverse("/a/a"))
        assertNull(pathTrie.traverse("/a/a/b"))
    }

    @Test
    fun testOpenMultiWildcardTraversal() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/a/**", true)

        assertNull(pathTrie.traverse("/a"))
        assertNotNull(pathTrie.traverse("/a/a"))
        assertNotNull(pathTrie.traverse("/a/a/a"))
        assertNotNull(pathTrie.traverse("/a/a/a/a"))
    }

    @Test
    fun testMultipleRoutes() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/a/b/c", true)
        pathTrie.addPath("/a/b/d", true)
        pathTrie.addPath("/c", true)

        assertNotNull(pathTrie.traverse("/a/b/c"))
        assertNull(pathTrie.traverse("/a/b/c/d"))
        assertNull(pathTrie.traverse("/a/b"))

        assertNotNull(pathTrie.traverse("/a/b/d"))
        assertNull(pathTrie.traverse("/a/b/d/e"))
        assertNull(pathTrie.traverse("/a/b"))

        assertNotNull(pathTrie.traverse("/c"))
        assertNull(pathTrie.traverse("/c/a"))
    }

    @Test
    fun testCatchAll() {
        val pathTrie = PathTrie("root")
        pathTrie.addPath("/**", true)
        pathTrie.addPath("/a/b/d", false)

        assertTrue(pathTrie.traverse("/a/b/c")!!.getIsAuthenticated())
        assertTrue(pathTrie.traverse("/a/b")!!.getIsAuthenticated())
        assertTrue(!pathTrie.traverse("/a/b/d")!!.getIsAuthenticated())
    }
}
