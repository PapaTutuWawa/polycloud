package me.polynom.polycloud.apps.files.storage

import java.time.Instant

/**
 * Result of probing a path without knowing beforehand whether it is a file or a folder.
 * See [Storage.stat].
 */
data class StorageStat(
    val directory: Boolean,
    val size: Long?,
    val lastModified: Instant,
)
