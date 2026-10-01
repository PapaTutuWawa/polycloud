package me.polynom.polycloud.apps.auth.stub

import me.polynom.polycloud.apps.auth.stub.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.auth.stub.dto.AuthResult
import me.polynom.polycloud.plugin.auth.JwtService
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * A simple stub authentication method that only allows a hardcoded credential
 * pair.
 */
@PluginEnabled
@RestController
@RequestMapping("/api/auth/stub")
class StubAuthPlugin(
    /** The JWT service. */
    private val jwtService: JwtService,
) : PolycloudAuthPlugin {
    /** Logger. */
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getData(): AuthPluginData =
        AuthPluginData(
            pluginName = javaClass.name,
            scheme = null,
            displayName = "Stub Login",
            null,
            null,
        )

    override fun verify(token: String): AuthVerificationResult? = null

    override fun register() {
        logger.info("StubAuthPlugin registered")
    }

    @Suppress("ReturnCount")
    @PostMapping("/authenticate")
    fun authenticate(
        @RequestHeader("Authorization") authHeader: String?,
    ): ResponseEntity<AuthResult> {
        if (authHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).build()
        }

        val prefix = "Basic "
        if (!authHeader.startsWith(prefix)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).build()
        }

        // User: user
        // Password: user
        if (authHeader.substring(prefix.length) == "dXNlcjp1c2Vy") {
            val token =
                jwtService.generateAuthToken(
                    "user",
                    listOf("admin"),
                )
            jwtService.saveRefreshToken("user", token.refreshToken)
            return ResponseEntity.ok(
                AuthResult(
                    auth =
                        AuthResult.Token(
                            token.authToken,
                            token.authTokenExpiryIn,
                        ),
                    refresh =
                        AuthResult.Token(
                            token.refreshToken,
                            token.refreshTokenExpiryIn,
                        ),
                ),
            )
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN.value()).build()
    }
}
