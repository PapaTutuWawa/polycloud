package me.polynom.polycloud.apps.calendar.config

import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

@PluginEnabled
@Configuration
open class JsonConfig {
    @Bean
    open fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}