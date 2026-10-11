package me.polynom.polycloud.apps.webdav.store

import org.springframework.http.MediaTypeFactory

/**
 * Resolves the content type to store for a PUT. Many WebDAV clients either omit the
 * `Content-Type` header or send a generic `application/octet-stream`, which would otherwise make
 * file browsers (including WebDAV clients themselves) unable to tell a PDF from a plain file. In
 * that case, fall back to guessing from the file extension.
 */
object ContentTypes {
    private val GENERIC = setOf("application/octet-stream", "content/unknown")
    private const val FALLBACK = "application/octet-stream"

    fun resolve(
        path: String,
        declared: String?,
    ): String {
        val base = declared?.substringBefore(';')?.trim()?.lowercase()
        if (declared != null && !base.isNullOrEmpty() && base !in GENERIC) {
            return declared
        }

        return MediaTypeFactory.getMediaType(DavPath.name(path)).map { it.toString() }.orElse(declared ?: FALLBACK)
    }
}
