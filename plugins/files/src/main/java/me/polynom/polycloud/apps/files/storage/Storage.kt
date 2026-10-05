package me.polynom.polycloud.apps.files.storage

import me.polynom.polycloud.apps.files.persistence.entities.Upload
import org.springframework.core.io.Resource
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

interface Storage {
    fun listFiles(
        user: String,
        path: StoragePath,
    ): List<EntryMeta>

    fun getFile(
        user: String,
        path: StoragePath,
    ): Resource

    fun delete(
        user: String,
        path: StoragePath,
    )

    fun stageUpload(upload: Upload)

    fun patchUpload(
        upload: Upload,
        stream: InputStream,
    ): Long

    fun finaliseUpload(
        user: String,
        path: StoragePath,
        upload: Upload,
    )

    fun deleteUpload(upload: Upload)

    val mount: StoragePath
}
