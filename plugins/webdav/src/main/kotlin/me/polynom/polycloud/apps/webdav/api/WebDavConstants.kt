package me.polynom.polycloud.apps.webdav.api

object WebDavConstants {
    /** The path the WebDAV servlet is mounted under. Must match the servlet registration. */
    const val MOUNT = "/dav"
    const val ALLOWED_METHODS = "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, COPY, MOVE, PROPFIND, PROPPATCH, LOCK, UNLOCK"
}

/** Non-standard HTTP headers used by WebDAV (RFC 4918), not present in Spring's [org.springframework.http.HttpHeaders]. */
object DavHeaders {
    const val DEPTH = "Depth"
    const val DESTINATION = "Destination"
    const val OVERWRITE = "Overwrite"
    const val LOCK_TOKEN = "Lock-Token"
    const val TIMEOUT = "Timeout"
    const val IF = "If"
}
