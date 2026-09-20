package me.polynom.polycloud.auth.jwt

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import me.polynom.polycloud.auth.jwt.dto.TokenRefreshDto
import me.polynom.polycloud.auth.jwt.dto.WhoAmIDto
import me.polynom.polycloud.config.JwtConfig
import me.polynom.polycloud.persistence.entities.RefreshToken
import me.polynom.polycloud.persistence.repository.RefreshTokenRepository
import me.polynom.polycloud.plugin.auth.JwtService
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.UserContext
import me.polynom.polycloud.plugin.auth.dto.AuthPluginData
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Authentication plugin for JWT bearer tokens.
 */
@RestController
@RequestMapping("/api/v1/auth/")
class JwtAuthPlugin(
    /** The JWT service. */
    private val jwtService: JwtService,
    /** The JWT config. */
    private val jwtConfig: JwtConfig,
    /** The user context. */
    private val userContext: UserContext,
) : PolycloudAuthPlugin {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun getData(): AuthPluginData =
        AuthPluginData(
            pluginName = "jwt",
            scheme = "Bearer",
            displayName = "JWT",
            iconUrl = null,
            data = null,
        )

    override fun verify(token: String): AuthVerificationResult?
        = jwtService.verifyAuthToken(token.substring(7))

    override fun register() {}

    @GetMapping("/whoami")
    @SecurityRequirement(name = "jwt")
    fun whoami(): WhoAmIDto =
        WhoAmIDto(
            userContext.getUser()!!.username,
            userContext.getUser()!!.roles,
        )

    @PostMapping("/refresh")
    @SecurityRequirement(name = "jwt")
    fun refresh(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<TokenRefreshDto> {
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(403).build()
        }

        // Verify the refresh token (valid JWT + it is in the database).
        val refreshTokenValue = authHeader.substring(7)
        val refreshTokenResult = jwtService.verifyRefreshToken(refreshTokenValue)
        if (refreshTokenResult == null) {
            logger.debug("Refresh token invalid")
            return ResponseEntity.status(403).build()
        }
        if (!jwtService.hasRefreshToken(refreshTokenValue)) {
            logger.debug("Refresh token not in database")
            return ResponseEntity.status(403).build()
        }

        // Generate a new token and put it into the database.
        val authToken = jwtService.generateAuthToken(
            refreshTokenResult.username,
            refreshTokenResult.roles,
        )
        jwtService.replaceRefreshToken(
            refreshTokenResult.username,
            refreshTokenValue,
            authToken.refreshToken
        )

        return ResponseEntity.ok(
            TokenRefreshDto(
                auth = TokenRefreshDto.Token(
                    token = authToken.authToken,
                    expiresIn = jwtConfig.tokenLifetime,
                ),
                refresh = TokenRefreshDto.Token(
                    token = authToken.refreshToken,
                    expiresIn = jwtConfig.refreshLifetime,
                ),
            )
        )
    }
}
