package me.polynom.polycloud.apps.webdav.xml

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

data class ProppatchRequest(
    val sets: List<Pair<DavPropertyName, String>>,
    val removes: List<DavPropertyName>,
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
        val sets = mutableListOf<Pair<DavPropertyName, String>>()
        val removes = mutableListOf<DavPropertyName>()

        childElements(root).forEach { instruction ->
            val propElement = findChild(instruction, "prop") ?: return@forEach
            childElements(propElement).forEach { prop ->
                if (instruction.localName == "set") {
                    sets += prop.toPropertyName() to prop.textContent
                } else if (instruction.localName == "remove") {
                    removes += prop.toPropertyName()
                }
            }
        }

        return ProppatchRequest(sets, removes)
    }

    fun parseLockOwner(body: String): String? {
        if (body.isBlank()) {
            return null
        }

        return findChild(parseDocument(body), "owner")?.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun Element.toPropertyName(): DavPropertyName = DavPropertyName(namespaceURI ?: DAV_NAMESPACE, localName)

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
