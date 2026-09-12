package me.polynom.polycloud.plugin.auth

import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult

/**
 * Interface of the bean that provides the authentication manager.
 */
interface UserContext {
    /**
     * Returns the user that did the current request.
     *
     * @return A populated {@link AuthVerificationResult} if the request is authenticated. Null, if not.
     */
    fun getUser(): AuthVerificationResult?
}