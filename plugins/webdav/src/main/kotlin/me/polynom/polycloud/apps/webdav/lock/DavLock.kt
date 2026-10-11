package me.polynom.polycloud.apps.webdav.lock

import java.time.Instant

enum class LockDepth {
    ZERO,
    INFINITY,
}

enum class LockScope {
    EXCLUSIVE,
    SHARED,
}

data class DavLock(
    val token: String,
    val path: String,
    val depth: LockDepth,
    val scope: LockScope,
    val owner: String?,
    val expiresAt: Instant,
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = now.isAfter(expiresAt)

    fun covers(
        otherPath: String,
        isAncestorOf: (ancestor: String, path: String) -> Boolean,
    ): Boolean = otherPath == path || (depth == LockDepth.INFINITY && isAncestorOf(path, otherPath))
}
