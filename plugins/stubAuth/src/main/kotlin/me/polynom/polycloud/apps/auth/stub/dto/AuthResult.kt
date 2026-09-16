package me.polynom.polycloud.apps.auth.stub.dto

/**
 * API response when the authentication was successful.
 */
data class AuthResult(
    /** The authentication token. */
    val auth: Token,
    /** The refresh token. */
    val refresh: Token,
) {
    data class Token(
        /** The token value. */
        val token: String,
        /** The time in seconds until the token expires. */
        val expiry: Long,
        /** Type of the token. */
        val type: String = "Bearer",
    )
}
