package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.config.StorageConfig
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import me.polynom.polycloud.apps.files.storage.impls.LocalStorage
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
@PluginEnabled
class StorageService(
    storageConfig: StorageConfig,
) {
    val mapper: Map<StoragePath, Storage> = initMapping(storageConfig)
    val maxDepth: Int = mapper.keys.maxOfOrNull { it.components.size } ?: 0

    private fun initMapping(storageConfig: StorageConfig): Map<StoragePath, Storage> {
        val mapper = mutableMapOf<StoragePath, Storage>()
        storageConfig.local.forEach { local ->
            val storage = LocalStorage(local.mount, local.path.normalize(), local.shared)

            if (mapper.putIfAbsent(local.mount, storage) != null) {
                throw IllegalArgumentException("Two storage backends try to mount at ${local.mount}")
            }
        }

        return mapper
    }

    fun resolveMount(path: StoragePath): Pair<Storage?, StoragePath> {
        var d = min(path.folderDepth(), maxDepth)
        while (d >= 0) {
            val (mount, relPath) = path.split(d)

            mapper[mount]?.let {
                return Pair(it, relPath)
            }

            d--
        }

        return Pair(null, path)
    }

    fun listMounts(path: StoragePath): List<StoragePath> =
        mapper.keys
            // Only mounts that are one level deeper may match
            .filter { it.folderDepth() == path.folderDepth() + 1 }
            .map { it.split(it.folderDepth() - 1) }
            // Match mounts that are in the path's directory
            .filter { it.first == path.folder() }
            // Get that mounts name
            .map { it.second }
            .toList()
}
