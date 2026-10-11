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

        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = "alice")

        assertEquals("/a", manager.lockCovering("/a")?.path)
        assertEquals(lock.token, manager.lockCovering("/a")?.token)
    }

    @Test
    fun test_lockCovering_withZeroDepth_doesNotCoverChildren() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)

        assertNull(manager.lockCovering("/a/b"))
    }

    @Test
    fun test_lockCovering_withInfiniteDepth_coversChildren() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.INFINITY, scope = LockScope.EXCLUSIVE, owner = null)

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
        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)

        assertFalse(manager.isLockedByOther("/a", setOf(lock.token)))
    }

    @Test
    fun test_isLockedByOther_withDifferentToken_isTrue() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)

        assertTrue(manager.isLockedByOther("/a", setOf("urn:uuid:not-the-real-one")))
    }

    @Test
    fun test_unlock_withMatchingTokenAndPath_removesLock() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)

        val result = manager.unlock("/a", lock.token)

        assertTrue(result)
        assertNull(manager.lockCovering("/a"))
    }

    @Test
    fun test_unlock_withWrongPath_fails() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null)

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
        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null, timeout = Duration.ofSeconds(1))

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

        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null, timeout = Duration.ofDays(30))

        val remaining = Duration.between(Instant.now(), lock.expiresAt)
        assertTrue(remaining <= LockManager.MAX_TIMEOUT.plusSeconds(1))
    }

    @Test
    fun test_expiredLock_isNotFoundAnymore() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, scope = LockScope.EXCLUSIVE, owner = null, timeout = Duration.ofSeconds(-1))

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

    @Test
    fun test_canAcquire_exclusiveOnUnlockedPath_isAllowed() {
        val manager = LockManager()

        assertTrue(manager.canAcquire("/a", LockScope.EXCLUSIVE))
    }

    @Test
    fun test_canAcquire_exclusiveBlockedByExistingSharedLock() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, LockScope.SHARED, owner = null)

        assertFalse(manager.canAcquire("/a", LockScope.EXCLUSIVE))
    }

    @Test
    fun test_canAcquire_secondSharedLockAllowedAlongsideFirst() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, LockScope.SHARED, owner = "alice")

        assertTrue(manager.canAcquire("/a", LockScope.SHARED))
    }

    @Test
    fun test_canAcquire_sharedBlockedByExistingExclusiveLock() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, LockScope.EXCLUSIVE, owner = null)

        assertFalse(manager.canAcquire("/a", LockScope.SHARED))
    }

    @Test
    fun test_locksCovering_returnsAllSharedLocksOnThePath() {
        val manager = LockManager()
        val first = manager.lock("/a", LockDepth.ZERO, LockScope.SHARED, owner = "alice")
        val second = manager.lock("/a", LockDepth.ZERO, LockScope.SHARED, owner = "bob")

        assertEquals(setOf(first.token, second.token), manager.locksCovering("/a").map { it.token }.toSet())
    }

    @Test
    fun test_checkIfHeader_noHeaderAndNotLocked_isSatisfied() {
        val manager = LockManager()

        assertEquals(IfCheckResult.SATISFIED, manager.checkIfHeader("/a", null, currentEtag = null))
    }

    @Test
    fun test_checkIfHeader_noHeaderAndLockedByOther_isLocked() {
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, LockScope.EXCLUSIVE, owner = null)

        assertEquals(IfCheckResult.LOCKED, manager.checkIfHeader("/a", null, currentEtag = null))
    }

    @Test
    fun test_checkIfHeader_headerWithCorrectLockToken_isSatisfied() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, LockScope.EXCLUSIVE, owner = null)

        val result = manager.checkIfHeader("/a", "(<${lock.token}>)", currentEtag = null)

        assertEquals(IfCheckResult.SATISFIED, result)
    }

    @Test
    fun test_checkIfHeader_headerWithUnknownLockToken_isPreconditionFailed() {
        val manager = LockManager()

        val result = manager.checkIfHeader("/a", "(<urn:uuid:does-not-exist>)", currentEtag = null)

        assertEquals(IfCheckResult.PRECONDITION_FAILED, result)
    }

    @Test
    fun test_checkIfHeader_headerWithMatchingEntityTag_isSatisfied() {
        // The bracket's quotes are part of the etag's value - matching DavNode.etag's own format.
        val manager = LockManager()

        val result = manager.checkIfHeader("/a", "([\"etag-123\"])", currentEtag = "\"etag-123\"")

        assertEquals(IfCheckResult.SATISFIED, result)
    }

    @Test
    fun test_checkIfHeader_headerWithMismatchedEntityTag_isPreconditionFailed() {
        val manager = LockManager()

        val result = manager.checkIfHeader("/a", "([\"wrong-etag\"])", currentEtag = "\"etag-123\"")

        assertEquals(IfCheckResult.PRECONDITION_FAILED, result)
    }

    @Test
    fun test_checkIfHeader_notOfAnUnrelatedToken_isSatisfied() {
        val manager = LockManager()

        val result = manager.checkIfHeader("/a", "(Not <urn:uuid:unrelated>)", currentEtag = null)

        assertEquals(IfCheckResult.SATISFIED, result)
    }

    @Test
    fun test_checkIfHeader_multipleLists_areAndedNotOred() {
        // Confirmed against litmus's own source comments: "(<real-token>) (Not <DAV:no-lock>)"
        // is documented as "real lock-token AND not(bogus lock-token)" - a bogus list anywhere
        // in the header must fail the whole thing, even if another list would've matched alone.
        // Result is LOCKED, not PRECONDITION_FAILED: it's a pure lock-token mismatch (no
        // entity-tag condition involved) against a resource that actually is locked.
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, LockScope.EXCLUSIVE, owner = null)

        val result = manager.checkIfHeader("/a", "(<urn:uuid:bogus>) (<${lock.token}>)", currentEtag = null)

        assertEquals(IfCheckResult.LOCKED, result)
    }

    @Test
    fun test_checkIfHeader_tokenFailureWithEntityTagCondition_isPreconditionFailedNotLocked() {
        // litmus's fail_cond_put: "(<DAV:no-lock> [valid-etag])" on an actually-locked resource
        // still prefers 412 - mixing in an entity-tag condition makes it a generic precondition
        // check rather than a pure lock-token one, even though the resource is genuinely locked.
        val manager = LockManager()
        manager.lock("/a", LockDepth.ZERO, LockScope.EXCLUSIVE, owner = null)

        val result = manager.checkIfHeader("/a", "(<urn:uuid:bogus> [\"etag-123\"])", currentEtag = "\"etag-123\"")

        assertEquals(IfCheckResult.PRECONDITION_FAILED, result)
    }

    @Test
    fun test_checkIfHeader_multipleListsAllMatching_isSatisfied() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, LockScope.EXCLUSIVE, owner = null)

        val result = manager.checkIfHeader("/a", "(<${lock.token}>) (Not <urn:uuid:unrelated>)", currentEtag = null)

        assertEquals(IfCheckResult.SATISFIED, result)
    }

    @Test
    fun test_checkIfHeader_tokenValidButForADifferentPathNotCoveredByIt_isPreconditionFailed() {
        val manager = LockManager()
        val lock = manager.lock("/a", LockDepth.ZERO, LockScope.EXCLUSIVE, owner = null)

        val result = manager.checkIfHeader("/b", "(<${lock.token}>)", currentEtag = null)

        assertEquals(IfCheckResult.PRECONDITION_FAILED, result)
    }
}
