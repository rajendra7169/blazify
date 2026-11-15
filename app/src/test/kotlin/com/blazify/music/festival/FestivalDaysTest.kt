/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.festival

import com.blazify.music.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FestivalDaysTest {
    private fun on(y: Int, m: Int, d: Int, country: String) = Festival.on(LocalDate.of(y, m, d), country)

    @Test
    fun startsAWeekBeforeAndEndsTwoDaysAfter() {
        assertEquals(null, on(2027, 10, 23, "US"))
        assertEquals(Festival.HALLOWEEN, on(2027, 10, 24, "US"))
        assertEquals(Festival.HALLOWEEN, on(2027, 11, 2, "US"))
        assertEquals(null, on(2027, 11, 3, "US"))
    }

    @Test
    fun christmasThenNewYearWhenTheyOverlap() {
        // Christmas runs Dec 17–28, New Year Dec 24–Jan 3: the nearer one shows.
        assertEquals(Festival.CHRISTMAS, on(2026, 12, 17, "GB"))
        assertEquals(Festival.CHRISTMAS, on(2026, 12, 26, "GB"))
        assertEquals(Festival.CHRISTMAS, on(2026, 12, 27, "GB")) // 1 day after Christmas, 4 before New Year
        assertEquals(Festival.CHRISTMAS, on(2026, 12, 28, "GB")) // 2 after, 3 before
        assertEquals(Festival.NEW_YEAR, on(2026, 12, 29, "GB")) // 3 after, 2 before
        assertEquals(Festival.NEW_YEAR, on(2027, 1, 3, "GB"))
        assertEquals(null, on(2027, 1, 4, "GB"))
    }

    @Test
    fun countriesWithoutChristmasGetTheWholeNewYearWeek() {
        assertEquals(Festival.NEW_YEAR, on(2026, 12, 24, "JP"))
        assertEquals(null, on(2026, 12, 23, "JP"))
    }

    @Test
    fun orthodoxChristmasCountriesGetNewYearInstead() {
        assertEquals(Festival.NEW_YEAR, on(2026, 12, 25, "RU"))
        assertEquals(null, on(2026, 12, 20, "RU"))
        assertEquals(Festival.CHRISTMAS, on(2026, 12, 25, "UA"))
        assertEquals(Festival.CHRISTMAS, on(2026, 12, 20, "NP"))
    }

    @Test
    fun tiharAndDiwaliAreOneFestival() {
        assertEquals(Festival.DIWALI, on(2027, 10, 20, "NP"))
        assertEquals(Festival.DIWALI, on(2027, 10, 20, "IN"))
        assertEquals(R.string.festival_greeting_tihar, Festival.DIWALI.greetingIn("np"))
        assertEquals(R.string.festival_greeting_diwali, Festival.DIWALI.greetingIn("IN"))
    }

    @Test
    fun diwaliWinsOverHalloweenWhenTheyMeet() {
        for (day in 24..31) assertEquals(Festival.DIWALI, on(2027, 10, day, "NP"))
        assertEquals(Festival.DIWALI, on(2027, 11, 2, "IN"))
    }

    @Test
    fun festivalsStayInTheirCountries() {
        assertEquals(null, on(2027, 10, 29, "US").takeIf { it == Festival.DIWALI })
        assertEquals(Festival.HALLOWEEN, on(2028, 10, 31, "NP"))
        assertEquals(Festival.HALLOWEEN, on(2027, 10, 31, "JP"))
        assertEquals(Festival.CHRISTMAS, on(2026, 12, 20, "NP")) // a public holiday in Nepal
    }
}
