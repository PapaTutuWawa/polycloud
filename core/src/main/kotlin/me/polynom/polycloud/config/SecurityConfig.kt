package me.polynom.polycloud.config

import me.polynom.polycloud.auth.AuthenticatedPath
import me.polynom.polycloud.auth.PathAuthenticationConfig
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
                ),
        )
}
