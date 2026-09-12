package me.polynom.polycloud.apps.files.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * Annotation that enables beans only when
 * me.polynom.polycloud.apps.files.enabled is true.
 */
@ConditionalOnProperty(
    name = ["me.polynom.polycloud.apps.files.enabled"],
    havingValue = "true",
)
annotation class PluginEnabled() {}
