package me.polynom.polycloud.apps.files.service

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.storage.StoragePath
import org.springframework.stereotype.Component
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.InputStream

@Component
@PluginEnabled
class FilesService(val storageService: StorageService) {

    fun listFiles(user: String, path: StoragePath): List<StoragePath> {
        val (storage, relPath) = storageService.resolveMount(path)
        if (storage != null) {
            return storage.listFiles(user, relPath).map {
                path.merge(StoragePath(listOf(it.components[it.components.size - 1]), true))
            }
        } else {
            // FIXME: iterate mountpoints
            // FIXME: user facing error
            throw IllegalArgumentException("MOOP")
        }
    }

    fun getFile(user: String, path: StoragePath): InputStream {
        val (storage, relPath) = storageService.resolveMount(path)
        // FIXME: user facing error
        storage ?: throw IllegalArgumentException("MOOP")
        return storage.getFile(user, relPath)
    }

    fun putFile(user: String, path: StoragePath, data: InputStream) {
        val (storage, relPath) = storageService.resolveMount(path)
        // FIXME: user facing error
        storage ?: throw IllegalArgumentException("MOOP")
        storage.putFile(user, relPath, data)
    }

    fun delete(user: String, path: StoragePath) {
        val (storage, relPath) = storageService.resolveMount(path)
        // FIXME: user facing error
        storage ?: throw IllegalArgumentException("MOOP")
        storage.delete(user, relPath)
    }
}