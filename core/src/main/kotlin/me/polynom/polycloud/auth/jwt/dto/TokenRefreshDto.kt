package me.polynom.polycloud.auth.jwt.dto

/**
 * DTO describing the response to a token refresh.
 */
data class TokenRefreshDto(
    /** The new authentication token. */
    val auth: Token,
    /** The new refresh token. */
    val refresh: Token,
) {
    data class Token(
        /** The token value. */
        val token: String,
        /** How long the token lives. */
        val expiresIn: Long,
        /** The type of token. */
        val tokenType: String = "Bearer",
    )
}
