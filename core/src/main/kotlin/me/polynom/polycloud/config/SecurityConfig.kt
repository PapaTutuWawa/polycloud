package me.polynom.polycloud.config

import me.polynom.polycloud.plugin.security.AuthenticatedPath
import me.polynom.polycloud.plugin.security.PathAuthenticationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

/**
 * Web security configuration.
 */
@Configuration
class SecurityConfig {
    /**
     * Configuration of authenticated routes of the core API.
     */
    @Bean
    @Order(1)
    fun coreSecurityConfig() =
        PathAuthenticationConfig(
            paths =
                listOf(
                    AuthenticatedPath("/**"),
                    AuthenticatedPath("/api/v1/public/**", authenticated = false),
                    AuthenticatedPath("/actuator/**", authenticated = false),
                ),
        )
}
