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
    val tokenLifetime: Long = 86400, // 1 day.
)
