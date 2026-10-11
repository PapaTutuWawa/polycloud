package me.polynom.polycloud.apps.webdav.lock

import me.polynom.polycloud.apps.webdav.store.DavPath
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory exclusive-write WebDAV lock manager (RFC 4918 section 7). Shared locks are not
 * supported. Locks are not persisted and do not survive a restart.
 */
@Component
class LockManager {
    private val locksByToken = ConcurrentHashMap<String, DavLock>()

    companion object {
        val DEFAULT_TIMEOUT: Duration = Duration.ofMinutes(5)
        val MAX_TIMEOUT: Duration = Duration.ofHours(1)
        private val TOKEN_PATTERN = Regex("<([^>]+)>")

        /**
         * Extracts lock tokens from an `If` header, e.g. `(<urn:uuid:abc>)` or
         * `(<urn:uuid:abc>) (<urn:uuid:def>)`. Negated conditions (`Not <...>`) and ETag
         * conditions are not evaluated; only token presence is used.
         */
        fun parseIfHeaderTokens(ifHeader: String?): Set<String> {
            if (ifHeader == null) {
                return emptySet()
            }

            return TOKEN_PATTERN
                .findAll(ifHeader)
                .map { it.groupValues[1] }
                .filter { it.startsWith("urn:uuid:") }
                .toSet()
        }

        /** Extracts the token from a `Lock-Token` header value, e.g. `<urn:uuid:abc>`. */
        fun parseLockTokenHeader(header: String?): String? = header?.let { TOKEN_PATTERN.find(it)?.groupValues?.get(1) }
    }

    fun lock(
        path: String,
        depth: LockDepth,
        owner: String?,
        timeout: Duration = DEFAULT_TIMEOUT,
    ): DavLock {
        val cappedTimeout = if (timeout > MAX_TIMEOUT) MAX_TIMEOUT else timeout
        val lock =
            DavLock(
                token = "urn:uuid:${UUID.randomUUID()}",
                path = path,
                depth = depth,
                owner = owner,
                expiresAt = Instant.now().plus(cappedTimeout),
            )
        locksByToken[lock.token] = lock
        return lock
    }

    fun refresh(
        token: String,
        timeout: Duration = DEFAULT_TIMEOUT,
    ): DavLock? {
        val existing = locksByToken[token]?.takeIf { !it.isExpired() } ?: return null
        val cappedTimeout = if (timeout > MAX_TIMEOUT) MAX_TIMEOUT else timeout
        val refreshed = existing.copy(expiresAt = Instant.now().plus(cappedTimeout))
        locksByToken[refreshed.token] = refreshed
        return refreshed
    }

    fun unlock(
        path: String,
        token: String,
    ): Boolean {
        val matches = locksByToken[token]?.path == path
        if (matches) {
            locksByToken.remove(token)
        }

        return matches
    }

    fun byToken(token: String): DavLock? = locksByToken[token]?.takeIf { !it.isExpired() }

    /** The lock (direct, or inherited from a locked ancestor collection) that covers [path], if any. */
    fun lockCovering(path: String): DavLock? {
        purgeExpired()
        return locksByToken.values.firstOrNull { it.covers(path, DavPath::isAncestorOf) }
    }

    /**
     * True if [path] is locked by a lock whose token is not among [submittedTokens]. Used to
     * decide whether a write operation must be rejected with 423 Locked.
     */
    fun isLockedByOther(
        path: String,
        submittedTokens: Set<String>,
    ): Boolean {
        val lock = lockCovering(path) ?: return false
        return lock.token !in submittedTokens
    }

    private fun purgeExpired() {
        locksByToken.values.removeIf { it.isExpired() }
    }
}
