package me.polynom.polycloud.apps.webdav.auth

import me.polynom.polycloud.apps.webdav.auth.dto.CreateAppPasswordRequest
import me.polynom.polycloud.plugin.auth.UserContext
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppPasswordControllerTest {
    private class FixedUserContext(
        private val result: AuthVerificationResult,
    ) : UserContext {
        override fun getUser(): AuthVerificationResult = result
    }

    private fun newController(
        username: String = "alice",
        roles: List<String> = listOf("user"),
    ): Pair<AppPasswordController, AppPasswordService> {
        val service = AppPasswordService(FakeAppPasswordRepository())
        val controller = AppPasswordController(service, FixedUserContext(AuthVerificationResult(username, roles)))
        return controller to service
    }

    @Test
    fun test_create_returnsSecretAndStoresItForTheCurrentUser() {
        val (controller, service) = newController(username = "alice")

        val response = controller.create(CreateAppPasswordRequest("laptop"))

        assertTrue(response.secret.isNotBlank())
        assertEquals("laptop", response.name)
        assertEquals(1, service.list("alice").size)
    }

    @Test
    fun test_list_onlyReturnsCurrentUsersPasswords_andNeverTheSecret() {
        val (controller, service) = newController(username = "alice")
        service.create("alice", "laptop", emptyList())
        service.create("bob", "desktop", emptyList())

        val list = controller.list()

        assertEquals(listOf("laptop"), list.map { it.name })
    }

    @Test
    fun test_revoke_ownPassword_returns204() {
        val (controller, service) = newController(username = "alice")
        val (appPassword, _) = service.create("alice", "laptop", emptyList())

        val response = controller.revoke(appPassword.id)

        assertEquals(HttpStatus.NO_CONTENT.value(), response.statusCode.value())
    }

    @Test
    fun test_revoke_unknownId_returns404() {
        val (controller) = newController(username = "alice")

        val response = controller.revoke(UUID.randomUUID())

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode.value())
    }

    @Test
    fun test_revoke_anotherUsersPassword_returns404AndKeepsIt() {
        val (aliceController, service) = newController(username = "alice")
        val (bobPassword, bobSecret) = service.create("bob", "desktop", emptyList())

        val response = aliceController.revoke(bobPassword.id)

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode.value())
        assertEquals(bobPassword.id, service.verify(bobSecret)?.id)
    }
}
