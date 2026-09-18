package me.polynom.polycloud.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Entity for tracking valid refresh tokens in the database.
 */
@Entity
@Table(name = "refresh_token")
class RefreshToken(
    /** The hash of the refresh token. */
    @Id
    @Column(name = "token_hash", nullable = false)
    var tokenHash: String? = null,

    /** The username this token belongs to. */
    @Column(name = "username", nullable = false)
    var username: String? = null,
)