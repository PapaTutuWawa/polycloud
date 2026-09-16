package me.polynom.polycloud.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import jakarta.transaction.Transactional
import me.polynom.polycloud.config.JwtConfig
import me.polynom.polycloud.persistence.entities.RefreshToken
import me.polynom.polycloud.persistence.repository.RefreshTokenRepository
import me.polynom.polycloud.plugin.auth.JwtService
import me.polynom.polycloud.plugin.auth.dto.AuthToken
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

/**
 * Implementation of the service creating and validating JWTs.
 */
@Service
class JwtServiceImpl(
    /** The JWT configuration. */
    private val jwtConfig: JwtConfig,
    private val refreshTokenRepository: RefreshTokenRepository,
) : JwtService {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /** The algorithm used for signing. */
    private val algorithm = Algorithm.HMAC512(jwtConfig.secret)

    /** The class verifying the authentication JWTs. */
    private val authVerifier: JWTVerifier =
        JWT
            .require(algorithm)
            .withIssuer(JwtConstants.ISSUER)
            .acceptExpiresAt(jwtConfig.tokenLifetime)
            .withClaim(JwtConstants.TYPE, JwtType.AUTH.value)
            .build()

    /** The class verifying the refresh JWT. */
    private val refreshVerifier: JWTVerifier =
        JWT
            .require(algorithm)
            .withIssuer(JwtConstants.ISSUER)
            .acceptExpiresAt(jwtConfig.refreshLifetime)
            .withClaim(JwtConstants.TYPE, JwtType.REFRESH.value)
            .build()

    private fun generateJwtToken(
        username: String,
        roles: List<String>,
        type: JwtType,
    ): String {
        return JWT
                .create()
                .withIssuer(JwtConstants.ISSUER)
                .withSubject(username)
                .withIssuedAt(Instant.now(Clock.systemDefaultZone()))
                .withClaim(JwtConstants.TYPE, type.value)
                .withArrayClaim(
                    JwtConstants.ROLES,
                    roles.toTypedArray(),
                ).sign(algorithm)
    }

    override fun generateAuthToken(
        username: String,
        roles: List<String>,
    ): AuthToken {
        return AuthToken(
            generateJwtToken(username, roles, JwtType.AUTH),
            jwtConfig.tokenLifetime,
            generateJwtToken(username, roles, JwtType.REFRESH),
            jwtConfig.refreshLifetime,
        )
    }

    private fun authResultFromJwt(decoded: DecodedJWT): AuthVerificationResult {
        return AuthVerificationResult(
            username = decoded.subject,
            roles =
                decoded
                    .getClaim(
                        JwtConstants.ROLES,
                    ).asList(String::class.java),
        )
    }

    override fun verifyAuthToken(token: String): AuthVerificationResult? {
        try {
            val decoded = authVerifier.verify(token)
            return authResultFromJwt(decoded)
        } catch (e: JWTVerificationException) {
            return null
        }
    }

    override fun verifyRefreshToken(token: String): AuthVerificationResult? {
        try {
            val decoded = refreshVerifier.verify(token)
            return authResultFromJwt(decoded)
        } catch (e: JWTVerificationException) {
            return null
        }
    }

    private fun sha256Hash(value: String): String {
        val tokenDigest = MessageDigest.getInstance("SHA-256")
        tokenDigest.update(value.toByteArray())
        return tokenDigest.digest().toHexString()
    }

    override fun saveRefreshToken(username: String, token: String) {
        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = sha256Hash(token),
                username = username,
            ),
        )
    }

    override fun hasRefreshToken(token: String): Boolean {
        logger.debug("hash: [{}] token: [{}]", sha256Hash(token), token)
        return refreshTokenRepository.findById(sha256Hash(token)).isPresent
    }

    @Transactional
    override fun replaceRefreshToken(username: String, old: String, new: String) {
        refreshTokenRepository.deleteById(sha256Hash(old))
        saveRefreshToken(username, new)
    }
}
