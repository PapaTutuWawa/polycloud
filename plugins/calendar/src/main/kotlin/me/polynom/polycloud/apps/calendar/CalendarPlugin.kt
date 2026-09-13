package me.polynom.polycloud.apps.calendar

import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.PolycloudPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Main plugin interface for the calendar.
 */
@Component
@PluginEnabled
class CalendarPlugin : PolycloudPlugin {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun register() {
        logger.info("Calendar registered")
    }
}