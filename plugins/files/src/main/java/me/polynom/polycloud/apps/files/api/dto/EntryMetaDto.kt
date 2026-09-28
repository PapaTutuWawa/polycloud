package me.polynom.polycloud.apps.files.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class EntryMetaDto(
    /**
     * Name of the file/directory
     */
    val name: String,
    /**
     * Whether this entry is a file or a directory
     */
    val isDirectory: Boolean,
    /**
     * If entry is a file, how big is it in bytes
     */
    val size: Long?,
)
