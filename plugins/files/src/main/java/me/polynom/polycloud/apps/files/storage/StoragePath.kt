package me.polynom.polycloud.apps.files.storage

/**
 * Represents a path in a virtual file system separated by `/`.
 */
data class StoragePath(
    val components: List<String>,
    val file: Boolean,
) {
    /**
     * Constructs a StoragePath from a normal path string like "/hello/folder/test.txt"
     * @param path path string.
     */
    constructor(path: String) : this(parsePath(path), !path.endsWith("/"))

    init {
        if (components.isEmpty() && file) {
            throw IllegalArgumentException("An empty path is `/` and therefore cannot be a file")
        }
    }

    /**
     * Splits any path into a left folder path and right path.
     * Edge cases:
     * /folder1/ -> split(1) -> (/, /folder1/)
     * /file1 -> split(1) -> IllegalArgumentException
     * @param index first path component to appear on the right side.
     * @return (folder path, path).
     * @throws IllegalArgumentException if path is a file and index is equal or bigger to the amount of path components.
     */
    fun split(index: Int): Pair<StoragePath, StoragePath> =
        Pair(
            StoragePath(this.components.take(index), false),
            StoragePath(this.components.drop(index), this.file),
        )

    /**
     * Merges this folder path with another file/folder path.
     * @param other storage path to append to this path.
     * @return this + other.
     * @throws IllegalArgumentException if this is a file path.
     */
    fun merge(other: StoragePath): StoragePath {
        if (this.file) {
            throw IllegalArgumentException("Cant merge with left endpoint being a file")
        }

        return StoragePath(components + other.components, other.file)
    }

    /**
     * @return amount of non-file components.
     */
    fun folderDepth(): Int =
        if (file) {
            components.size - 1
        } else {
            components.size
        }

    /**
     * @return the folder path of this path.
     */
    fun folder() = this.split(this.folderDepth()).first

    override fun toString(): String {
        val postfix =
            if (file || components.isEmpty()) {
                ""
            } else {
                "/"
            }

        return components.joinToString("/", prefix = "/", postfix = postfix)
    }

    companion object {
        private fun parsePath(path: String): List<String> {
            if (!path.startsWith("/")) {
                throw IllegalArgumentException("Storage paths must start with a `/`")
            }

            return path
                .split("/")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
}
