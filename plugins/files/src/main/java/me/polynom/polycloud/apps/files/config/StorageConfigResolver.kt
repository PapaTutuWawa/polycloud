package me.polynom.polycloud.apps.files.config

import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import org.springframework.stereotype.Component

/**
 * Resolves each entry in `backends` config to its concrete config
 * data class (e.g. LocalStorageBackendConfig) by using `type` to differentiate.
 */
@Component
class StorageConfigResolver(config: FilesPluginConfig) {

    val localBackends: List<LocalStorageConfig> = config.backends.filter { elem ->
        val type = elem["type"] as? String
            ?: throw IllegalStateException("Storage backend entry is missing `type` field: $elem")

        type == "local"
    }.map { elem -> resolve(elem, LocalStorageConfig::class.java) }

    private fun <T : Any> resolve(raw: Map<String, Any>, backendClass: Class<T>): T {
        val source = MapConfigurationPropertySource(raw)
        return Binder(source).bind("", Bindable.of(backendClass))
            .orElseThrow { IllegalStateException("Failed to bind storage backend of type '$backendClass': $raw") }
    }
}
