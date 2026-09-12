package me.polynom.polycloud.auth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Data class that presents an authenticated path.
 */
data class AuthenticatedPath(
    /** The path template that is authenticated. */
    val pathTemplate: String,
    /** Flag controlling whether the path should be authenticated or not. */
    val authenticated: Boolean = true,
)

/**
 * Collection of paths that are to be authenticated.
 */
data class PathAuthenticationConfig(
    /** List of path configurations. */
    val paths: List<AuthenticatedPath>,
)

/**
 * A trie that that works on "/" separated paths.
 */
open class PathTrie(
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

/**
 * Bean that holds the logic to evaluate if paths should be authenticated.
 */
@Service
class RouteAuthenticationEvaluator(
    /** The path configurations. */
    configs: List<PathAuthenticationConfig>,
) {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /** The trie that holds all authenticated paths. */
    private val pathTrie: PathTrie = PathTrie("root")

    init {
        var addedRoutes = 0
        configs.forEach { config ->
            config.paths.forEach {
                pathTrie.addPath(it.pathTemplate, it.authenticated)
                addedRoutes++
            }
        }

        logger.info("Added [{}] authenticated routes", addedRoutes)
    }

    /**
     * Checks if a path is supposed to be authenticated.
     *
     * @param path  The path to check.
     * @return True, if the path is authenticated. False, if not.
     */
    fun isPathAuthenticated(path: String): Boolean = pathTrie.traverse(path)?.getIsAuthenticated() ?: false
}
