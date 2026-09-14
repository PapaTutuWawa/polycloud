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
class StorageService (storageConfig: StorageConfig) {

    val mapper: Map<String, Storage> = initMapping(storageConfig)
    val maxDepth: Int = mapper.keys.maxOfOrNull { it.split("/").size - 2 } ?: 0

    private fun initMapping(storageConfig: StorageConfig): Map<String, Storage> {
        val mapper = mutableMapOf<String, Storage>()
        storageConfig.local.forEach { local ->
            if (local.mount.file) {
                throw IllegalArgumentException("Mount ${local.mount} has to end with `/`")
            }

            val mount = local.mount.toString()
            val storage = LocalStorage(local.path.normalize(), local.shared)
            if (mapper.putIfAbsent(mount, storage) != null) {
                throw IllegalArgumentException("Two storage backends try to mount at $mount")
            }
        }

        return mapper
    }

    fun resolveMount(path: StoragePath): Pair<Storage?, StoragePath> {
        var d = min(path.folderDepth(), maxDepth)
        while (d >= 0) {
            val (mount, relPath) = path.split(d)

            mapper[mount.toString()]?.let {
                return Pair(it, relPath)
            }

            d--
        }

        return Pair(null, path)
    }
}