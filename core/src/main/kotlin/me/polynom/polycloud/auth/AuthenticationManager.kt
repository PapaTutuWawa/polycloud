package me.polynom.polycloud.auth

import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Bean dealing with authentication checks.
 */
@Service
class AuthenticationManager(
    /** List of available authentication plugins. */
    private val authPlugins: List<PolycloudAuthPlugin>,
) {
    /** Logger. */
    private val logger = LoggerFactory.getLogger(javaClass)

    fun authenticate(token: String): AuthVerificationResult? {
        val parts = token.split(" ")
        if (parts.size != 2) {
            logger.warn("Token is of wrong format. Expected 2 parts, got [{}]", parts.size)
            return null
        }

        val plugin = authPlugins.find { it.getData().scheme == parts[0] }
        if (plugin == null) {
            return null
        }

        logger.debug("Chose [{}] as the authenticating plugin", plugin.javaClass.name)
        return plugin.verify(token)
    }
}
