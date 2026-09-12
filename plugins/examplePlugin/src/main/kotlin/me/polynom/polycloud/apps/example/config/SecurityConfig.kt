package me.polynom.polycloud.apps.example.config

import me.polynom.polycloud.apps.example.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.security.AuthenticatedPath
import me.polynom.polycloud.plugin.security.PathAuthenticationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Web security config.
 */
@PluginEnabled
@Configuration
open class ExampleSecurityConfig {
    @Bean
    open fun examplePluginSecurityConfig(): PathAuthenticationConfig = PathAuthenticationConfig(
        paths = listOf(
            AuthenticatedPath("/api/apps/example/test", authenticated = false),
        )
    )
}