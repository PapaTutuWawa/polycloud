package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavNode
import me.polynom.polycloud.apps.webdav.store.DavStore
import me.polynom.polycloud.apps.webdav.xml.DavResponseEntry
import me.polynom.polycloud.apps.webdav.xml.DavXml
import me.polynom.polycloud.apps.webdav.xml.DavXmlParser
import me.polynom.polycloud.apps.webdav.xml.LiveProperties
import me.polynom.polycloud.apps.webdav.xml.PropfindRequest
import me.polynom.polycloud.apps.webdav.xml.ProppatchOperation
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/** Handles PROPFIND and PROPPATCH. */
@Component
class PropertyHandler(
    private val store: DavStore,
    private val lockManager: LockManager,
) {
    fun propfind(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        val node = store.get(path)
        if (node == null) {
            fail(response, HttpStatus.NOT_FOUND.value())
            return
        }

        val propfindRequest = parseXmlBody { DavXmlParser.parsePropfind(readBody(request)) }
        val entries =
            collectTargets(path, node, request.getHeader(DavHeaders.DEPTH))
                .map { (targetPath, targetNode) -> resolveProps(targetPath, targetNode, propfindRequest) }

        writeXml(response, HttpStatus.MULTI_STATUS.value(), DavXml.buildMultiStatus(entries))
    }

    fun proppatch(
        request: HttpServletRequest,
        response: HttpServletResponse,
        path: String,
    ) {
        val node = store.get(path)
        if (node == null) {
            fail(response, HttpStatus.NOT_FOUND.value())
            return
        }
        checkPrecondition(lockManager, request, path, node.etag)

        val proppatchRequest = parseXmlBody { DavXmlParser.parseProppatch(readBody(request)) }
        proppatchRequest.operations.forEach { operation ->
            when (operation) {
                is ProppatchOperation.Set -> node.deadProperties[operation.name] = operation.value
                is ProppatchOperation.Remove -> node.deadProperties.remove(operation.name)
            }
        }

        val changedNames = proppatchRequest.operations.map { it.name }.distinct()
        val entry = DavResponseEntry(href = hrefFor(path, node), foundProps = changedNames.associateWith { "" })
        writeXml(response, HttpStatus.MULTI_STATUS.value(), DavXml.buildMultiStatus(listOf(entry)))
    }

    private fun collectTargets(
        path: String,
        node: DavNode,
        depthHeader: String?,
    ): List<Pair<String, DavNode>> {
        val result = mutableListOf(path to node)
        if (node is DavCollection) {
            when (depthHeader) {
                "0" -> Unit
                "1" -> result += directChildren(path)
                else -> collectDescendants(path, result)
            }
        }
        return result
    }

    private fun directChildren(path: String): List<Pair<String, DavNode>> =
        store.children(path).mapNotNull { childPath -> store.get(childPath)?.let { childPath to it } }

    private fun collectDescendants(
        path: String,
        acc: MutableList<Pair<String, DavNode>>,
    ) {
        directChildren(path).forEach { (childPath, childNode) ->
            acc += childPath to childNode
            if (childNode is DavCollection) {
                collectDescendants(childPath, acc)
            }
        }
    }

    private fun resolveProps(
        path: String,
        node: DavNode,
        propfindRequest: PropfindRequest,
    ): DavResponseEntry {
        val all = node.deadProperties.mapValues { DavXml.escapeXml(it.value) } + LiveProperties.render(path, node)
        val href = hrefFor(path, node)

        return when (propfindRequest) {
            PropfindRequest.AllProp -> {
                DavResponseEntry(href, foundProps = all)
            }

            PropfindRequest.PropName -> {
                DavResponseEntry(href, foundProps = all.mapValues { "" })
            }

            is PropfindRequest.Named -> {
                val found = propfindRequest.names.mapNotNull { name -> all[name]?.let { name to it } }.toMap()
                val missing = propfindRequest.names.filterNot { all.containsKey(it) }
                DavResponseEntry(href, foundProps = found, missingProps = missing)
            }
        }
    }
}
