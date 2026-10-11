package me.polynom.polycloud.apps.webdav.auth

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppPasswordServiceTest {
    @Test
    fun test_create_returnsAUsableSecret() {
        val service = AppPasswordService(FakeAppPasswordRepository())

        val (appPassword, secret) = service.create("alice", "laptop", listOf("user"))

        assertEquals("alice", appPassword.username)
        assertEquals("laptop", appPassword.name)
        assertTrue(secret.isNotBlank())
    }

    @Test
    fun test_create_generatesDifferentSecretsEachTime() {
        val service = AppPasswordService(FakeAppPasswordRepository())

        val (_, secretOne) = service.create("alice", "laptop", emptyList())
        val (_, secretTwo) = service.create("alice", "phone", emptyList())

        assertNotEquals(secretOne, secretTwo)
    }

    @Test
    fun test_verify_withCorrectSecret_returnsMetadata() {
        val service = AppPasswordService(FakeAppPasswordRepository())
        val (appPassword, secret) = service.create("alice", "laptop", listOf("user", "admin"))

        val result = service.verify(secret)

        assertNotNull(result)
        assertEquals(appPassword.id, result.id)
        assertEquals(listOf("user", "admin"), result.roles)
    }

    @Test
    fun test_verify_withWrongSecret_returnsNull() {
        val service = AppPasswordService(FakeAppPasswordRepository())
        service.create("alice", "laptop", emptyList())

        assertNull(service.verify("not-the-real-secret"))
    }

    @Test
    fun test_list_onlyReturnsPasswordsForThatUser() {
        val service = AppPasswordService(FakeAppPasswordRepository())
        service.create("alice", "laptop", emptyList())
        service.create("bob", "desktop", emptyList())

        val aliceList = service.list("alice")

        assertEquals(1, aliceList.size)
        assertEquals("laptop", aliceList.first().name)
    }

    @Test
    fun test_revoke_removesPasswordAndItNoLongerVerifies() {
        val service = AppPasswordService(FakeAppPasswordRepository())
        val (appPassword, secret) = service.create("alice", "laptop", emptyList())

        val revoked = service.revoke("alice", appPassword.id)

        assertTrue(revoked)
        assertNull(service.verify(secret))
    }

    @Test
    fun test_revoke_byAnotherUser_fails() {
        val service = AppPasswordService(FakeAppPasswordRepository())
        val (appPassword, secret) = service.create("alice", "laptop", emptyList())

        val revoked = service.revoke("bob", appPassword.id)

        assertFalse(revoked)
        assertNotNull(service.verify(secret))
    }

    @Test
    fun test_revoke_unknownId_fails() {
        val service = AppPasswordService(FakeAppPasswordRepository())

        assertFalse(service.revoke("alice", java.util.UUID.randomUUID()))
    }
}
