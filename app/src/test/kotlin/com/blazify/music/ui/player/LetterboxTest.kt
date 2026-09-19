package com.blazify.music.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class LetterboxTest {
    private val black = 0xFF000000.toInt()
    private val grey = 0xFF808080.toInt()

    /** A frame [height] rows tall whose first [top] and last [bottom] rows are black. */
    private fun frame(height: Int, top: Int, bottom: Int, width: Int = 8) =
        IntArray(width * height) { index ->
            val row = index / width
            if (row < top || row >= height - bottom) black else grey
        }

    @Test
    fun `finds bands above and below the picture`() {
        assertEquals(0.12f, letterboxShare(frame(50, 6, 6), 8, 50), 0.001f)
    }

    @Test
    fun `goes by the thinner band`() {
        assertEquals(0.04f, letterboxShare(frame(50, 6, 2), 8, 50), 0.001f)
    }

    @Test
    fun `a picture with no bands has none`() {
        assertEquals(0f, letterboxShare(frame(50, 0, 0), 8, 50), 0f)
    }

    @Test
    fun `a dark scene is not taken for bands`() {
        assertEquals(0f, letterboxShare(frame(50, 20, 20), 8, 50), 0f)
        assertEquals(0f, letterboxShare(frame(50, 25, 25), 8, 50), 0f)
    }

    @Test
    fun `a logo printed in a band leaves it a band`() {
        val pixels = frame(50, 6, 6)
        pixels[3 * 8 + 5] = grey
        pixels[46 * 8 + 1] = grey
        pixels[46 * 8 + 2] = grey
        assertEquals(0.12f, letterboxShare(pixels, 8, 50), 0.001f)
    }

    @Test
    fun `a mostly lit row is picture`() {
        val pixels = frame(50, 6, 6)
        for (x in 0 until 5) pixels[3 * 8 + x] = grey
        assertEquals(0.06f, letterboxShare(pixels, 8, 50), 0.001f)
    }
}
