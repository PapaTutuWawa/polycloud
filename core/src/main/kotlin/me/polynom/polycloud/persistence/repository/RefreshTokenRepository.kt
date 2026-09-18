package me.polynom.polycloud.persistence.repository

import me.polynom.polycloud.persistence.entities.RefreshToken
import org.springframework.data.repository.CrudRepository

/**
 * Repository for the refresh token repository.
 */
interface RefreshTokenRepository : CrudRepository<RefreshToken, String> {
}