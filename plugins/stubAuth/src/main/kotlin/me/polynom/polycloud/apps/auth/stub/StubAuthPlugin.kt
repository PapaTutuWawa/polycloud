package me.polynom.polycloud.apps.auth.stub

import me.polynom.polycloud.apps.auth.stub.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * A simple stub authentication method that only allows a hardcoded credential
 * pair.
 */
@PluginEnabled
@Service
class StubAuthPlugin : PolycloudAuthPlugin {
    /** Logger. */
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getData(): AuthPluginData {
        return AuthPluginData(
            pluginName = javaClass.name,
            scheme = "Stub",
            displayName = "Stub Login",
            null,
            null,
        )
    }

    override fun verify(token: String): AuthVerificationResult? {
        if (token == "Stub example") {
            return AuthVerificationResult(
                username = "admin",
                roles = listOf("admin", "user"),
            )
        }

        return null
    }

    override fun register() {
        logger.info("StubAuthPlugin registered")
    }
}