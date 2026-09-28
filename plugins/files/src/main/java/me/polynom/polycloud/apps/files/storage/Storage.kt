package me.polynom.polycloud.apps.files.storage

import org.springframework.core.io.Resource
import java.io.InputStream
import java.io.OutputStream

interface Storage {
    fun listFiles(
        user: String,
        path: StoragePath,
    ): List<EntryMeta>

    fun getFile(
        user: String,
        path: StoragePath,
    ): Resource

    fun putFile(
        user: String,
        path: StoragePath,
        data: InputStream,
    )

    fun delete(
        user: String,
        path: StoragePath,
    )
}
