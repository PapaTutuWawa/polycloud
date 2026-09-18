package me.polynom.polycloud.plugin.auth.dto

/**
 * DTO object for auth token generation.
 */
data class AuthToken(
    /** The auth token value. */
    val authToken: String,
    /** The time the auth token lives. */
    val authTokenExpiryIn: Long,
    /** The refresh token value. */
    val refreshToken: String,
    /** The time the refresh token lives. */
    val refreshTokenExpiryIn: Long,
)