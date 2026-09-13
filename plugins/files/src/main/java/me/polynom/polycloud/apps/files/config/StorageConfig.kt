package me.polynom.polycloud.apps.files.config

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties("me.polynom.polycloud.apps.files.storage")
@PluginEnabled
class StorageConfig @ConstructorBinding constructor(
    val local: List<LocalStorageConfig>,
)