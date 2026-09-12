package me.polynom.polycloud.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO describing a single auth mechanisms.
 */
@Serializable
data class AuthMechanismDto(
    /** The ID of the mechanism. */
    val id: String,
    /** The display name of the mechanism. */
    val displayName: String,
    /** Data provided by the plugin. */
    val data: Map<String, String>?,
)
