package me.polynom.polycloud.auth

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the {@link PathTrie} class.
 */
class AuthorizationConfigTests {
    @Test
    fun testTrieAddingAndTraversal() {
        val pathTrie = PathTrie()
        pathTrie.addPath("/a/b/c")

        val trie = pathTrie.traverse("/a/b/c")
        assertNotNull(trie)
    }

    @Test
    fun testTrieAddingAndTraversalNotFound() {
        val pathTrie = PathTrie()
        pathTrie.addPath("/a/b/c")

        val trie = pathTrie.traverse("/a/b/c/d")
        assertNull(trie)
    }

    @Test
    fun testSingleWildcardTraversal() {
        val pathTrie = PathTrie()
        pathTrie.addPath("/a/*/c")

        assertNotNull(pathTrie.traverse("/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/b/c"))
        assertNotNull(pathTrie.traverse("/a/c/c"))
        assertNull(pathTrie.traverse("/a/c/c/d"))
        assertNull(pathTrie.traverse("/a"))
    }

    @Test
    fun testSingleWildcardTraversalWithOtherPath() {
        val pathTrie = PathTrie()
        pathTrie.addPath("/a/*/c")
        pathTrie.addPath("/a/b/e")

        assertNotNull(pathTrie.traverse("/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/b/e"))
    }

    @Test
    fun testMultiWildcardTraversal() {
        val pathTrie = PathTrie()
        pathTrie.addPath("/a/**/c")

        assertNotNull(pathTrie.traverse("/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/a/a/c"))
        assertNotNull(pathTrie.traverse("/a/a/a/a/c"))
        assertNull(pathTrie.traverse("/a/a"))
        assertNull(pathTrie.traverse("/a/a/b"))
    }

    @Test
    fun testOpenMultiWildcardTraversal() {
        val pathTrie = PathTrie()
        pathTrie.addPath("/a/**")

        assertNull(pathTrie.traverse("/a"))
        assertNotNull(pathTrie.traverse("/a/a"))
        assertNotNull(pathTrie.traverse("/a/a/a"))
        assertNotNull(pathTrie.traverse("/a/a/a/a"))
    }

    @Test
    fun testMultipleRoutes() {
        val pathTrie = PathTrie()
        pathTrie.addPath("/a/b/c")
        pathTrie.addPath("/a/b/d")
        pathTrie.addPath("/c")

        assertNotNull(pathTrie.traverse("/a/b/c"))
        assertNull(pathTrie.traverse("/a/b/c/d"))
        assertNull(pathTrie.traverse("/a/b"))

        assertNotNull(pathTrie.traverse("/a/b/d"))
        assertNull(pathTrie.traverse("/a/b/d/e"))
        assertNull(pathTrie.traverse("/a/b"))

        assertNotNull(pathTrie.traverse("/c"))
        assertNull(pathTrie.traverse("/c/a"))
    }
}
