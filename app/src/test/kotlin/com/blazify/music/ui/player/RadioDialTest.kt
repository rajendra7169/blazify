package com.blazify.music.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class RadioDialTest {
    private val width = 1000f

    @Test
    fun theScaleStartsAtTheFirstMarkNotAtTheGlassEdge() {
        assertEquals(0f, dialFraction(0f, width), 0f)
        assertEquals(0f, dialFraction(85f, width), 0.001f)
    }

    @Test
    fun theScaleEndsAtTheLastMark() {
        assertEquals(1f, dialFraction(915f, width), 0.001f)
        assertEquals(1f, dialFraction(width, width), 0f)
    }

    @Test
    fun theMiddleOfTheGlassIsTheMiddleOfTheSong() {
        assertEquals(0.5f, dialFraction(500f, width), 0.001f)
    }

    @Test
    fun aTouchOutsideTheStripStaysOnTheScale() {
        assertEquals(0f, dialFraction(-40f, width), 0f)
        assertEquals(1f, dialFraction(1200f, width), 0f)
    }

    @Test
    fun aStripWithNoWidthYetGivesTheStart() {
        assertEquals(0f, dialFraction(10f, 0f), 0f)
    }
}
