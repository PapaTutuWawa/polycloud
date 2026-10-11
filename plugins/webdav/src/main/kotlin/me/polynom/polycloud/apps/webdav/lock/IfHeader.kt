package me.polynom.polycloud.apps.webdav.lock

/** A single `If` header condition: a lock state-token or an entity-tag, optionally negated via `Not`. */
sealed interface IfCondition {
    data class StateToken(
        val token: String,
    ) : IfCondition

    data class EntityTag(
        val etag: String,
    ) : IfCondition
}

data class IfEntry(
    val condition: IfCondition,
    val negated: Boolean,
)

/** One parenthesized `(...)` group. All [entries] must hold (AND) for the list to match. */
data class IfList(
    val resourceTag: String?,
    val entries: List<IfEntry>,
)

/**
 * Parser for the `If` request header (RFC 4918 section 10.4). The header is a sequence of
 * optionally tagged, parenthesized condition lists; a list matches if all its entries match
 * (AND), and the header as a whole matches if every list matches (also AND - see
 * [me.polynom.polycloud.apps.webdav.lock.LockManager.checkIfHeader] for why).
 */
object IfHeader {
    fun parse(header: String?): List<IfList> {
        if (header.isNullOrBlank()) {
            return emptyList()
        }

        val lists = mutableListOf<IfList>()
        var pendingTag: String? = null
        var i = 0
        while (i < header.length) {
            when (header[i]) {
                '<' -> {
                    val end = header.indexOf('>', i)
                    if (end < 0) {
                        break
                    }
                    pendingTag = header.substring(i + 1, end)
                    i = end + 1
                }

                '(' -> {
                    val (entries, next) = parseList(header, i + 1)
                    lists += IfList(pendingTag, entries)
                    pendingTag = null
                    i = next
                }

                else -> {
                    i++
                }
            }
        }
        return lists
    }

    private fun parseList(
        header: String,
        start: Int,
    ): Pair<List<IfEntry>, Int> {
        val entries = mutableListOf<IfEntry>()
        var negated = false
        var i = start
        while (i < header.length && header[i] != ')') {
            when {
                header.startsWith("Not", i) -> {
                    negated = true
                    i += "Not".length
                }

                header[i] == '<' || header[i] == '[' -> {
                    val closing = if (header[i] == '<') '>' else ']'
                    val end = header.indexOf(closing, i)
                    if (end < 0) {
                        break
                    }

                    val raw = header.substring(i + 1, end)
                    // The brackets wrap a complete quoted-string entity-tag (RFC 4918 10.4.1), e.g. `["abc"]` -
                    // the quotes are part of it, matching how etags are stored/compared everywhere else here.
                    val condition = if (closing == '>') IfCondition.StateToken(raw) else IfCondition.EntityTag(raw)
                    entries += IfEntry(condition, negated)
                    negated = false
                    i = end + 1
                }

                else -> {
                    i++
                }
            }
        }
        return entries to (i + 1)
    }
}
