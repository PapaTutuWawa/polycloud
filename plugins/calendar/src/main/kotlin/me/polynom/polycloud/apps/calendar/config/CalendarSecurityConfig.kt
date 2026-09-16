package me.polynom.polycloud.apps.calendar.config

import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.security.AuthenticatedPath
import me.polynom.polycloud.plugin.security.PathAuthenticationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Security configuration for the calendar plugin.
 */
@Configuration
@PluginEnabled
open class CalendarSecurityConfig {
    @Bean
    open fun calendarPluginSecurityConfig(): PathAuthenticationConfig =
        PathAuthenticationConfig(
            paths =
                listOf(
                    AuthenticatedPath("/api/apps/calendar/calendar/*", authenticated = false),
                    AuthenticatedPath("/api/apps/calendar/calendar/*/events", authenticated = false),
                ),
        )
}
