package me.polynom.polycloud.apps.webdav.auth

import me.polynom.polycloud.apps.webdav.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.webdav.persistence.entities.AppPasswordEntity
import me.polynom.polycloud.apps.webdav.persistence.repository.AppPasswordRepository
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

private const val SECRET_BYTES = 32

/**
 * Registry of app passwords, persisted in the `webdav_app_password` table. Secrets are never
 * stored in plaintext, only their SHA-256 hash, keyed for O(1) lookup on the auth hot path.
 */
@PluginEnabled
@Component
class AppPasswordService(
    private val repository: AppPasswordRepository,
) {
    private val secureRandom = SecureRandom()

    /** Creates a new app password for [username]. @return the metadata and the plaintext secret (shown only now). */
    fun create(
        username: String,
        name: String,
        roles: List<String>,
    ): Pair<AppPassword, String> {
        val secret = generateSecret()
        val appPassword = AppPassword(UUID.randomUUID(), username, name, roles, Instant.now())
        repository.save(appPassword.toEntity(hash(secret)))
        return appPassword to secret
    }

    fun verify(secret: String): AppPassword? = repository.findById(hash(secret)).map { it.toDomain() }.orElse(null)

    fun list(username: String): List<AppPassword> = repository.findAllByUsername(username).map { it.toDomain() }

    fun revoke(
        username: String,
        id: UUID,
    ): Boolean {
        val entity = repository.findByAppPasswordIdAndUsername(id, username) ?: return false
        repository.delete(entity)
        return true
    }

    private fun generateSecret(): String {
        val bytes = ByteArray(SECRET_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

private fun AppPasswordEntity.toDomain(): AppPassword =
    AppPassword(
        id = requireNotNull(appPasswordId),
        username = requireNotNull(username),
        name = requireNotNull(name),
        roles = roles?.takeIf { it.isNotEmpty() }?.split(",") ?: emptyList(),
        createdAt = requireNotNull(createdAt),
    )

private fun AppPassword.toEntity(secretHash: String): AppPasswordEntity =
    AppPasswordEntity(
        secretHash = secretHash,
        appPasswordId = id,
        username = username,
        name = name,
        roles = roles.joinToString(","),
        createdAt = createdAt,
    )
