package me.polynom.polycloud.apps.webdav.auth

import me.polynom.polycloud.apps.webdav.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Base64

private const val PREFIX = "Basic "

/**
 * Bridges HTTP Basic auth (what every native WebDAV client sends) to an app password created via
 * [AppPasswordController]. The username in the Basic payload must match the app password's owner;
 * the password half is the app password's secret, never the user's real login credential.
 */
@PluginEnabled
@Component
class WebDavBasicAuthPlugin(
    private val appPasswordService: AppPasswordService,
) : PolycloudAuthPlugin {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getData(): AuthPluginData =
        AuthPluginData(
            pluginName = "webdav-app-password",
            scheme = "Basic",
            displayName = "WebDAV App Password",
            iconUrl = null,
            data = null,
        )

    override fun register() {
        logger.info("WebDAV app-password Basic auth bridge registered")
    }

    override fun verify(token: String): AuthVerificationResult? {
        if (!token.startsWith(PREFIX)) {
            return null
        }

        val credentials = decodeCredentials(token.removePrefix(PREFIX))
        val appPassword =
            credentials?.let { (username, secret) -> appPasswordService.verify(secret)?.takeIf { it.username == username } }

        return appPassword?.let { AuthVerificationResult(it.username, it.roles) }
    }

    private fun decodeCredentials(encoded: String): Pair<String, String>? {
        val decoded = runCatching { Base64.getDecoder().decode(encoded).decodeToString() }.getOrNull() ?: return null
        val separatorIndex = decoded.indexOf(':')

        return if (separatorIndex < 0) null else decoded.substring(0, separatorIndex) to decoded.substring(separatorIndex + 1)
    }
}
