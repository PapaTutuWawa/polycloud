package me.polynom.polycloud.apps.example.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * Annotation that enables beans only when
 * me.polynom.polycloud.apps.example.enabled is true.
 */
@ConditionalOnProperty(
    name = ["me.polynom.polycloud.apps.example.enabled"],
    havingValue = "true",
)
annotation class PluginEnabled() {}
