package me.polynom.polycloud.apps.webdav.xml

import me.polynom.polycloud.apps.webdav.lock.LockScope
import me.polynom.polycloud.apps.webdav.store.DavPropertyName
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** What a PROPFIND request body asked for. An empty/missing body means [AllProp]. */
sealed interface PropfindRequest {
    data object AllProp : PropfindRequest

    data object PropName : PropfindRequest

    data class Named(
        val names: List<DavPropertyName>,
    ) : PropfindRequest
}

/** A single PROPPATCH instruction, in document order - RFC 4918 section 9.2 requires processing in order. */
sealed interface ProppatchOperation {
    val name: DavPropertyName

    data class Set(
        override val name: DavPropertyName,
        val value: String,
    ) : ProppatchOperation

    data class Remove(
        override val name: DavPropertyName,
    ) : ProppatchOperation
}

data class ProppatchRequest(
    val operations: List<ProppatchOperation>,
)

/** Parsing of the small subset of WebDAV request XML (RFC 4918) this plugin needs: PROPFIND/PROPPATCH/LOCK bodies. */
object DavXmlParser {
    fun parsePropfind(body: String): PropfindRequest {
        if (body.isBlank()) {
            return PropfindRequest.AllProp
        }

        val root = parseDocument(body)
        return when {
            findChild(root, "allprop") != null -> {
                PropfindRequest.AllProp
            }

            findChild(root, "propname") != null -> {
                PropfindRequest.PropName
            }

            else -> {
                val propElement = findChild(root, "prop")
                if (propElement == null) {
                    PropfindRequest.AllProp
                } else {
                    PropfindRequest.Named(childElements(propElement).map { it.toPropertyName() })
                }
            }
        }
    }

    fun parseProppatch(body: String): ProppatchRequest {
        val root = parseDocument(body)
        val operations = mutableListOf<ProppatchOperation>()

        childElements(root).forEach { instruction ->
            val propElement = findChild(instruction, "prop") ?: return@forEach
            childElements(propElement).forEach { prop ->
                when (instruction.localName) {
                    "set" -> operations += ProppatchOperation.Set(prop.toPropertyName(), prop.textContent)
                    "remove" -> operations += ProppatchOperation.Remove(prop.toPropertyName())
                }
            }
        }

        return ProppatchRequest(operations)
    }

    fun parseLockOwner(body: String): String? {
        if (body.isBlank()) {
            return null
        }

        return findChild(parseDocument(body), "owner")?.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Defaults to exclusive, matching most clients that omit `<lockscope>` entirely. */
    fun parseLockScope(body: String): LockScope {
        if (body.isBlank()) {
            return LockScope.EXCLUSIVE
        }

        val scopeElement = findChild(parseDocument(body), "lockscope")
        val isShared = scopeElement != null && findChild(scopeElement, "shared") != null
        return if (isShared) LockScope.SHARED else LockScope.EXCLUSIVE
    }

    /** An element with no namespace in scope has a null [Element.getNamespaceURI] - that's RFC 4918's "no namespace", not DAV:. */
    private fun Element.toPropertyName(): DavPropertyName = DavPropertyName(namespaceURI ?: "", localName)

    private fun parseDocument(body: String): Element = newDocumentBuilder().parse(ByteArrayInputStream(body.toByteArray())).documentElement

    private fun newDocumentBuilder() =
        DocumentBuilderFactory
            .newInstance()
            .apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            }.newDocumentBuilder()

    private fun findChild(
        parent: Element,
        localName: String,
    ): Element? = childElements(parent).firstOrNull { it.namespaceURI == DAV_NAMESPACE && it.localName == localName }

    private fun childElements(parent: Element): List<Element> {
        val children = parent.childNodes
        return (0 until children.length).mapNotNull { children.item(it) as? Element }
    }
}
