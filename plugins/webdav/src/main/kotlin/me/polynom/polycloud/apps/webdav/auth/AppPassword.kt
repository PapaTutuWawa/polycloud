package me.polynom.polycloud.apps.webdav.auth

import java.time.Instant
import java.util.UUID

/**
 * A Nextcloud-style "app password": a credential distinct from the user's normal login,
 * generated for a specific device/client, usable only for Basic auth, and independently
 * revocable. Only its hash is ever kept (see [AppPasswordService]) - the plaintext secret
 * is shown to the user exactly once, at creation time. Held in memory only; lost on restart.
 */
data class AppPassword(
    val id: UUID,
    val username: String,
    val name: String,
    val roles: List<String>,
    val createdAt: Instant,
)
