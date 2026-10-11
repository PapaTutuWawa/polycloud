package me.polynom.polycloud.apps.webdav.store

/**
 * Helpers for normalizing and manipulating WebDAV resource paths.
 *
 * Paths are represented as absolute, `/`-separated strings without a trailing slash,
 * except for the root collection, which is represented as `/`.
 */
object DavPath {
    const val ROOT = "/"

    fun normalize(rawPath: String): String {
        val collapsed = "/" + rawPath.split("/").filter { it.isNotEmpty() }.joinToString("/")
        return collapsed
    }

    fun parent(path: String): String {
        if (path == ROOT) {
            return ROOT
        }

        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash <= 0) ROOT else path.substring(0, lastSlash)
    }

    fun name(path: String): String {
        if (path == ROOT) {
            return ""
        }

        return path.substring(path.lastIndexOf('/') + 1)
    }

    fun join(
        parent: String,
        childName: String,
    ): String = if (parent == ROOT) "$ROOT$childName" else "$parent/$childName"

    /** The non-empty `/`-separated segments of [path], e.g. `/a/b` -> `["a", "b"]`, `/` -> `[]`. */
    fun components(path: String): List<String> = path.split("/").filter { it.isNotEmpty() }

    fun isAncestorOf(
        maybeAncestor: String,
        path: String,
    ): Boolean {
        if (maybeAncestor == ROOT) {
            return path != ROOT
        }

        return path.startsWith("$maybeAncestor/")
    }
}
