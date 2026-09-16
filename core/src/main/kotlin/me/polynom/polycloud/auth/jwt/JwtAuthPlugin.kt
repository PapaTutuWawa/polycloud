package me.polynom.polycloud.auth.jwt

import me.polynom.polycloud.plugin.auth.JwtService
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Authentication plugin for JWT bearer tokens.
 */
@Service
class JwtAuthPlugin(
    /** The JWT service. */
    private val jwtService: JwtService,
) : PolycloudAuthPlugin {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun getData(): AuthPluginData =
        AuthPluginData(
            pluginName = "jwt",
            scheme = "Bearer",
            displayName = "JWT",
            iconUrl = null,
            data = null,
        )

    override fun verify(token: String): AuthVerificationResult? {
        // Trim off the "Bearer "
        val trimmedToken = token.substring(7)
        return jwtService.verifyToken(trimmedToken)
    }

    override fun register() {}
}
