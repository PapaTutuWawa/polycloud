package me.polynom.polycloud.apps.webdav.lock

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IfHeaderTest {
    @Test
    fun test_parse_nullOrBlank_isEmpty() {
        assertEquals(emptyList(), IfHeader.parse(null))
        assertEquals(emptyList(), IfHeader.parse(""))
    }

    @Test
    fun test_parse_singleUntaggedToken() {
        val lists = IfHeader.parse("(<urn:uuid:abc>)")

        assertEquals(1, lists.size)
        assertEquals(null, lists[0].resourceTag)
        assertEquals(listOf(IfEntry(IfCondition.StateToken("urn:uuid:abc"), negated = false)), lists[0].entries)
    }

    @Test
    fun test_parse_multipleLists_areSeparateOrCandidates() {
        val lists = IfHeader.parse("(<urn:uuid:abc>) (<urn:uuid:def>)")

        assertEquals(2, lists.size)
        assertEquals(IfCondition.StateToken("urn:uuid:abc"), lists[0].entries.single().condition)
        assertEquals(IfCondition.StateToken("urn:uuid:def"), lists[1].entries.single().condition)
    }

    @Test
    fun test_parse_notNegatesTheFollowingCondition() {
        val lists = IfHeader.parse("(Not <urn:uuid:abc>)")

        assertEquals(listOf(IfEntry(IfCondition.StateToken("urn:uuid:abc"), negated = true)), lists[0].entries)
    }

    @Test
    fun test_parse_notOnlyAppliesToTheImmediatelyFollowingCondition() {
        val lists = IfHeader.parse("(Not <urn:uuid:abc> <urn:uuid:def>)")

        assertEquals(
            listOf(
                IfEntry(IfCondition.StateToken("urn:uuid:abc"), negated = true),
                IfEntry(IfCondition.StateToken("urn:uuid:def"), negated = false),
            ),
            lists[0].entries,
        )
    }

    @Test
    fun test_parse_entityTagCondition_keepsTheQuotesAsPartOfTheEtag() {
        // The brackets wrap a complete quoted-string etag (RFC 4918 10.4.1) - the quotes are
        // part of the value, matching how etags are represented everywhere else in this plugin.
        val lists = IfHeader.parse("([\"abc123\"])")

        assertEquals(IfCondition.EntityTag("\"abc123\""), lists[0].entries.single().condition)
    }

    @Test
    fun test_parse_mixedTokenAndEntityTagInOneList() {
        val lists = IfHeader.parse("(<urn:uuid:abc> [\"etag\"])")

        assertEquals(2, lists[0].entries.size)
        assertEquals(IfCondition.StateToken("urn:uuid:abc"), lists[0].entries[0].condition)
        assertEquals(IfCondition.EntityTag("\"etag\""), lists[0].entries[1].condition)
    }

    @Test
    fun test_parse_taggedList_capturesResourceTag() {
        val lists = IfHeader.parse("<http://example.com/res> (<urn:uuid:abc>)")

        assertEquals("http://example.com/res", lists[0].resourceTag)
    }

    @Test
    fun test_parse_untaggedListAfterTaggedOne_hasNoTag() {
        val lists = IfHeader.parse("<http://example.com/res> (<urn:uuid:abc>) (<urn:uuid:def>)")

        assertEquals("http://example.com/res", lists[0].resourceTag)
        assertEquals(null, lists[1].resourceTag)
    }

    @Test
    fun test_parse_corruptToken_doesNotCrashAndYieldsNoEntriesForThatList() {
        val lists = IfHeader.parse("(<urn:uuid:unterminated")

        assertTrue(lists.isNotEmpty())
        assertTrue(lists[0].entries.isEmpty())
    }
}
