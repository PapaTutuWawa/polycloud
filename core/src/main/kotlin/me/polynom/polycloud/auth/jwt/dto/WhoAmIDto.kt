package me.polynom.polycloud.auth.jwt.dto

/**
 * API DTO containing information about the current user.
 */
data class WhoAmIDto(
    /** The username. */
    val username: String,
    /** The roles the user has. */
    val roles: List<String>,
)
