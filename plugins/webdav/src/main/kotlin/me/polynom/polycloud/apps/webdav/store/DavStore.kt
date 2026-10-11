package me.polynom.polycloud.apps.webdav.store

/**
 * Backing storage for the WebDAV resource tree.
 *
 * This is a pure data abstraction: it has no notion of HTTP semantics (status codes, locks, ...).
 * All WebDAV protocol decisions live in the request handling layer, which consults [exists] /
 * [get] before calling the mutating methods below. Implementations may assume their preconditions
 * have already been validated by the caller.
 *
 * The in-memory implementation in this plugin exists solely to let the WebDAV protocol surface be
 * built and tested in isolation. It is expected to be replaced by an implementation backed by the
 * files plugin's FilesService/UploadService, see WebDav.md step 3.
 */
interface DavStore {
    fun exists(path: String): Boolean

    fun get(path: String): DavNode?

    /** Immediate children of the collection at [path], as full normalized paths. */
    fun children(path: String): List<String>

    /**
     * Creates or overwrites the file at [path].
     * @param path parent of [path] must already exist and be a collection.
     */
    fun putFile(
        path: String,
        content: ByteArray,
        contentType: String,
    ): DavFile

    /**
     * Creates an empty collection at [path].
     * @param path parent of [path] must already exist and be a collection, and [path] itself must not exist yet.
     */
    fun makeCollection(path: String): DavCollection

    /** Removes the node at [path]. If it is a collection, removes all of its descendants too. */
    fun delete(path: String)

    /**
     * Copies the node at [source] to [destination] (including all descendants if it is a collection,
     * unless [shallow] is true).
     * @return true if [destination] already existed and was replaced.
     */
    fun copy(
        source: String,
        destination: String,
        shallow: Boolean = false,
    ): Boolean

    /**
     * Moves the node at [source] to [destination] (including all descendants).
     * @return true if [destination] already existed and was replaced.
     */
    fun move(
        source: String,
        destination: String,
    ): Boolean
}
