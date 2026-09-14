package me.polynom.polycloud.apps.files.storage

class StoragePath(val components: List<String>, val file: Boolean) {

    constructor(path: String) : this(parsePath(path), !path.endsWith("/"))

    fun split(index: Int): Pair<StoragePath, StoragePath> = Pair(
        StoragePath(this.components.take(index), false),
        StoragePath(this.components.drop(index), this.file)
    )

    fun merge(other: StoragePath): StoragePath {
        if (this.file) {
            throw IllegalArgumentException("Cant merge with left endpoint being a file")
        }

        return StoragePath(components + other.components, other.file)
    }

    fun folderDepth(): Int = if (file) {
        components.size - 1
    } else {
        components.size
    }

    override fun toString(): String {
        val postfix = if (file || components.size == 0) {
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

            return path.split("/")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
}