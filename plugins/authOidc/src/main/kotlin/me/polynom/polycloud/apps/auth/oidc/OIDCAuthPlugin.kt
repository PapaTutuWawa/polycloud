package me.polynom.polycloud.apps.auth.oidc

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import kotlinx.serialization.json.Json
import me.polynom.polycloud.apps.auth.oidc.api.dto.AuthResult
import me.polynom.polycloud.apps.auth.oidc.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.auth.oidc.config.OIDCConfig
import me.polynom.polycloud.apps.auth.oidc.config.OIDCDiscoveredConfig
import me.polynom.polycloud.apps.auth.oidc.jwt.RSAKeyProvider
import me.polynom.polycloud.apps.auth.oidc.rest.OIDCDiscoveryResponse
import me.polynom.polycloud.plugin.auth.JwtService
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets

/**
 * Auth plugin that allows login via OIDC.
 */
@PluginEnabled
@RestController
@RequestMapping("/api/auth/oidc")
class OIDCAuthPlugin(
    /** Plugin configuration. */
    val config: OIDCConfig,
    /** The JWT service. */
    val jwtService: JwtService,
) : PolycloudAuthPlugin {
    /** Logging. */
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /** OIDC config. */
    lateinit var oidcConfig: OIDCDiscoveredConfig

    /** The provider handling JWKS fetching. */
    lateinit var jwksProvider: JwkProvider

    /** Glue between java-jwt and jwt-rsa. */
    lateinit var jwkRsaProvider: RSAKeyProvider

    override fun getData(): AuthPluginData =
        AuthPluginData(
            "oidc",
            null,
            config.displayName,
            config.displayIcon,
            mapOf(
                "url" to oidcConfig.authorize,
                "client_id" to config.clientId,
                "response_type" to "code",
                "scopes" to config.scopes.joinToString(" "),
            ),
        )

    override fun verify(token: String): AuthVerificationResult? = null

    override fun register() {
        // Discover OIDC data
        val client = HttpClient.newHttpClient()
        val request =
            HttpRequest
                .newBuilder()
                .GET()
                .uri(URI.create(config.discoveryUrl))
                .build()
        val responseRaw =
            client.send(
                request,
                java.net.http.HttpResponse.BodyHandlers
                    .ofString(),
            )
        if (responseRaw.statusCode() != 200) {
            throw Exception("Failed to discover OIDC metadata")
        }

        val response = Json.decodeFromString<OIDCDiscoveryResponse>(responseRaw.body())
        oidcConfig =
            OIDCDiscoveredConfig(
                authorize = response.authorizationEndpoint,
                token = response.tokenEndpoint,
                jwks = response.jwksUri,
                issuer = response.issuer,
            )
        logger.info("Discovered OIDC values: [{}] [{}] [{}]", oidcConfig.authorize, oidcConfig.jwks, oidcConfig.issuer)

        // Discover JWKS
        jwksProvider =
            JwkProviderBuilder(URL(oidcConfig.jwks))
                .cached(true)
                .build()
        jwkRsaProvider = RSAKeyProvider(jwksProvider)
    }

    @PostMapping("/authenticate")
    fun authenticate(
        @RequestHeader("Authorization") authHeader: String?,
    ): ResponseEntity<AuthResult> {
        if (authHeader == null) {
            return ResponseEntity.status(401).build()
        }

        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build()
        }

        val t = authHeader.substring(7)
        val algo =
            com.auth0.jwt.algorithms.Algorithm
                .RSA256(jwkRsaProvider)
        val verifier =
            JWT
                .require(algo)
                .withIssuer(oidcConfig.issuer)
                .build()
        try {
            val decoded = verifier.verify(t)
            val username = decoded.getClaim(config.usernameClaim).asString()
            logger.debug(
                "Generating token with [{}] and [{}]",
                username,
                decoded.getClaim(config.rolesClaim)?.asList(String::class.java)
            )
            val polycloudJwt =
                jwtService.generateAuthToken(
                    username,
                    decoded.getClaim(config.rolesClaim).asList(String::class.java),
                )
            jwtService.saveRefreshToken(username, polycloudJwt.refreshToken)
            return ResponseEntity.ok(
                AuthResult(
                    auth = AuthResult.Token(polycloudJwt.authToken, expiry = polycloudJwt.authTokenExpiryIn),
                    refresh = AuthResult.Token(polycloudJwt.refreshToken, expiry = polycloudJwt.refreshTokenExpiryIn),
                )
            )
        } catch (e: JWTVerificationException) {
            logger.warn("JWTVerificationException", e)
            return ResponseEntity.status(403).build()
        }
    }

    /**
     * Proxy endpoint to do a token exchange. In case the IDP does not set CORS headers.
     */
    @PostMapping("/proxy/token", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun token(@RequestBody payload: MultiValueMap<String, String>): ResponseEntity<String> {
        logger.debug("Got request [{}]", payload)
        val client = HttpClient.newHttpClient()
        val mutablePayload = payload.toMutableMap()
        mutablePayload["client_id"] = listOf(config.clientId)
        mutablePayload["client_secret"] = listOf(config.clientSecret)
        val payload = mutablePayload.map { (key, value) ->
            "$key=${URLEncoder.encode(value[0], StandardCharsets.UTF_8.toString())}"
        }.joinToString("&")
        logger.debug("Sending payload [{}]", payload)

        val request =
            HttpRequest
                .newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(oidcConfig.token))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build()
        val response =
            client.send(
                request,
                java.net.http.HttpResponse.BodyHandlers
                    .ofString(),
            )
        logger.debug("Got response: [{}] [{}]", response.statusCode(), response.headers())
        logger.debug("Body: [{}]", response.body())

        return ResponseEntity
            .status(response.statusCode())
            .headers { headers ->
                response.headers().map().forEach { (name, value) ->
                    headers.put(name, value)
                }
            }
            .body(response.body())
    }
}
