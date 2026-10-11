package me.polynom.polycloud.apps.webdav.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * Annotation that enables beans only when
 * me.polynom.polycloud.apps.webdav.enabled is true.
 */
@ConditionalOnProperty(
    name = ["me.polynom.polycloud.apps.webdav.enabled"],
    havingValue = "true",
)
annotation class PluginEnabled
