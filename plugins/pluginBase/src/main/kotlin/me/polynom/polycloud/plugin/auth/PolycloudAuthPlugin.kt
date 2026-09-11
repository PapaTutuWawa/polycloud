package me.polynom.polycloud.plugin.auth

import me.polynom.polycloud.plugin.PolycloudPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult

/**
 * Interface that auth plugins must implement.
 */
interface PolycloudAuthPlugin : PolycloudPlugin {
    /**
     * Returns data about the auth plugin.
     *
     * @return The description of the plugin.
     */
    fun getData(): AuthPluginData

    /**
     * Verify that a request is authenticated.
     *
     * @param token The token to verify.
     * @return The result of the verification. Null, if not authenticated.
     */
    fun verify(token: String): AuthVerificationResult?
}