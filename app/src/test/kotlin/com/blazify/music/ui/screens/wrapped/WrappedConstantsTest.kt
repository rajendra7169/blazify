package com.blazify.music.ui.screens.wrapped

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WrappedConstantsTest {

    @Test
    fun `december looks back on the year that is ending`() {
        assertEquals(2026, WrappedConstants.year(LocalDate.of(2026, 12, 1)))
        assertEquals(2026, WrappedConstants.year(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `every other month looks back on the last full year`() {
        assertEquals(2026, WrappedConstants.year(LocalDate.of(2027, 1, 10)))
        assertEquals(2025, WrappedConstants.year(LocalDate.of(2026, 9, 15)))
        assertEquals(2025, WrappedConstants.year(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun `the card is offered in december and january only`() {
        assertTrue(WrappedConstants.isSeason(LocalDate.of(2026, 12, 5)))
        assertTrue(WrappedConstants.isSeason(LocalDate.of(2027, 1, 31)))
        assertFalse(WrappedConstants.isSeason(LocalDate.of(2027, 2, 1)))
        assertFalse(WrappedConstants.isSeason(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun `a year covers every moment from new year to new year's eve`() {
        assertEquals(LocalDate.of(2026, 1, 1).atStartOfDay(), WrappedConstants.start(2026))
        assertEquals(2026, WrappedConstants.end(2026).year)
        assertEquals(12, WrappedConstants.end(2026).monthValue)
        assertEquals(31, WrappedConstants.end(2026).dayOfMonth)
        assertEquals("Blazify 2026", WrappedConstants.playlistName(2026))
    }
}
