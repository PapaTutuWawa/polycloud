package me.polynom.polycloud.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO describing all available auth mechanisms.
 */
@Serializable
data class AuthMechanismsDto(
    /** The list of available mechanisms. */
    val mechanisms: List<AuthMechanismDto>,
)
