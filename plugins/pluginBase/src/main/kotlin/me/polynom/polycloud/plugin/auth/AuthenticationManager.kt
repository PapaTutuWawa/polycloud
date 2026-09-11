package me.polynom.polycloud.plugin.auth

import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult

/**
 * Interface of the bean that provides the authentication manager.
 */
interface AuthenticationManager {
    /**
     * Authenticates the user using the provided token by delegating to
     * the handling auth plugin.
     *
     * @param token The provided auth token.
     * @return A populated {@link AuthVerificationResult} if the request is authenticated. Null, if not.
     */
    fun authenticate(token: String): AuthVerificationResult?
}