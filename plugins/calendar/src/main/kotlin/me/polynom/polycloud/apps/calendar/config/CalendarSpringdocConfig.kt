package me.polynom.polycloud.apps.calendar.config

import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * SpringDoc configuration.
 */
@Configuration
@PluginEnabled
open class CalendarSpringdocConfig {
    /**
     * SpringDoc config for the calendar plugin.
     */
    @Bean("calendarOpenApi")
    open fun calendarOpenApi(): GroupedOpenApi {
        return GroupedOpenApi
            .builder()
            .group("calendar")
            .packagesToScan("me.polynom.polycloud.apps.calendar")
            .build()
    }
}