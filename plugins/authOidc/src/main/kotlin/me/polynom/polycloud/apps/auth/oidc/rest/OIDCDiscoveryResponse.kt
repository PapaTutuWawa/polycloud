package me.polynom.polycloud.apps.auth.oidc.rest

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * Response returned by the OIDC IDP upon discovery.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class OIDCDiscoveryResponse(
    /** The endpoint to send auth requests to. */
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    /** The endpoint holding the JWKS keypair. */
    @SerialName("jwks_uri")
    val jwksUri: String,
    /** The issuer of the IDP. */
    val issuer: String,
)
