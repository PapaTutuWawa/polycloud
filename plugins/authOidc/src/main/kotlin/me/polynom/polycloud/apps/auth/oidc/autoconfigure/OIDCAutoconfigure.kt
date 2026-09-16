package me.polynom.polycloud.apps.auth.oidc.autoconfigure

import me.polynom.polycloud.apps.auth.oidc.config.OIDCConfig
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Auto-configuration entrypoint for SpringBoot.
 */
@AutoConfiguration
@EnableConfigurationProperties(OIDCConfig::class)
@PluginEnabled
class OIDCAutoconfigure
