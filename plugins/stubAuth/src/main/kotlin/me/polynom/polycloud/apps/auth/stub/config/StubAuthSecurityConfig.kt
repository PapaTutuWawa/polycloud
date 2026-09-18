package me.polynom.polycloud.apps.auth.stub.config

import me.polynom.polycloud.apps.auth.stub.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.security.AuthenticatedPath
import me.polynom.polycloud.plugin.security.PathAuthenticationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Web security configuration.
 */
@PluginEnabled
@Configuration
open class StubAuthSecurityConfig {
    /**
     * Configuration of authenticated routes of the stub auth API.
     */
    @Bean("stubAuthSecurityName")
    open fun stubAuthSecurityConfig() =
        PathAuthenticationConfig(
            paths =
                listOf(
                    AuthenticatedPath("/api/auth/stub/authenticate", authenticated = false),
                ),
        )
}
