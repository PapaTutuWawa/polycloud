package me.polynom.polycloud.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Config for SpringDoc.
 */
@Configuration
@SecurityScheme(
    name = "jwt",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
)
class SpringdocConfig {
    /**
     * SpringDoc config for the core API
     */
    @Bean("coreOpenApi")
    fun coreOpenApi(): GroupedOpenApi {
        return GroupedOpenApi
            .builder()
            .group("core")
            .pathsToMatch("/api/v1/**")
            .build()
    }
}