package me.polynom.polycloud.apps.webdav.store

import org.springframework.core.io.ByteArrayResource
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, in-memory [DavStore]. Entirely self-contained: nothing here talks to the files
 * plugin. Not a Spring bean - [me.polynom.polycloud.apps.webdav.store.FilesServiceDavStore] is
 * the one registered against the real app; this is kept around for the webdav plugin's own
 * fast, isolated protocol-surface tests.
 */
class InMemoryDavStore : DavStore {
    private val nodes = ConcurrentHashMap<String, DavNode>()

    init {
        nodes[DavPath.ROOT] = DavCollection()
    }

    override fun exists(path: String): Boolean = nodes.containsKey(path)

    override fun get(path: String): DavNode? = nodes[path]

    override fun children(path: String): List<String> {
        if (nodes[path] !is DavCollection) {
            return emptyList()
        }

        return nodes.keys
            .filter { DavPath.parent(it) == path && it != path }
            .sorted()
    }

    override fun putFile(
        path: String,
        content: ByteArray,
        contentType: String,
    ): DavFile {
        val file = DavFile(resource = ByteArrayResource(content), contentType = contentType)
        nodes[path] = file
        return file
    }

    override fun makeCollection(path: String): DavCollection {
        val collection = DavCollection()
        nodes[path] = collection
        return collection
    }

    override fun delete(path: String) {
        descendantsOf(path).forEach { nodes.remove(it) }
        nodes.remove(path)
    }

    override fun copy(
        source: String,
        destination: String,
        shallow: Boolean,
    ): Boolean {
        val existed = nodes.containsKey(destination)
        val sourceNode = nodes[source] ?: return existed

        if (existed) {
            delete(destination)
        }
        nodes[destination] = copyNode(sourceNode)
        if (sourceNode is DavCollection && !shallow) {
            descendantsOf(source).forEach { descendant ->
                val relativeDestination = DavPath.normalize(destination + descendant.removePrefix(source))
                nodes[relativeDestination] = copyNode(nodes.getValue(descendant))
            }
        }

        return existed
    }

    override fun move(
        source: String,
        destination: String,
    ): Boolean {
        val existed = copy(source, destination, shallow = false)
        delete(source)
        return existed
    }

    private fun descendantsOf(path: String): List<String> = nodes.keys.filter { DavPath.isAncestorOf(path, it) }

    private fun copyNode(node: DavNode): DavNode =
        when (node) {
            is DavCollection -> node.copy(deadProperties = node.deadProperties.toMutableMap())
            is DavFile -> node.copy(deadProperties = node.deadProperties.toMutableMap())
        }
}
