package me.polynom.polycloud.plugin.auth

import me.polynom.polycloud.plugin.auth.dto.AuthToken
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
     * @return The signed JWT.
     */
    fun generateAuthToken(username: String, roles: List<String>): AuthToken

    /**
     * Verifies the signature of a JWT.
     *
     * @param token The JWT to verify.
     * @return The {@link AuthVerificationResult} if the token is valid. Null, if not.
     */
    fun verifyAuthToken(token: String): AuthVerificationResult?

    /**
     * Verify the signature of a refresh token.
     *
     * @param token The refresh JWT.
     * @return The {@link AuthVerificationResult} if the token is valid. Null, if not.
     */
    fun verifyRefreshToken(token: String): AuthVerificationResult?

    /**
     * Saves the refresh token into the database.
     *
     * @param token The refresh token.
     */
    fun saveRefreshToken(username: String, token: String)

    /**
     * Checks if the refresh token is in the database like that.
     *
     * @param token The refresh token.
     * @return True, if the token is in the database. False if not.
     */
    fun hasRefreshToken(token: String): Boolean

    /**
     * Replaces the refresh token in the database with a new one.
     *
     * @param old   The old refresh token.
     * @param new   The new refresh token.
     */
    fun replaceRefreshToken(username: String, old: String, new: String)
}