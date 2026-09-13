package me.polynom.polycloud.apps.files.autoconfigure

import me.polynom.polycloud.apps.files.config.StorageConfig
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

@AutoConfiguration
@EnableConfigurationProperties(StorageConfig::class)
@PluginEnabled
class FilesAutoconfigure {}