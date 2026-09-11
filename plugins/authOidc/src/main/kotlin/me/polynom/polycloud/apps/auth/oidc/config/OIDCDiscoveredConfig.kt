package me.polynom.polycloud.apps.auth.oidc.config

/**
 * Configuration discovered using OAuth's .well-known directory.
 */
data class OIDCDiscoveredConfig(
    /** URL for authentication. */
    val authorize: String,
    /** URL for JWKS. */
    val jwks: String,
    /** The issuer. */
    val issuer: String,
)
