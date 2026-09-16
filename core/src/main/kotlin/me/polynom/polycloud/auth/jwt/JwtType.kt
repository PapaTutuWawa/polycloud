package me.polynom.polycloud.auth.jwt

/**
 * The type a JWT can have.
 */
enum class JwtType(
    /** The backing string. */
    val value: String,
) {
    /** The JWT can be used for authentication. */
    AUTH("auth"),

    /** The JWT can be used for refresh. */
    REFRESH("refresh");

    companion object {
        /**
         * Converts a string to a {@link JwtType}. Throws an IllegalArgumentException on invalid strings.
         *
         * @param value The value to convert.
         * @return The {@link JwtType}.
         */
        fun fromString(value: String): JwtType {
            return when (value) {
                "auth" -> AUTH
                "refresh" -> REFRESH
                else -> throw IllegalArgumentException("$value is not a valid jwtType")
            }
        }
    }
}