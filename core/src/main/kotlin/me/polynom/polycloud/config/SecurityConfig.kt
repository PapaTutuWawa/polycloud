package me.polynom.polycloud.config

import me.polynom.polycloud.plugin.security.AuthenticatedPath
import me.polynom.polycloud.plugin.security.PathAuthenticationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Web security configuration.
 */
@Configuration
class SecurityConfig {
    /**
     * Configuration of authenticated routes of the core API.
     */
    @Bean
    fun coreSecurityConfig() =
        PathAuthenticationConfig(
            paths =
                listOf(
                    AuthenticatedPath("/**"),
                    AuthenticatedPath("/api/v1/public/**", authenticated = false),
                    // Authentication
                    AuthenticatedPath("/api/v1/auth/jwt/refresh", authenticated = false),
                    // Actuator
                    AuthenticatedPath("/actuator/**", authenticated = false),
                    // API Docs
                    AuthenticatedPath("/v3/**", authenticated = false),
                    AuthenticatedPath("/swagger-ui/**", authenticated = false),
                ),
        )
}
