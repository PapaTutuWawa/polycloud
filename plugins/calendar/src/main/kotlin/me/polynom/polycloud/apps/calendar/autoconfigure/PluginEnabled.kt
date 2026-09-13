package me.polynom.polycloud.apps.calendar.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * Class that only enables beans when me.polynom.polycloud.apps.calendar.enabled is true.
 */
@ConditionalOnProperty(
    name = ["me.polynom.polycloud.apps.calendar.enabled"],
    havingValue = "true",
)
annotation class PluginEnabled()
