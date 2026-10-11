package me.polynom.polycloud.apps.webdav.lock

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LockManagerTest {
    @Test
    fun test_lock_returnsATokenCoveringThePath() {
        val manager = LockManager()

        val lock = manager.lock("/a", LockDepth.ZERO, owner = "alice")

        assertEquals("/a", manager.lockCovering("/a")?.path)
        assertEquals(lock.token, manager.lockCovering("/a")?.token)
    }

    @Test
    fun test_lockCovering_withZeroDepth_doesNotCoverChildren() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, owner = null)

        assertNull(manager.lockCovering("/a/b"))
    }

    @Test
    fun test_lockCovering_withInfiniteDepth_coversChildren() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.INFINITY, owner = null)

        assertEquals(lock.token, manager.lockCovering("/a/b/c")?.token)
    }

    @Test
    fun test_isLockedByOther_withoutLock_isFalse() {
        val manager = LockManager()

        assertFalse(manager.isLockedByOther("/a", emptySet()))
    }

    @Test
    fun test_isLockedByOther_withOwnToken_isFalse() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, owner = null)

        assertFalse(manager.isLockedByOther("/a", setOf(lock.token)))
    }

    @Test
    fun test_isLockedByOther_withDifferentToken_isTrue() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, owner = null)

        assertTrue(manager.isLockedByOther("/a", setOf("urn:uuid:not-the-real-one")))
    }

    @Test
    fun test_unlock_withMatchingTokenAndPath_removesLock() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, owner = null)

        val result = manager.unlock("/a", lock.token)

        assertTrue(result)
        assertNull(manager.lockCovering("/a"))
    }

    @Test
    fun test_unlock_withWrongPath_fails() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, owner = null)

        val result = manager.unlock("/different", lock.token)

        assertFalse(result)
        assertNotNull(manager.lockCovering("/a"))
    }

    @Test
    fun test_unlock_withUnknownToken_fails() {
        val manager = LockManager()

        assertFalse(manager.unlock("/a", "urn:uuid:unknown"))
    }

    @Test
    fun test_refresh_extendsExpiry() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, owner = null, timeout = Duration.ofSeconds(1))

        val refreshed = manager.refresh(lock.token, Duration.ofMinutes(10))

        assertNotNull(refreshed)
        assertTrue(refreshed.expiresAt.isAfter(lock.expiresAt))
        assertEquals(lock.token, refreshed.token)
    }

    @Test
    fun test_refresh_unknownToken_returnsNull() {
        val manager = LockManager()

        assertNull(manager.refresh("urn:uuid:unknown"))
    }

    @Test
    fun test_lock_timeoutIsCappedAtMaxTimeout() {
        val manager = LockManager()

        val lock = manager.lock("/a", LockDepth.ZERO, owner = null, timeout = Duration.ofDays(30))

        val remaining = Duration.between(Instant.now(), lock.expiresAt)
        assertTrue(remaining <= LockManager.MAX_TIMEOUT.plusSeconds(1))
    }

    @Test
    fun test_expiredLock_isNotFoundAnymore() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, owner = null, timeout = Duration.ofSeconds(-1))

        assertNull(manager.lockCovering("/a"))
        assertFalse(manager.isLockedByOther("/a", emptySet()))
        assertNull(manager.byToken(lock.token))
    }

    @Test
    fun test_parseIfHeaderTokens_extractsUuidUrnTokens() {
        val tokens = LockManager.parseIfHeaderTokens("(<urn:uuid:abc>) (<urn:uuid:def>)")

        assertEquals(setOf("urn:uuid:abc", "urn:uuid:def"), tokens)
    }

    @Test
    fun test_parseIfHeaderTokens_nullHeader_isEmpty() {
        assertEquals(emptySet(), LockManager.parseIfHeaderTokens(null))
    }

    @Test
    fun test_parseIfHeaderTokens_ignoresNonUuidUrnContent() {
        val tokens = LockManager.parseIfHeaderTokens("([\"some-etag\"])")

        assertEquals(emptySet(), tokens)
    }

    @Test
    fun test_parseLockTokenHeader_extractsToken() {
        assertEquals("urn:uuid:abc", LockManager.parseLockTokenHeader("<urn:uuid:abc>"))
    }

    @Test
    fun test_parseLockTokenHeader_nullHeader_isNull() {
        assertNull(LockManager.parseLockTokenHeader(null))
    }
}
