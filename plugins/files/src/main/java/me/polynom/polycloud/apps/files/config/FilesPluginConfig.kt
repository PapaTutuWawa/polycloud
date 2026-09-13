package me.polynom.polycloud.apps.files.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties("me.polynom.polycloud.apps.files")
data class FilesPluginConfig @ConstructorBinding constructor(
    /**
     * List of configured file storage backends, map is parsed by StorageBackendConfigResolver
     */
    val backends: List<Map<String, Any>>,
)
