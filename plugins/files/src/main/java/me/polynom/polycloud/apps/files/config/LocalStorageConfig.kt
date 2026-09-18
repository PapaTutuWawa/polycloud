package me.polynom.polycloud.apps.files.config

import me.polynom.polycloud.apps.files.storage.StoragePath
import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.nio.file.Path

data class LocalStorageConfig
    @ConstructorBinding
    constructor(
        /**
         * Absolute path on the host that serves as the root of the backend
         */
        val path: Path,
        /**
         * The path in the virtual file system where this backend exposes its files
         */
        val mount: StoragePath,
        /**
         * Whether file access is separated between users or a common file pool is accessed (use with caution!)
         */
        val shared: Boolean = false,
    )
