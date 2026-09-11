package me.polynom.polycloud.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO for the health endpoint.
 */
@Serializable
data class HealthDto(
    /** The status message. */
    val status: String,
)
