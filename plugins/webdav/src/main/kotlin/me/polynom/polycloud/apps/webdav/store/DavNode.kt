package me.polynom.polycloud.apps.webdav.store

import org.springframework.core.io.Resource
import java.time.Instant
import java.util.UUID

/**
 * A node that can be addressed inside the DAV resource tree. Either a [DavCollection] or a [DavFile].
 */
sealed interface DavNode {
    val lastModified: Instant
    val etag: String

    /** Namespace-qualified dead (custom) properties set on this node via PROPPATCH. */
    val deadProperties: MutableMap<DavPropertyName, String>
}

data class DavCollection(
    override val lastModified: Instant = Instant.now(),
    override val etag: String = newEtag(),
    override val deadProperties: MutableMap<DavPropertyName, String> = mutableMapOf(),
) : DavNode

/**
 * [resource] is read lazily - a [DavFile] for a PROPFIND listing entry or an existence check
 * never has to pull the whole file into memory, only [ResourceHandler.get]'s GET body does.
 */
data class DavFile(
    val resource: Resource,
    val contentType: String,
    override val lastModified: Instant = Instant.now(),
    override val etag: String = newEtag(),
    override val deadProperties: MutableMap<DavPropertyName, String> = mutableMapOf(),
) : DavNode

/** A namespace-qualified property name, as used by PROPFIND/PROPPATCH. */
data class DavPropertyName(
    val namespace: String,
    val localName: String,
)

fun newEtag(): String = "\"${UUID.randomUUID()}\""
