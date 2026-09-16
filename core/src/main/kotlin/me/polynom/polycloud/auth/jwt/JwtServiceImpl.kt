package me.polynom.polycloud.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import me.polynom.polycloud.config.JwtConfig
import me.polynom.polycloud.plugin.auth.JwtService
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

private object JwtConstants {
    val ISSUER = "polycloud"
    val ROLES = "roles"
}

/**
 * Implementation of the service creating and validating JWTs.
 */
@Service
class JwtServiceImpl(
    /** The JWT configuration. */
    private val jwtConfig: JwtConfig,
) : JwtService {
    /** The algorithm used for signing. */
    private val algorithm = Algorithm.HMAC512(jwtConfig.secret)

    /** The class verifying the JWT. */
    private val verifier: JWTVerifier =
        JWT
            .require(algorithm)
            .withIssuer(JwtConstants.ISSUER)
            .acceptExpiresAt(jwtConfig.tokenLifetime)
            .build()

    override fun generateToken(
        username: String,
        roles: List<String>,
        extra: Map<String, String>,
    ): String {
        val jwt =
            JWT
                .create()
                .withIssuer(JwtConstants.ISSUER)
                .withSubject(username)
                .withIssuedAt(Instant.now(Clock.systemDefaultZone()))
                .withArrayClaim(
                    JwtConstants.ROLES,
                    roles.toTypedArray(),
                )
        extra.forEach { (key, value) -> jwt.withClaim(key, value) }
        return jwt.sign(algorithm)
    }

    override fun verifyToken(token: String): AuthVerificationResult? {
        try {
            val decoded = verifier.verify(token)
            return AuthVerificationResult(
                username = decoded.subject,
                roles =
                    decoded
                        .getClaim(
                            JwtConstants.ROLES,
                        ).asList(String::class.java),
            )
        } catch (e: JWTVerificationException) {
            return null
        }
    }
}
