package me.polynom.polycloud.apps.auth.oidc.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties("me.polynom.polycloud.auth.oidc")
data class OIDCConfig @ConstructorBinding constructor(
    /** The URL to discover the OIDC configuration. */
    val discoveryUrl: String,
    /** The client ID. */
    val clientId: String,
    /** The client secret. */
    val clientSecret: String,
    /** The display name of the login method. */
    val displayName: String,
    /** The display icon of the login method. */
    val displayIcon: String?,
    /** Username claim. */
    val usernameClaim: String = "prefered_username",
    /** Roles claim. */
    val rolesClaim: String = "groups",
    /** OIDC scopes to request. */
    val scopes: List<String> = listOf("openid", "profile", "refresh_token"),
)