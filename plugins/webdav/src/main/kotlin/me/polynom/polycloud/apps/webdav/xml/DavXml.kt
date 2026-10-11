package me.polynom.polycloud.apps.webdav.xml

import me.polynom.polycloud.apps.webdav.lock.DavLock
import me.polynom.polycloud.apps.webdav.lock.LockDepth
import me.polynom.polycloud.apps.webdav.lock.LockScope
import me.polynom.polycloud.apps.webdav.store.DavPropertyName
import java.time.Duration
import java.time.Instant

const val DAV_NAMESPACE = "DAV:"

/** One `<D:response>` entry of a multistatus body. [foundProps] values are pre-rendered XML fragments. */
data class DavResponseEntry(
    val href: String,
    val foundProps: Map<DavPropertyName, String> = emptyMap(),
    val missingProps: List<DavPropertyName> = emptyList(),
)

/** Building of the small subset of WebDAV response XML (RFC 4918) this plugin needs: multistatus and lockdiscovery. */
object DavXml {
    fun escapeXml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    fun buildMultiStatus(entries: List<DavResponseEntry>): String {
        val namespaces = collectNamespaces(entries)
        val body = StringBuilder()
        entries.forEach { body.append(buildResponse(it, namespaces)) }

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append("<D:multistatus xmlns:D=\"DAV:\"")
            namespaces.forEach { (namespace, prefix) ->
                if (namespace != DAV_NAMESPACE) {
                    append(" xmlns:$prefix=\"${escapeXml(namespace)}\"")
                }
            }
            append(">")
            append(body)
            append("</D:multistatus>")
        }
    }

    fun buildLockDiscovery(
        lock: DavLock,
        lockRootHref: String,
    ): String {
        val depthText = if (lock.depth == LockDepth.INFINITY) "infinity" else "0"
        val scopeText = if (lock.scope == LockScope.SHARED) "shared" else "exclusive"
        val timeoutSeconds = Duration.between(Instant.now(), lock.expiresAt).seconds.coerceAtLeast(0)
        val ownerXml = lock.owner?.let { "<D:owner>${escapeXml(it)}</D:owner>" } ?: ""

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<D:prop xmlns:D=\"DAV:\"><D:lockdiscovery><D:activelock>" +
            "<D:locktype><D:write/></D:locktype>" +
            "<D:lockscope><D:$scopeText/></D:lockscope>" +
            "<D:depth>$depthText</D:depth>" +
            ownerXml +
            "<D:timeout>Second-$timeoutSeconds</D:timeout>" +
            "<D:locktoken><D:href>${escapeXml(lock.token)}</D:href></D:locktoken>" +
            "<D:lockroot><D:href>${escapeXml(lockRootHref)}</D:href></D:lockroot>" +
            "</D:activelock></D:lockdiscovery></D:prop>"
    }

    private fun buildResponse(
        entry: DavResponseEntry,
        namespaces: Map<String, String>,
    ): String =
        buildString {
            append("<D:response><D:href>${escapeXml(entry.href)}</D:href>")
            if (entry.foundProps.isNotEmpty()) {
                append("<D:propstat><D:prop>")
                entry.foundProps.forEach { (name, value) -> append(wrapProp(name, namespaces, value)) }
                append("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat>")
            }
            if (entry.missingProps.isNotEmpty()) {
                append("<D:propstat><D:prop>")
                entry.missingProps.forEach { name -> append(wrapProp(name, namespaces, null)) }
                append("</D:prop><D:status>HTTP/1.1 404 Not Found</D:status></D:propstat>")
            }
            append("</D:response>")
        }

    private fun wrapProp(
        name: DavPropertyName,
        namespaces: Map<String, String>,
        value: String?,
    ): String {
        val qualifiedName = qualifiedName(name, namespaces)
        return if (value.isNullOrEmpty()) {
            "<$qualifiedName/>"
        } else {
            "<$qualifiedName>$value</$qualifiedName>"
        }
    }

    /** RFC 4918's "no namespace" (an empty string) renders unprefixed - `xmlns:foo=""` is illegal XML. */
    private fun qualifiedName(
        name: DavPropertyName,
        namespaces: Map<String, String>,
    ): String {
        if (name.namespace.isEmpty()) {
            return name.localName
        }

        val prefix = namespaces[name.namespace] ?: "D"
        return "$prefix:${name.localName}"
    }

    private fun collectNamespaces(entries: List<DavResponseEntry>): Map<String, String> {
        val custom =
            entries
                .flatMap { it.foundProps.keys + it.missingProps }
                .map { it.namespace }
                .filter { it != DAV_NAMESPACE && it.isNotEmpty() }
                .distinct()
                .sorted()

        val namespaces = linkedMapOf(DAV_NAMESPACE to "D")
        custom.forEachIndexed { index, namespace -> namespaces[namespace] = "ns${index + 1}" }
        return namespaces
    }
}
