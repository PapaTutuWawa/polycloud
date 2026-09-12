package me.polynom.polycloud.plugin.security

/**
 * Collection of paths that are to be authenticated.
 */
open class PathAuthenticationConfig(
    /** List of path configurations. */
    val paths: List<AuthenticatedPath>,
)
