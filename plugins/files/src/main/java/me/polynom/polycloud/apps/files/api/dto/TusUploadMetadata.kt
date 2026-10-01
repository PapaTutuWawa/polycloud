package me.polynom.polycloud.apps.files.api.dto

class TusUploadMetadata(
    private val data: Map<String, String>,
) {
    operator fun get(key: String): String? = data[key]

    fun toMap(): Map<String, String> = data
}
