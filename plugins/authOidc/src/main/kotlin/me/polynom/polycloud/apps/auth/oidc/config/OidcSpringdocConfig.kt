package me.polynom.polycloud.apps.auth.oidc.config

import me.polynom.polycloud.apps.auth.oidc.autoconfigure.PluginEnabled
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * SpringDoc config for OIDC.
 */
@Configuration
@PluginEnabled
open class OidcSpringdocConfig {
    /**
     * SpringDoc config for OIDC.
     */
    @Bean("oidcOpenApi")
    open fun oidcOpenApi(): GroupedOpenApi {
        return GroupedOpenApi
            .builder()
            .group("oidc")
            .packagesToScan("me.polynom.polycloud.apps.auth.oidc")
            .build()
    }
}