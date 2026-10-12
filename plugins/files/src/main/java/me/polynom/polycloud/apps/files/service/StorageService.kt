package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.config.StorageConfig
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import me.polynom.polycloud.apps.files.storage.impls.LocalStorage
import org.springframework.stereotype.Component
import kotlin.math.min

/**
 * Manages the paths where certain storage backends are mounted.
 * Contains main-logic to convert between vfs path <-> backend + rel-path.
 * Makes sure that storage backends are reachable via VFS even
 * if toplevel backends do not provide folders to reach nested backend.
 */
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

    /**
     * Resolves a vfs path to a specific storage backend and a backend-relative path.
     * This matching is greedy, so a path is always matched to the deepest possible backend mount.
     * @path the vfs path to resolve to a backend.
     * @return the storage backend if found and a backend-relative path.
     */
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

    /**
     * Lists all mounted storage backends below the specified path
     * and returns the path of all folders that need to exist at
     * that level such that each backend mount is reachable in the vfs.
     * @param path the vfs path to list mounts in.
     * @return set of folders at path which need to exist for mount navigability/reachability.
     */
    fun listMounts(path: StoragePath): Set<StoragePath> =
        mapper.keys
            .asSequence()
            // Only mounts that are at least one level deeper may match
            .filter { it.folderDepth() >= path.folderDepth() + 1 }
            .map { it.split(path.folderDepth()) }
            // Match mounts that are in the path's directory
            .filter { it.first == path.folder() }
            // Get that mounts name
            .map { StoragePath(it.second.components.take(1), false) }
            // Deduplicate
            .toSet()
}
