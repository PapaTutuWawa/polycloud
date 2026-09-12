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
class PathTrie {
    /** The mapping of path segments to the next node in the trie. */
    private val mapping: MutableMap<String, PathTrie> = mutableMapOf()

    /** If this node has a "*" node, then this is a reference to the node "*" points to. */
    private var singleSegmentWildcard: PathTrie? = null

    /**
     * If this node has a "**" node, then this is a reference to the node "**" points to together with the path segment
     * that follows it.
     */
    private var multiSegmentWildcard: Pair<PathTrie, String?>? = null

    /** Indicator if this node is the terminal for a path. */
    private var isTerminal: Boolean = false

    /** Getter for the single segment wildcard. */
    fun getSingleSegmentWildcard(): PathTrie? = singleSegmentWildcard

    /** Getter for the multi segment wildcard. */
    fun getMultiSegmentWildcard(): Pair<PathTrie, String?>? = multiSegmentWildcard

    /** Setter for the isTerminal flag. */
    fun setIsTerminal(value: Boolean) {
        isTerminal = value
    }

    /** Getter for the isTerminal flag. */
    fun getIsTerminal(): Boolean = isTerminal

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
        println("$textIndent*: [[$singleSegmentWildcard]]")
        println("$textIndent**: [[$multiSegmentWildcard]]")
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
        val trie = PathTrie()
        mapping[segment] = trie
        if (segment == "*") {
            singleSegmentWildcard = trie
        } else if (segment == "**") {
            multiSegmentWildcard = Pair(trie, nextSegment)
            if (nextSegment == null) {
                trie.isTerminal = true
            }
        }
        return trie
    }

    /**
     * Adds a new path into the trie.
     *
     * @param path  The path to add.
     */
    fun addPath(path: String) {
        val segments = path.split("/")
        var trie: PathTrie = this
        for ((index, segment) in segments.withIndex()) {
            val child = trie.getTrie(segment)

            val nextSegment = if (index + 1 >= segments.size) null else segments[index + 1]
            trie = child ?: trie.addEmptyTrie(segment, nextSegment)
        }
        trie.isTerminal = true
    }

    /**
     * Traverses the trie based on the path.
     *
     * @param path  The path to traverse.
     * @return The trie node at the end or null, if we did not land on a terminal node.
     */
    fun traverse(path: String): PathTrie? {
        val segments = path.split("/")
        var trie = this
        var wildcardUse = 0
        for ((index, segment) in segments.withIndex()) {
            val isEnd = index == segments.size - 1
            if (trie.singleSegmentWildcard != null && trie.getTrie(segment) == null) {
                trie = trie.singleSegmentWildcard!!
                continue
            } else if (trie.multiSegmentWildcard != null && trie.getTrie(segment) == null) {
                if (wildcardUse > 0) {
                    println("$wildcardUse;$segment;$isEnd;${trie.multiSegmentWildcard!!.second}")
                    if (segment == trie.multiSegmentWildcard!!.second) {
                        trie = trie.multiSegmentWildcard!!.first
                        wildcardUse = 0
                    } else if (trie.multiSegmentWildcard!!.second == null && isEnd) {
                        trie = trie.multiSegmentWildcard!!.first
                        break
                    } else {
                        wildcardUse += 1
                        continue
                    }
                } else {
                    wildcardUse += 1
                    if (isEnd) {
                        trie = trie.multiSegmentWildcard!!.first
                    }
                    continue
                }
            }

            val child = trie.getTrie(segment) ?: return null
            trie = child
        }

        if (!trie.isTerminal) {
            return null
        }
        return trie
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
    private val pathTrie: PathTrie = PathTrie()

    init {
        var addedRoutes = 0
        configs.forEach { config ->
            config.paths.forEach {
                pathTrie.addPath(it.pathTemplate)
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
    fun isPathAuthenticated(path: String): Boolean = pathTrie.traverse(path) != null
}
