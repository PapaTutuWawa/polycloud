package me.polynom.polycloud.apps.webdav.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Database model for a WebDAV app password. Keyed by the SHA-256 hash of the secret so the
 * plaintext is never stored; see `me.polynom.polycloud.apps.webdav.auth.AppPasswordService`.
 */
@Entity
@Table(name = "webdav_app_password")
class AppPasswordEntity(
    @Id
    @Column(name = "secret_hash", nullable = false)
    var secretHash: String? = null,
    @Column(name = "id", nullable = false, unique = true)
    var appPasswordId: UUID? = null,
    @Column(name = "username", nullable = false)
    var username: String? = null,
    @Column(name = "name", nullable = false)
    var name: String? = null,
    /** Comma-separated role names. */
    @Column(name = "roles", nullable = false)
    var roles: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant? = null,
)
