package me.polynom.polycloud.config

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Config for SpringDoc.
 */
@Configuration
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