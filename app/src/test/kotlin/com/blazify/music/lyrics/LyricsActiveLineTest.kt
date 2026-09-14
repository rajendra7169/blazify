package com.blazify.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsActiveLineTest {

    private fun sungLine(startMs: Long, lastWordEndSec: Double) =
        LyricsEntry(
            time = startMs,
            text = "la",
            words = listOf(WordTimestamp("la", startMs / 1000.0, lastWordEndSec, hasTrailingSpace = false)),
        )

    @Test
    fun `a line stays lit through a short breath before the next line`() {
        val lines = listOf(sungLine(1_000, 2.0), sungLine(2_800, 3.5))

        assertEquals(setOf(0), LyricsUtils.findActiveLineIndices(lines, 2_400))
        assertEquals(setOf(1), LyricsUtils.findActiveLineIndices(lines, 2_900))
    }

    @Test
    fun `a real break leaves nothing lit so the interval marker can show`() {
        val lines = listOf(sungLine(1_000, 2.0), sungLine(9_000, 10.0))

        assertEquals(emptySet<Int>(), LyricsUtils.findActiveLineIndices(lines, 4_000))
    }

    @Test
    fun `lines without word timings still run until the next line starts`() {
        val lines = listOf(LyricsEntry(1_000, "a"), LyricsEntry(5_000, "b"))

        assertEquals(setOf(0), LyricsUtils.findActiveLineIndices(lines, 4_000))
    }
}
