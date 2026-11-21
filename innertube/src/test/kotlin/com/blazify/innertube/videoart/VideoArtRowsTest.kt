package com.blazify.innertube.videoart

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rows read out of recorded search answers ("Dynamite BTS", 2026-11-22, trimmed). */
class VideoArtRowsTest {
    private fun answer(name: String): JsonElement {
        val text =
            javaClass.classLoader!!
                .getResourceAsStream("videoart/$name")!!
                .bufferedReader()
                .use { it.readText() }
        return Json.parseToJsonElement(text)
    }

    @Test
    fun musicRows() {
        val rows = VideoArtRows.music(answer("music_dynamite.json"))
        assertEquals(7, rows.size)
        assertEquals(
            MusicVideoRow("gdZLi9oWNZg", "Dynamite", "BTS", "MUSIC_VIDEO_TYPE_OMV", 2_100_000_000L, 224),
            rows[0],
        )
        assertEquals("KBS WORLD TV", rows[1].artist)
        assertEquals("MUSIC_VIDEO_TYPE_UGC", rows[1].musicVideoType)
        assertEquals(11_000_000L, rows[1].views)
        assertEquals("MUSIC_VIDEO_TYPE_OFFICIAL_SOURCE_MUSIC", rows[2].musicVideoType)
        assertEquals(44, rows[2].length)
        assertEquals("YW7JVCiDgJk", rows[6].videoId)
        assertEquals("BTS (방탄소년단) 'Dynamite' @ FNS MUSIC FESTIVAL", rows[6].title)
    }

    @Test
    fun webRows() {
        val rows = VideoArtRows.web(answer("web_dynamite.json"))
        assertEquals(6, rows.size)
        assertEquals(
            WebVideoRow("gdZLi9oWNZg", "BTS (방탄소년단) 'Dynamite' Official MV", "HYBE LABELS", false, true, 2_169_464_543L, 224),
            rows[0],
        )
        assertFalse(rows[1].verified)
        assertEquals("7clouds K-pop", rows[1].channel)
        // BANGTANTV is BTS's own channel: the artist badge, which is a tick as well.
        assertTrue(rows[2].artistBadge)
        assertTrue(rows[2].verified)
        assertEquals(202, rows[2].length)
        assertEquals("FIFA and BANGTANTV", rows[4].channel)
    }

    @Test
    fun rowsAreFoundWhereverTheyAre() {
        val answer =
            Json.parseToJsonElement(
                """
                {"a":[{"shelf":{"items":[{"videoRenderer":{"videoId":"aaaaaaaaaaa","title":{"runs":[{"text":"One"}]},
                 "ownerText":{"runs":[{"text":"Chan"},{"text":"nel"}]}}}]}},
                 {"videoRenderer":{"title":{"runs":[{"text":"No id"}]}}},
                 {"videoRenderer":{"videoId":"bbbbbbbbbbb","lengthText":{"simpleText":"1:02:03"},
                  "viewCountText":{"simpleText":"No views"}}}]}
                """.trimIndent(),
            )
        val rows = VideoArtRows.web(answer)
        assertEquals(listOf("aaaaaaaaaaa", "bbbbbbbbbbb"), rows.map { it.videoId })
        assertEquals("Channel", rows[0].channel)
        assertEquals(0, rows[0].length)
        assertEquals(3723, rows[1].length)
        assertEquals(0L, rows[1].views)
        assertEquals("", rows[1].title)
    }

    @Test
    fun aMusicRowWithoutAVideoIsSkipped() {
        val answer =
            Json.parseToJsonElement(
                """
                {"contents":[{"musicResponsiveListItemRenderer":{"flexColumns":[]}},
                 {"musicResponsiveListItemRenderer":{"playlistItemData":{"videoId":"ccccccccccc"},"flexColumns":[
                  {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Title"}]}}}]}}]}
                """.trimIndent(),
            )
        val rows = VideoArtRows.music(answer)
        assertEquals(1, rows.size)
        assertEquals("ccccccccccc", rows[0].videoId)
        assertEquals("", rows[0].artist)
        assertNull(rows[0].musicVideoType)
        assertEquals(0, rows[0].length)
    }
}
