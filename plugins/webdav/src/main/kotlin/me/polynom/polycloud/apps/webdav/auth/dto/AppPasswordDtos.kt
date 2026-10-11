package me.polynom.polycloud.apps.webdav.auth.dto

import java.time.Instant
import java.util.UUID

data class CreateAppPasswordRequest(
    val name: String,
)

data class CreateAppPasswordResponse(
    val id: UUID,
    val name: String,
    /** The plaintext secret. Shown exactly once - it cannot be retrieved again after this response. */
    val secret: String,
)

data class AppPasswordSummaryDto(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
)
