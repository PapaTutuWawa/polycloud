package me.polynom.polycloud.apps.webdav.lock

import me.polynom.polycloud.apps.webdav.store.DavPath
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Outcome of [LockManager.checkIfHeader]; the HTTP status mapping lives in the `api` layer. */
enum class IfCheckResult {
    SATISFIED,
    LOCKED,
    PRECONDITION_FAILED,
}

/**
 * In-memory WebDAV lock manager (RFC 4918 section 7), supporting both exclusive and shared write
 * locks. Locks are not persisted and do not survive a restart.
 */
@Component
class LockManager {
    private val locksByToken = ConcurrentHashMap<String, DavLock>()

    companion object {
        val DEFAULT_TIMEOUT: Duration = Duration.ofMinutes(5)
        val MAX_TIMEOUT: Duration = Duration.ofHours(1)
        private val TOKEN_PATTERN = Regex("<([^>]+)>")

        /** Extracts lock tokens from an `If` header, ignoring `Not`/entity-tag conditions. Used only for LOCK refresh detection. */
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
        scope: LockScope,
        owner: String?,
        timeout: Duration = DEFAULT_TIMEOUT,
    ): DavLock {
        val cappedTimeout = if (timeout > MAX_TIMEOUT) MAX_TIMEOUT else timeout
        val lock =
            DavLock(
                token = "urn:uuid:${UUID.randomUUID()}",
                path = path,
                depth = depth,
                scope = scope,
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

    /** Every active lock (direct, or inherited from a locked ancestor collection) covering [path]. */
    fun locksCovering(path: String): List<DavLock> {
        purgeExpired()
        return locksByToken.values.filter { it.covers(path, DavPath::isAncestorOf) }
    }

    /** The lock (direct, or inherited from a locked ancestor collection) that covers [path], if any. */
    fun lockCovering(path: String): DavLock? = locksCovering(path).firstOrNull()

    /** True if a new lock of [scope] may be granted on [path], given whatever already covers it. */
    fun canAcquire(
        path: String,
        scope: LockScope,
    ): Boolean {
        val covering = locksCovering(path)
        return when (scope) {
            LockScope.EXCLUSIVE -> covering.isEmpty()
            LockScope.SHARED -> covering.all { it.scope == LockScope.SHARED }
        }
    }

    /**
     * True if [path] is covered by a lock whose token is not among [submittedTokens]. Used to
     * decide whether a write operation must be rejected with 423 Locked.
     */
    fun isLockedByOther(
        path: String,
        submittedTokens: Set<String>,
    ): Boolean {
        val covering = locksCovering(path)
        return covering.isNotEmpty() && covering.none { it.token in submittedTokens }
    }

    /**
     * Full precondition check for a state-changing request against [path] (RFC 4918 section
     * 10.4): evaluates the `If` header if present (a lock-token condition is satisfied if that
     * lock actually covers [path]; an entity-tag condition is satisfied if it equals
     * [currentEtag]), otherwise falls back to the plain lock check.
     *
     * Every parenthesized list in the header must be satisfied (AND) - confirmed against
     * litmus's own test comments (e.g. "PUT conditional on real lock-token and not(bogus
     * lock-token), should succeed" for `(<real-token>) (Not <DAV:no-lock>)`), since a single
     * space-separated run of untagged lists is one No-tag-list, not several OR'd branches.
     *
     * On failure: [LOCKED] (423) when the only reason is a lock-token mismatch against a
     * resource that's actually locked (no entity-tag condition anywhere muddying it into a
     * generic precondition check); [PRECONDITION_FAILED] (412) otherwise - also confirmed
     * against litmus, which prefers 423 for a corrupted-token-only header but 412 once an
     * entity-tag condition is involved, or once the resource isn't actually locked at all.
     */
    fun checkIfHeader(
        path: String,
        ifHeader: String?,
        currentEtag: String?,
    ): IfCheckResult {
        if (ifHeader.isNullOrBlank()) {
            return if (isLockedByOther(path, emptySet())) IfCheckResult.LOCKED else IfCheckResult.SATISFIED
        }

        val lists = IfHeader.parse(ifHeader)
        val satisfied = lists.all { list -> list.entries.all { entry -> evaluateEntry(entry, path, currentEtag) } }
        val involvesEntityTag = lists.any { list -> list.entries.any { it.condition is IfCondition.EntityTag } }
        val isPureLockTokenFailure = !satisfied && !involvesEntityTag && locksCovering(path).isNotEmpty()

        return when {
            satisfied -> IfCheckResult.SATISFIED
            isPureLockTokenFailure -> IfCheckResult.LOCKED
            else -> IfCheckResult.PRECONDITION_FAILED
        }
    }

    private fun evaluateEntry(
        entry: IfEntry,
        path: String,
        currentEtag: String?,
    ): Boolean {
        val raw =
            when (val condition = entry.condition) {
                is IfCondition.StateToken -> byToken(condition.token)?.covers(path, DavPath::isAncestorOf) == true
                is IfCondition.EntityTag -> condition.etag == currentEtag
            }
        return raw != entry.negated
    }

    private fun purgeExpired() {
        locksByToken.values.removeIf { it.isExpired() }
    }
}
