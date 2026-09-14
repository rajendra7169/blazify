package com.blazify.music.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorTest {

    @Test
    fun `the site's age wording is recognised`() {
        assertTrue(isAgeRestrictedMessage("Sign in to confirm your age"))
        assertTrue(isAgeRestrictedMessage("This video is age-restricted"))
        assertTrue(isAgeRestrictedMessage("playabilityStatus: LOGIN_REQUIRED"))
    }

    @Test
    fun `ordinary failures are not blamed on age restrictions`() {
        assertFalse(isAgeRestrictedMessage("The page needs to be reloaded"))
        assertFalse(isAgeRestrictedMessage("Response code: 403"))
        assertFalse(isAgeRestrictedMessage("Unable to load image"))
        assertFalse(isAgeRestrictedMessage("Unable to connect to rr3---sn-abc.googlevideo.com"))
    }
}
