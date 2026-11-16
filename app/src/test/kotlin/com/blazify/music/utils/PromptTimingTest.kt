package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PromptTimingTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val t0 = 1_800_000_000_000L

    @Test
    fun updateShowsOnceThenOnceMoreAWeekAfterLater() {
        // Never put off: shows.
        assertTrue(UpdatePrompt.shouldOffer("v9.20.0", null, null, null, t0))
        // Put off today: not tomorrow, not on day 6.
        assertFalse(UpdatePrompt.shouldOffer("v9.20.0", "v9.20.0", t0, null, t0 + day))
        assertFalse(UpdatePrompt.shouldOffer("v9.20.0", "v9.20.0", t0, null, t0 + 6 * day + day / 2))
        // A week on: once more.
        assertTrue(UpdatePrompt.shouldOffer("v9.20.0", "v9.20.0", t0, null, t0 + 7 * day))
        assertTrue(UpdatePrompt.isReminder("v9.20.0", "v9.20.0"))
        // Reminded: never again for this version.
        assertFalse(UpdatePrompt.shouldOffer("v9.20.0", "v9.20.0", t0, "v9.20.0", t0 + 60 * day))
        // A newer version starts over.
        assertTrue(UpdatePrompt.shouldOffer("v9.21.0", "v9.20.0", t0, "v9.20.0", t0 + 60 * day))
        assertFalse(UpdatePrompt.isReminder("v9.21.0", "v9.20.0"))
    }

    @Test
    fun aVersionPutOffBeforeThisRuleNeverComesBack() {
        // Declined in 9.19.0, which kept no date.
        assertFalse(UpdatePrompt.shouldOffer("v9.19.0", "v9.19.0", null, null, t0 + 30 * day))
    }

    @Test
    fun notificationOncePerVersion() {
        assertTrue(UpdatePrompt.shouldNotify("v9.20.0", null))
        assertFalse(UpdatePrompt.shouldNotify("v9.20.0", "v9.20.0"))
        assertTrue(UpdatePrompt.shouldNotify("v9.21.0", "v9.20.0"))
    }

    @Test
    fun starAsksTenDaysThenAMonthAfterTheVersionOpenedThenStops() {
        assertFalse(StarPrompt.isDue(t0 + 9 * day, t0, 0))
        assertTrue(StarPrompt.isDue(t0 + 10 * day, t0, 0))
        assertFalse(StarPrompt.isDue(t0 + 29 * day, t0, 1))
        assertTrue(StarPrompt.isDue(t0 + 30 * day, t0, 1))
        assertFalse(StarPrompt.isDue(t0 + 365 * day, t0, 2))
        assertEquals(listOf(10L, 30L), StarPrompt.ASK_DAYS)
    }
}
