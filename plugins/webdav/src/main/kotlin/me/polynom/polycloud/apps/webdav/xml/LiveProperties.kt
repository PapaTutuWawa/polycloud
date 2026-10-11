package me.polynom.polycloud.apps.webdav.xml

import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavFile
import me.polynom.polycloud.apps.webdav.store.DavNode
import me.polynom.polycloud.apps.webdav.store.DavPath
import me.polynom.polycloud.apps.webdav.store.DavPropertyName
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Renders the WebDAV "live" properties (RFC 4918 section 15) of a [DavNode]. These always take
 * precedence over any same-named dead property set via PROPPATCH.
 */
object LiveProperties {
    private val HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

    fun render(
        path: String,
        node: DavNode,
    ): Map<DavPropertyName, String> {
        val displayName = DavPath.name(path).ifEmpty { "/" }
        val properties =
            mutableMapOf(
                prop("resourcetype") to if (node is DavCollection) "<D:collection/>" else "",
                prop("displayname") to DavXml.escapeXml(displayName),
                prop("getetag") to DavXml.escapeXml(node.etag),
                prop("getlastmodified") to HTTP_DATE.format(node.lastModified),
                prop("creationdate") to DateTimeFormatter.ISO_INSTANT.format(node.lastModified),
            )

        when (node) {
            is DavFile -> {
                properties[prop("getcontentlength")] = node.content.size.toString()
                properties[prop("getcontenttype")] = DavXml.escapeXml(node.contentType)
            }

            is DavCollection -> {
                properties[prop("getcontentlength")] = "0"
            }
        }

        return properties
    }

    private fun prop(localName: String) = DavPropertyName(DAV_NAMESPACE, localName)
}
