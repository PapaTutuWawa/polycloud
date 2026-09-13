package me.polynom.polycloud.apps.files.config

import org.springframework.boot.context.properties.bind.ConstructorBinding

/**
 * Example config
 *           backends:
 *             - type: "local"
 *               path: "/srv/root"
 *               mount: "/"
 */
data class LocalStorageConfig @ConstructorBinding constructor(
    /**
     * Absolute path on the host that serves as the root of the backend
     */
    val path: String,
    /**
     * The path in the virtual file system where this backend exposes its files
     */
    val mount: String,
    /**
     * Whether file access is separated between users or a common file pool is accessed (use with caution!)
     */
    val shared: Boolean = false,
)
