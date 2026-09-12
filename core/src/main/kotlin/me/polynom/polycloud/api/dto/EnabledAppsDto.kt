package me.polynom.polycloud.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO describing all enabled apps.
 */
@Serializable
data class EnabledAppsDto(
    /** List of apps that are enabled. This excludes authentication plugins. */
    val apps: List<EnabledAppDto>,
)
