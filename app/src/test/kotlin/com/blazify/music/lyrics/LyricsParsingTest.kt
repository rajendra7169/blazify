package com.blazify.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lyrics that never follow the song usually come down to one of these: a timestamp written
 * slightly differently from `[mm:ss.xx]`, an `[offset:]` tag, or plain lyrics that happen to
 * start with a section header in brackets and get mistaken for synced ones.
 */
class LyricsParsingTest {

    private fun times(lyrics: String) = LyricsUtils.parseLyrics(lyrics).map { it.time }

    @Test
    fun `the usual timestamps still parse exactly`() {
        assertEquals(listOf(9_860L, 12_170L), times("[00:09.86]The club\n[00:12.170]So the bar"))
    }

    @Test
    fun `timestamps written a little differently are still read`() {
        val lyrics = """
            [0:12.34]one digit minute
            [00:13]no fraction
            [00:14.5]one digit fraction
            [01:02:30]colon before the fraction
        """.trimIndent()

        assertEquals(listOf(12_340L, 13_000L, 14_500L, 62_300L), times(lyrics))
    }

    @Test
    fun `a line with two timestamps appears at both`() {
        assertEquals(listOf(1_000L, 5_000L), times("[00:01.00][00:05.00]Oh I"))
    }

    @Test
    fun `a positive offset tag moves every line earlier`() {
        assertEquals(listOf(9_500L, 11_500L), times("[offset:+500]\n[00:10.00]a\n[00:12.00]b"))
    }

    @Test
    fun `a negative offset tag moves every line later, and nothing goes below zero`() {
        assertEquals(listOf(10_250L), times("[offset:-250]\n[00:10.00]a"))
        assertEquals(listOf(0L), times("[offset:2000]\n[00:01.00]a"))
    }

    @Test
    fun `word timings move with the offset too`() {
        val entry = LyricsUtils.parseLyrics("[offset:+500]\n[00:10.00]Hello\n<Hello:10.0:10.4>").single()

        assertEquals(9_500L, entry.time)
        assertEquals(9.5, entry.words!!.single().startTime, 0.0001)
        assertEquals(9.9, entry.words!!.single().endTime, 0.0001)
    }

    @Test
    fun `plain lyrics that open with a section header are not synced`() {
        val plain = "[Verse 1]\nThe club isn't the best place to find a lover\n[Chorus]\nI'm in love"

        assertFalse(lyricsTextLooksSynced(plain))
        assertTrue(LyricsUtils.parseLyrics(plain).isEmpty())
    }

    @Test
    fun `synced lyrics are recognised whatever comes before the first timestamp`() {
        assertTrue(lyricsTextLooksSynced("[00:09.86]The club"))
        assertTrue(lyricsTextLooksSynced("﻿\n[ar:Ed Sheeran]\n[ti:Shape of You]\n[0:09]The club"))
        assertFalse(lyricsTextLooksSynced("The club isn't the best place"))
    }
}
