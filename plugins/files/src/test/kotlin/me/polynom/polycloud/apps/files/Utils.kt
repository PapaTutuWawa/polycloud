package me.polynom.polycloud.apps.files

import me.polynom.polycloud.apps.files.persistence.entities.Upload
import me.polynom.polycloud.apps.files.storage.StoragePath
import java.util.UUID

object Utils {
    fun createUpload(
        user: String,
        path: StoragePath,
        content: ByteArray,
    ) = Upload(
        UUID.randomUUID(),
        content.size.toLong(),
        user,
        path,
        offset = 0L,
    )

    fun String.into() = StoragePath(this)
}
