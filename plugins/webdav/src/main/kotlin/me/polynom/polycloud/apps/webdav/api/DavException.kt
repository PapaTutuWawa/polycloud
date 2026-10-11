package me.polynom.polycloud.apps.webdav.api

/** Thrown by a verb handler to abort with a specific HTTP [status] and optional plain-text [message]. */
class DavException(
    val status: Int,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
