package me.polynom.polycloud.apps.webdav.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAppPasswordRequest(
    val name: String,
)

@Serializable
data class CreateAppPasswordResponse(
    val id: String,
    val name: String,
    /** The plaintext secret. Shown exactly once - it cannot be retrieved again after this response. */
    val secret: String,
)

@Serializable
data class AppPasswordSummaryDto(
    val id: String,
    val name: String,
    val createdAt: String,
)
