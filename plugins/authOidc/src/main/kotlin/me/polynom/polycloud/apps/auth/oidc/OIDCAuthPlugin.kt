package me.polynom.polycloud.apps.auth.oidc

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import kotlinx.serialization.json.Json
import me.polynom.polycloud.apps.auth.oidc.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.auth.oidc.config.OIDCConfig
import me.polynom.polycloud.apps.auth.oidc.config.OIDCDiscoveredConfig
import me.polynom.polycloud.apps.auth.oidc.jwt.RSAKeyProvider
import me.polynom.polycloud.apps.auth.oidc.rest.OIDCDiscoveryResponse
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest

/**
 * Auth plugin that allows login via OIDC.
 */
@Component
@PluginEnabled
class OIDCAuthPlugin(
    /** Plugin configuration. */
    val config: OIDCConfig,
) : PolycloudAuthPlugin {
    /** Logging. */
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /** OIDC config. */
    lateinit var oidcConfig: OIDCDiscoveredConfig

    /** The provider handling JWKS fetching. */
    lateinit var jwksProvider: JwkProvider

    /** Glue between java-jwt and jwt-rsa. */
    lateinit var jwkRsaProvider: RSAKeyProvider

    override fun getData(): AuthPluginData {
        val redirectUrl = UriComponentsBuilder
            .fromUri(URI.create(oidcConfig.authorize))
            .queryParam("client_id", config.clientId)
            .queryParam("response_type", "code")
            .queryParam("scope", listOf("openid", "profile", "refresh_token").joinToString("%20"))
            .build()
            .toUriString()
        return AuthPluginData(
            "oidc",
            "Bearer",
            config.displayName,
            config.displayIcon,
            mapOf(
                "urlBase" to redirectUrl,
            ),
        )
    }

    override fun verify(token: String): AuthVerificationResult? {
        val t = token.substring(7)
        val algo = com.auth0.jwt.algorithms.Algorithm.RSA256(jwkRsaProvider)
        val verifier = JWT
            .require(algo)
            .withIssuer(oidcConfig.issuer)
            .build()
        try {
            val decoded = verifier.verify(t)
            return AuthVerificationResult(
                decoded.getClaim(config.usernameClaim).asString(),
                decoded.getClaim(config.rolesClaim).asList(String::class.java),
            )
        } catch (e: JWTVerificationException) {
            logger.warn("JWTVerificationException", e)
            return null
        }
    }

    override fun register() {
        // Discover OIDC data
        val client = HttpClient.newHttpClient()
        val request = HttpRequest
            .newBuilder()
            .GET()
            .uri(URI.create(config.discoveryUrl))
            .build()
        val responseRaw = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        if (responseRaw.statusCode() != 200) {
            throw Exception("Failed to discover OIDC metadata")
        }

        val response = Json.decodeFromString<OIDCDiscoveryResponse>(responseRaw.body())
        oidcConfig = OIDCDiscoveredConfig(
            authorize = response.authorizationEndpoint,
            jwks = response.jwksUri,
            issuer = response.issuer,
        )
        logger.info("Discovered OIDC values: [{}] [{}] [{}]", oidcConfig.authorize, oidcConfig.jwks, oidcConfig.issuer)

        // Discover JWKS
        jwksProvider = JwkProviderBuilder(URL(oidcConfig.jwks))
            .cached(true)
            .build()
        jwkRsaProvider = RSAKeyProvider(jwksProvider)
    }
}