package me.polynom.polycloud.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT-related config.
 */
@ConfigurationProperties(prefix = "me.polynom.polycloud.auth.jwt")
data class JwtConfig(
    /** The secret for JWT signatures. */
    val secret: String,
    /** Lifetime of the token in seconds. Defaults to 7 days. */
    val tokenLifetime: Long = 7 * 86400,
    /** Lifetime of the refresh token in seconds. Defaults to 180 days. */
    val refreshLifetime: Long = 15552000,
    /** Minimum number of seconds from the end of the refresh time that you can request. Defaults to 1 day. */
    val minimumRefreshTime: Long = 86400,
)
