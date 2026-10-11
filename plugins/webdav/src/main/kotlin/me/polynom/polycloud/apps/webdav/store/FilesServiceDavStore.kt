package me.polynom.polycloud.apps.webdav.store

import me.polynom.polycloud.apps.files.exceptions.PathEmptyException
import me.polynom.polycloud.apps.files.exceptions.StoragePathDoesNotExistException
import me.polynom.polycloud.apps.files.exceptions.StorageQuotaException
import me.polynom.polycloud.apps.files.exceptions.StorageReadOnlyException
import me.polynom.polycloud.apps.files.exceptions.StorageWrongTypeException
import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.service.FilesService
import me.polynom.polycloud.apps.files.service.StorageService
import me.polynom.polycloud.apps.files.storage.Storage
import me.polynom.polycloud.apps.files.storage.StoragePath
import me.polynom.polycloud.apps.files.storage.StorageStat
import me.polynom.polycloud.apps.webdav.api.DavException
import me.polynom.polycloud.apps.webdav.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.auth.UserContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.nio.file.FileAlreadyExistsException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val HEX_RADIX = 16
private val VIRTUAL_DIRECTORY_STAT = StorageStat(directory = true, size = null, lastModified = Instant.EPOCH)

/**
 * [DavStore] backed by the files plugin's [FilesService]/[StorageService], user-rooted at
 * whoever is authenticated for the current request (via [UserContext]) - WebDav.md's step 3.
 * [InMemoryDavStore] remains unused in production; it still backs the webdav plugin's own
 * protocol-surface tests.
 *
 * Known limitations: copying/moving a folder across two different storage backends is not
 * supported (file copy/move across backends is); PUT buffers the whole request body in memory
 * before writing it; locks and PROPPATCH dead properties are still in-memory only (unrelated to
 * this store, see [me.polynom.polycloud.apps.webdav.lock.LockManager]).
 */
@PluginEnabled
@Component
class FilesServiceDavStore(
    private val filesService: FilesService,
    private val storageService: StorageService,
    private val userContext: UserContext,
) : DavStore {
    /**
     * PROPPATCH-set custom properties, keyed by path. [get] hands out this exact mutable map
     * reference so [me.polynom.polycloud.apps.webdav.api.PropertyHandler]'s direct mutation of
     * `node.deadProperties` actually persists across requests - there is nothing else backing
     * it, by design (in-memory only, lost on restart).
     */
    private val deadPropertiesByPath = ConcurrentHashMap<String, MutableMap<DavPropertyName, String>>()

    override fun exists(path: String): Boolean = stat(path) != null

    override fun get(path: String): DavNode? {
        val stat = stat(path) ?: return null
        val deadProperties = deadPropertiesByPath.computeIfAbsent(path) { mutableMapOf() }
        return if (stat.directory) {
            DavCollection(lastModified = stat.lastModified, etag = etagFor(stat), deadProperties = deadProperties)
        } else {
            val resource = filesService.getFile(currentUser(), toStoragePath(path, file = true))
            DavFile(
                resource = resource,
                contentType = ContentTypes.resolve(path, null),
                lastModified = stat.lastModified,
                etag = etagFor(stat),
                deadProperties = deadProperties,
            )
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override fun children(path: String): List<String> {
        // An empty, a missing, and a non-folder path all mean the same thing here: no children.
        val entries =
            try {
                filesService.listFiles(currentUser(), toStoragePath(path, file = false))
            } catch (e: Exception) {
                when (e) {
                    is PathEmptyException, is StoragePathDoesNotExistException, is StorageWrongTypeException -> emptyList()
                    else -> throw e
                }
            }

        return entries.map { DavPath.join(path, it.name) }
    }

    override fun putFile(
        path: String,
        content: ByteArray,
        contentType: String,
    ): DavFile {
        val user = currentUser()
        val storagePath = toStoragePath(path, file = true)
        val (storage, _) = resolveStorage(storagePath)
        val upload = Upload(id = UUID.randomUUID(), size = content.size.toLong(), user = user, path = storagePath.toString(), offset = 0)

        mapStorageExceptions {
            storage.prepareUpload(upload)
            ByteArrayInputStream(content).use { storage.continueUpload(upload, it) }
            storage.finaliseUpload(upload)
        }

        return get(path) as? DavFile ?: error("File at $path vanished right after being written")
    }

    override fun makeCollection(path: String): DavCollection {
        val (storage, relPath) = resolveStorage(toStoragePath(path, file = false))
        mapStorageExceptions { storage.createDirectory(currentUser(), relPath) }
        return get(path) as? DavCollection ?: error("Folder at $path vanished right after being created")
    }

    override fun delete(path: String) {
        val stat = stat(path) ?: throw DavException(HttpStatus.NOT_FOUND.value())
        val (storage, relPath) = resolveStorage(toStoragePath(path, file = !stat.directory))
        mapStorageExceptions { storage.delete(currentUser(), relPath) }
        clearDeadProperties(path)
    }

    override fun copy(
        source: String,
        destination: String,
        shallow: Boolean,
    ): Boolean {
        val transfer = resolveTransfer(source, destination)
        mapStorageExceptions {
            when {
                transfer.sameBackend && shallow && transfer.sourceStat.directory -> replaceWithEmptyFolder(transfer)
                transfer.sameBackend -> transfer.sourceStorage.copy(transfer.user, transfer.sourceRelative, transfer.destinationRelative)
                !transfer.sourceStat.directory -> copyFileAcrossBackends(transfer, destination)
                else -> throw DavException(HttpStatus.NOT_IMPLEMENTED.value(), "Cross-backend folder copy is not supported yet")
            }
        }
        relocateDeadProperties(source, destination, keepSource = true, deep = !shallow)

        return transfer.destinationExisted
    }

    override fun move(
        source: String,
        destination: String,
    ): Boolean {
        val transfer = resolveTransfer(source, destination)
        mapStorageExceptions {
            when {
                transfer.sameBackend -> {
                    transfer.sourceStorage.move(transfer.user, transfer.sourceRelative, transfer.destinationRelative)
                }

                !transfer.sourceStat.directory -> {
                    copyFileAcrossBackends(transfer, destination)
                    transfer.sourceStorage.delete(transfer.user, transfer.sourceRelative)
                }

                else -> {
                    throw DavException(HttpStatus.NOT_IMPLEMENTED.value(), "Cross-backend folder move is not supported yet")
                }
            }
        }
        relocateDeadProperties(source, destination, keepSource = false, deep = true)

        return transfer.destinationExisted
    }

    private fun clearDeadProperties(path: String) {
        deadPropertiesByPath.remove(path)
        deadPropertiesByPath.keys.removeIf { DavPath.isAncestorOf(path, it) }
    }

    /** Shared by [copy] (keepSource=true) and [move] (keepSource=false). */
    private fun relocateDeadProperties(
        source: String,
        destination: String,
        keepSource: Boolean,
        deep: Boolean,
    ) {
        takeDeadProperties(source, keepSource)?.let { deadPropertiesByPath[destination] = it }
        if (!deep) {
            return
        }

        deadPropertiesByPath.keys
            .filter { DavPath.isAncestorOf(source, it) }
            .toList()
            .forEach { key ->
                val relocated = DavPath.normalize(destination + key.removePrefix(source))
                takeDeadProperties(key, keepSource)?.let { deadPropertiesByPath[relocated] = it }
            }
    }

    private fun takeDeadProperties(
        path: String,
        keep: Boolean,
    ): MutableMap<DavPropertyName, String>? = if (keep) deadPropertiesByPath[path]?.toMutableMap() else deadPropertiesByPath.remove(path)

    private fun currentUser(): String = userContext.getUser()?.username ?: throw DavException(HttpStatus.UNAUTHORIZED.value())

    private fun toStoragePath(
        path: String,
        file: Boolean,
    ): StoragePath = StoragePath(DavPath.components(path), file)

    private fun resolveStorage(storagePath: StoragePath): Pair<Storage, StoragePath> {
        val (storage, relPath) = storageService.resolveMount(storagePath)
        if (storage == null) {
            throw DavException(HttpStatus.CONFLICT.value(), "No storage mounted at this path")
        }

        return storage to relPath
    }

    /** Also covers the root and virtual, storage-less parent folders of mount points. */
    private fun stat(path: String): StorageStat? {
        if (path == DavPath.ROOT) {
            return VIRTUAL_DIRECTORY_STAT
        }

        val storagePath = toStoragePath(path, file = false)
        val (storage, relPath) = storageService.resolveMount(storagePath)
        return when {
            storage != null -> storage.stat(currentUser(), relPath)
            storageService.listMounts(storagePath).isNotEmpty() -> VIRTUAL_DIRECTORY_STAT
            else -> null
        }
    }

    private fun etagFor(stat: StorageStat): String {
        val sizeMarker = stat.size?.toString(HEX_RADIX) ?: "dir"
        return "\"${stat.lastModified.toEpochMilli().toString(HEX_RADIX)}-$sizeMarker\""
    }

    private data class Transfer(
        val user: String,
        val sourceStat: StorageStat,
        val destinationExisted: Boolean,
        val sourceStorage: Storage,
        val sourceRelative: StoragePath,
        val destinationStorage: Storage,
        val destinationRelative: StoragePath,
        val sourceStoragePath: StoragePath,
    ) {
        val sameBackend get() = sourceStorage === destinationStorage
    }

    private fun resolveTransfer(
        source: String,
        destination: String,
    ): Transfer {
        val user = currentUser()
        val sourceStat = stat(source) ?: throw DavException(HttpStatus.NOT_FOUND.value())
        val destinationExisted = exists(destination)

        val sourceStoragePath = toStoragePath(source, file = !sourceStat.directory)
        val destinationStoragePath = toStoragePath(destination, file = !sourceStat.directory)
        val (sourceStorage, sourceRelative) = resolveStorage(sourceStoragePath)
        val (destinationStorage, destinationRelative) = resolveStorage(destinationStoragePath)

        return Transfer(
            user,
            sourceStat,
            destinationExisted,
            sourceStorage,
            sourceRelative,
            destinationStorage,
            destinationRelative,
            sourceStoragePath,
        )
    }

    private fun replaceWithEmptyFolder(transfer: Transfer) {
        if (transfer.destinationExisted) {
            transfer.destinationStorage.delete(transfer.user, transfer.destinationRelative)
        }

        transfer.destinationStorage.createDirectory(transfer.user, transfer.destinationRelative)
    }

    private fun copyFileAcrossBackends(
        transfer: Transfer,
        destination: String,
    ) {
        val content = filesService.getFile(transfer.user, transfer.sourceStoragePath).inputStream.use { it.readBytes() }
        putFile(destination, content, ContentTypes.resolve(destination, null))
    }

    private fun <T> mapStorageExceptions(block: () -> T): T =
        try {
            block()
        } catch (e: StoragePathDoesNotExistException) {
            throw DavException(HttpStatus.NOT_FOUND.value(), e.message, e)
        } catch (e: StorageWrongTypeException) {
            throw DavException(HttpStatus.CONFLICT.value(), e.message, e)
        } catch (e: StorageReadOnlyException) {
            throw DavException(HttpStatus.FORBIDDEN.value(), e.message, e)
        } catch (e: StorageQuotaException) {
            throw DavException(HttpStatus.INSUFFICIENT_STORAGE.value(), e.message, e)
        } catch (e: FileAlreadyExistsException) {
            throw DavException(HttpStatus.CONFLICT.value(), e.message, e)
        }
}
