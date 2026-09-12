package me.polynom.polycloud.api.dto

import kotlinx.serialization.Serializable

/**
 * A single app that is enabled.
 */
@Serializable
data class EnabledAppDto(
    /** Name of the enabled app. */
    val id: String,
)
