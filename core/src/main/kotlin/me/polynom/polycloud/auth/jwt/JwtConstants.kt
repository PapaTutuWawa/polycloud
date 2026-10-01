package me.polynom.polycloud.auth.jwt

/**
 * Constants for the JWT service.
 */
object JwtConstants {
    /** The issuer inside the JWT. */
    const val ISSUER = "polycloud"

    /** The role claim in the JWT. */
    const val ROLES = "roles"

    /** The type claim in the JWT. */
    const val TYPE = "type"
}
