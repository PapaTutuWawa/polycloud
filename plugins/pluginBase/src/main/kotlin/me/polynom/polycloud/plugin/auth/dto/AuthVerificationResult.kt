package me.polynom.polycloud.plugin.auth.dto

/**
 * DTO representing an authentication result.
 */
data class AuthVerificationResult(
    /** The username of the requesting user. */
    val username: String,
    /** The roles of the requesting user. */
    val roles: List<String>,
)
