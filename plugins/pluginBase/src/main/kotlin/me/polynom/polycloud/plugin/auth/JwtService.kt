package me.polynom.polycloud.plugin.auth

import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult

/**
 * Interface for a bean that creates JWT tokens.
 */
interface JwtService {
    /**
     * Creates a JWT token for API authentication.
     *
     * @param username  The username to include in the JWT.
     * @param roles     The user roles to include in the JWT.
     * @param extra     Extra information to include in the JWT.
     * @return The signed JWT.
     */
    fun generateToken(username: String, roles: List<String>, extra: Map<String, String>): String

    /**
     * Verifies the signature of a JWT.
     *
     * @param token The JWT to verify.
     * @return The {@link AuthVerificationResult} if the token is valid. Null, if not.
     */
    fun verifyToken(token: String): AuthVerificationResult?
}