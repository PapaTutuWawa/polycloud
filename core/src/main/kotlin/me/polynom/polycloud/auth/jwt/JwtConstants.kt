package me.polynom.polycloud.auth.jwt

/**
 * Constants for the JWT service.
 */
object JwtConstants {
    /** The issuer inside the JWT. */
    val ISSUER = "polycloud"

    /** The role claim in the JWT. */
    val ROLES = "roles"

    /** The type claim in the JWT. */
    val TYPE = "type";
}