package me.polynom.polycloud.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT-related config.
 */
@ConfigurationProperties(prefix = "me.polynom.polycloud.auth.jwt")
data class JwtConfig(
    /** The secret for JWT signatures. */
    val secret: String,
    /** Lifetime of the token in seconds. */
    val tokenLifetime: Long = 7*86400, // 7 day.
    /** Lifetime of the refresh token in seconds. */
    val refreshLifetime: Long = 15552000, // 180 day.
    /** Minimum number of seconds from the end of the refresh time that you can request. */
    val minimumRefreshTime: Long = 86400, // 1 day
)
