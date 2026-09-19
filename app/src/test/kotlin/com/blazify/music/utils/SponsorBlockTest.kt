package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockTest {
    private val all = SponsorBlock.Category.entries.toSet()

    @Test
    fun `asks by four characters of the hash, never the id`() {
        val prefix = SponsorBlock.hashPrefix("kJQP7kiw5Fk")

        assertEquals("f1d9", prefix)
        assertEquals(4, prefix.length)
    }

    @Test
    fun `picks out the one video asked about`() {
        val segments = SponsorBlock.parse(BODY, "kJQP7kiw5Fk", all)

        assertEquals(2, segments.size)
        assertEquals(0L, segments[0].startMs)
        assertEquals(21808L, segments[0].endMs)
        assertEquals(SponsorBlock.Category.NON_MUSIC, segments[0].category)
        assertEquals(SponsorBlock.Category.SPONSOR, segments[1].category)
    }

    @Test
    fun `ignores the other videos in the answer`() {
        assertTrue(SponsorBlock.parse(BODY, "somethingelse", all).isEmpty())
    }

    @Test
    fun `leaves out what the community voted down, and what was not asked for`() {
        val body =
            """
            [{"videoID":"abc","segments":[
              {"category":"music_offtopic","actionType":"skip","segment":[0,10],"votes":-3},
              {"category":"sponsor","actionType":"skip","segment":[20,30],"votes":5},
              {"category":"music_offtopic","actionType":"mute","segment":[40,50],"votes":9},
              {"category":"music_offtopic","actionType":"skip","segment":[70,60],"votes":2}
            ]}]
            """.trimIndent()

        val segments = SponsorBlock.parse(body, "abc", all)

        assertEquals(1, segments.size)
        assertEquals(SponsorBlock.Category.SPONSOR, segments[0].category)
        assertTrue(SponsorBlock.parse(body, "abc", setOf(SponsorBlock.Category.INTRO)).isEmpty())
    }

    @Test
    fun `knows when the player is inside a segment`() {
        val segments = SponsorBlock.parse(BODY, "kJQP7kiw5Fk", all)

        assertEquals(segments[0], SponsorBlock.segmentAt(segments, 0))
        assertEquals(segments[0], SponsorBlock.segmentAt(segments, 15_000))
        assertNull(SponsorBlock.segmentAt(segments, 30_000))
        // Right at the end there is nothing worth jumping for.
        assertNull(SponsorBlock.segmentAt(segments, 21_500))
    }

    @Test
    fun `survives an answer it cannot read`() {
        assertTrue(SponsorBlock.parse("not json", "abc", all).isEmpty())
        assertTrue(SponsorBlock.parse("[]", "abc", all).isEmpty())
    }

    private companion object {
        const val BODY =
            """
            [
              {"videoID":"kJQP7kiw5Fk","segments":[
                {"category":"music_offtopic","actionType":"skip","segment":[0,21.808434],"votes":41},
                {"category":"sponsor","actionType":"skip","segment":[120,135.5],"votes":3}
              ]},
              {"videoID":"otherVideo","segments":[
                {"category":"intro","actionType":"skip","segment":[0,5],"votes":1}
              ]}
            ]
            """
    }
}
