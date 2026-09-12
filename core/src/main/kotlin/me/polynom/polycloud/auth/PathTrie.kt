package me.polynom.polycloud.auth

/**
 * A trie that that works on "/" separated paths.
 */
class PathTrie(
    private val segment: String,
) {
    /** The mapping of path segments to the next node in the trie. */
    private val mapping: MutableMap<String, PathTrie> = mutableMapOf()

    /** The ending path segment of a multi wildcard. */
    private var multiWildcardEnd: String? = null

    /** Indicator if this node is the terminal for a path. */
    private var isTerminal: Boolean = false

    /** Indicator if this node should require authentication or not. */
    private var isAuthenticated: Boolean = true

    /** Getter for the isAuthenticated flag. */
    internal fun getIsAuthenticated(): Boolean = isAuthenticated

    /**
     * Gets the next trie node from the mapping.
     *
     * @param segment   The next segment.
     * @return The next trie node or null, if the segment does not exist in the node.
     */
    fun getTrie(segment: String): PathTrie? {
        return mapping[segment]
    }

    fun debug(indent: Int = 0) {
        val textIndent = " ".repeat(4 * indent)
        println("$textIndent[[$isTerminal]]")
        mapping.forEach { (key, trie) ->
            println("${textIndent}$key:")
            trie.debug(indent + 1)
        }
    }

    /**
     * Adds a new trie node into this one with the provided path segment.
     * singleSegmentWildcard and multiSegmentWildcard are updated appropriately, if segment is "*" or
     * "**". Should the next path segment be non-existent, then the new node is also marked as a terminal.
     *
     * @param segment       The segment that should point to the new trie node.
     * @param nextSegment   The path segment that follows {@param segment} or null, if there is none.
     * @return The created trie node.
     */
    private fun addEmptyTrie(
        segment: String,
        nextSegment: String?,
    ): PathTrie {
        val trie = PathTrie(segment)
        mapping[segment] = trie
        if (segment == "**") {
            multiWildcardEnd = nextSegment
            if (nextSegment == null) {
                trie.isTerminal = true
            }
        }
        return trie
    }

    /**
     * Adds a new path into the trie.
     *
     * @param path          The path to add.
     * @param authenticated Should the path require authentication (true) or not (false).
     */
    fun addPath(
        path: String,
        authenticated: Boolean,
    ) {
        var segments = path.split("/")
        segments = segments.subList(1, segments.size)
        var trie: PathTrie = this
        for ((index, segment) in segments.withIndex()) {
            val child = trie.getTrie(segment)

            val nextSegment = if (index + 1 >= segments.size) null else segments[index + 1]
            trie = child ?: trie.addEmptyTrie(segment, nextSegment)
        }
        trie.isTerminal = true
        trie.isAuthenticated = authenticated
    }

    /**
     * Traverses the trie based on the path.
     *
     * @param path  The path to traverse.
     * @return The trie node at the end or null, if we did not land on a terminal node.
     */
    fun traverse(path: String): PathTrie? {
        val segments = path.split("/")
        return traverse(segments.subList(1, segments.size))
    }

    /**
     * Traverses the trie based on the path.
     *
     * @param segments The path segments.
     * @return The trie node at the end or null, if we did not land on a terminal node.
     */
    fun traverse(segments: List<String>): PathTrie? {
        if (segments.isEmpty()) {
            return this
        }

        // Check if we have to recurse into ourselves
        // TODO: I think we can just remove segments until we have find the end wildcard or the list is empty.
        val s = segments.first()
        if (mapping[s] == null && segment == "**") {
            val child = this.traverse(segments.subList(1, segments.size))
            if (child != null && child.isTerminal) {
                return child
            }
        }

        // Make sure that we try the wildcards last to first try matching on concrete
        // keys.
        val keys = mapping.keys.toMutableList()
        val hasSingleWildcard = keys.remove("*")
        val hasMultiWildcard = keys.remove("**")
        if (hasSingleWildcard) {
            keys.add("*")
        }
        if (hasMultiWildcard) {
            keys.add("**")
        }

        // Try the mapping.
        for (key in keys) {
            val trie = mapping[key]!!
            if (key == s || key == "*" || key == "**") {
                val child = trie.traverse(segments.subList(1, segments.size))
                if (child != null && child.isTerminal) {
                    return child
                }
            }
        }
        return null
    }
}
