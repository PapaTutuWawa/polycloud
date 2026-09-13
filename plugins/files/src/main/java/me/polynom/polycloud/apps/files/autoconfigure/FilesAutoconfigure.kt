package me.polynom.polycloud.apps.files.autoconfigure

import me.polynom.polycloud.apps.files.config.FilesPluginConfig
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

@AutoConfiguration
@EnableConfigurationProperties(FilesPluginConfig::class)
@PluginEnabled
class FilesAutoconfigure {}