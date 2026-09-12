package me.polynom.polycloud.auth

import me.polynom.polycloud.plugin.security.PathAuthenticationConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
