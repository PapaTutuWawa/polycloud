package me.polynom.polycloud.auth

import me.polynom.polycloud.plugin.auth.AuthenticationManager
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Bean dealing with authentication checks.
 */
@Service
class AuthenticationManagerImpl(
    /** List of available authentication plugins. */
    private val authPlugins: List<PolycloudAuthPlugin>,
) : AuthenticationManager {
    /** Logger. */
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun authenticate(token: String): AuthVerificationResult? {
        val parts = token.split(" ")
        if (parts.size != 2) {
            logger.warn("Token is of wrong format. Expected 2 parts, got [{}]", parts.size)
            return null
        }

        // Find the plugin that handles this auth scheme.
        val plugin = authPlugins.find { it.getData().scheme == parts[0] } ?: return null
        return plugin.verify(token)
    }
}