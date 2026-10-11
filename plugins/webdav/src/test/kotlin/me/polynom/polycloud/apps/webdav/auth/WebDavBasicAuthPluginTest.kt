package me.polynom.polycloud.apps.webdav.auth

import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WebDavBasicAuthPluginTest {
    private fun basicHeader(
        username: String,
        password: String,
    ): String = "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    @Test
    fun test_getData_registersBasicScheme() {
        val plugin = WebDavBasicAuthPlugin(AppPasswordService(FakeAppPasswordRepository()))

        assertEquals("Basic", plugin.getData().scheme)
    }

    @Test
    fun test_verify_withMatchingUsernameAndSecret_succeeds() {
        val appPasswordService = AppPasswordService(FakeAppPasswordRepository())
        val (_, secret) = appPasswordService.create("alice", "laptop", listOf("user"))
        val plugin = WebDavBasicAuthPlugin(appPasswordService)

        val result = plugin.verify(basicHeader("alice", secret))

        assertNotNull(result)
        assertEquals("alice", result.username)
        assertEquals(listOf("user"), result.roles)
    }

    @Test
    fun test_verify_withWrongSecret_fails() {
        val appPasswordService = AppPasswordService(FakeAppPasswordRepository())
        appPasswordService.create("alice", "laptop", listOf("user"))
        val plugin = WebDavBasicAuthPlugin(appPasswordService)

        assertNull(plugin.verify(basicHeader("alice", "wrong-secret")))
    }

    @Test
    fun test_verify_withSecretButWrongUsername_fails() {
        val appPasswordService = AppPasswordService(FakeAppPasswordRepository())
        val (_, secret) = appPasswordService.create("alice", "laptop", listOf("user"))
        val plugin = WebDavBasicAuthPlugin(appPasswordService)

        assertNull(plugin.verify(basicHeader("bob", secret)))
    }

    @Test
    fun test_verify_nonBasicScheme_fails() {
        val plugin = WebDavBasicAuthPlugin(AppPasswordService(FakeAppPasswordRepository()))

        assertNull(plugin.verify("Bearer sometoken"))
    }

    @Test
    fun test_verify_malformedBase64_doesNotThrowAndFails() {
        val plugin = WebDavBasicAuthPlugin(AppPasswordService(FakeAppPasswordRepository()))

        assertNull(plugin.verify("Basic not-valid-base64!!!"))
    }

    @Test
    fun test_verify_base64WithoutColon_fails() {
        val plugin = WebDavBasicAuthPlugin(AppPasswordService(FakeAppPasswordRepository()))
        val encoded = Base64.getEncoder().encodeToString("nocolonhere".toByteArray())

        assertNull(plugin.verify("Basic $encoded"))
    }
}
