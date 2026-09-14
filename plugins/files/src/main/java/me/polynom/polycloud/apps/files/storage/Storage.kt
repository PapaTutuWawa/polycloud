package me.polynom.polycloud.apps.files.storage

import java.io.InputStream
import java.io.OutputStream

interface Storage {

    fun listFiles(user: String, path: StoragePath): List<StoragePath>

    fun getFile(user: String, path: StoragePath): InputStream

    fun putFile(user: String, path: StoragePath, data: InputStream)

    fun delete(user: String, path: StoragePath)
}