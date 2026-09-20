package me.polynom.polycloud.apps.auth.oidc.config

import me.polynom.polycloud.apps.auth.oidc.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.security.AuthenticatedPath
import me.polynom.polycloud.plugin.security.PathAuthenticationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for HTTP security
 */
@Configuration
@PluginEnabled
open class OidcSecurityConfig {
    @Bean("oidcPluginSecurityConfig")
    open fun oidcPluginSecurityConfig(): PathAuthenticationConfig =
        PathAuthenticationConfig(
            paths =
                listOf(
                    AuthenticatedPath("/api/auth/oidc/authenticate", authenticated = false),
                    AuthenticatedPath("/api/auth/oidc/proxy/token", authenticated = false),
                ),
        )
}