package me.polynom.polycloud.apps.auth.oidc.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * An annotation that enables the bean only when the OIDC plugin is enabled.
 */
@ConditionalOnProperty(
    name = ["me.polynom.polycloud.auth.oidc.enabled"],
    havingValue = "true"
)
annotation class PluginEnabled {}