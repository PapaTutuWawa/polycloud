package me.polynom.polycloud.apps.auth.stub.autoconfigure

import org.springframework.context.annotation.Profile
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * Annotation that enables the bean only when the stub auth plugin is enabled
 * *AND* the active profiles include "dev" or "test".
 */
@ConditionalOnProperty(
    name = ["me.polynom.polycloud.apps.auth.stub.enabled"],
    havingValue = "true",
)
@Profile("dev", "test")
annotation class PluginEnabled()
