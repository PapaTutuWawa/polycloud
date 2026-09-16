package me.polynom.polycloud.plugin.security

/**
 * Data class that presents an authenticated path.
 */
data class AuthenticatedPath(
    /** The path template that is authenticated. */
    val pathTemplate: String,
    /** Flag controlling whether the path should be authenticated or not. */
    val authenticated: Boolean = true,
)
