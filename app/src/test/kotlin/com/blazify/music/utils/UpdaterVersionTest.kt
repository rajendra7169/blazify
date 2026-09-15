package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterVersionTest {
    private fun release(tag: String, prerelease: Boolean = false) =
        ReleaseInfo(
            tagName = tag,
            versionName = tag.removePrefix("v"),
            description = "",
            releaseDate = "",
            assets = emptyList(),
            isPrerelease = prerelease,
        )

    @Test
    fun plainVersionsCompareNumberByNumber() {
        assertEquals(1, Updater.compareVersions("9.14.0", "9.13.3"))
        assertEquals(-1, Updater.compareVersions("9.9.9", "9.10.0"))
        assertEquals(0, Updater.compareVersions("v9.14.0", "9.14.0"))
        assertEquals(0, Updater.compareVersions("9.14", "9.14.0"))
    }

    @Test
    fun aBetaSortsBetweenTheReleaseBeforeItAndTheOneItLeadsTo() {
        assertEquals(1, Updater.compareVersions("9.14.1-beta.1", "9.14.0"))
        assertEquals(-1, Updater.compareVersions("9.14.1-beta.1", "9.14.1"))
        assertEquals(1, Updater.compareVersions("9.14.1", "9.14.1-beta.3"))
    }

    @Test
    fun laterBetasAreNewerEvenPastNine() {
        assertEquals(1, Updater.compareVersions("9.14.1-beta.2", "9.14.1-beta.1"))
        assertEquals(1, Updater.compareVersions("9.14.1-beta.10", "9.14.1-beta.9"))
        assertEquals(0, Updater.compareVersions("v9.14.1-beta.2", "9.14.1-beta.2"))
    }

    @Test
    fun aBetaTesterIsOfferedTheFinishedRelease() {
        assertTrue(Updater.isUpdateAvailable(currentVersion = "9.14.1-beta.2", latestVersion = "9.14.1"))
        assertTrue(!Updater.isUpdateAvailable(currentVersion = "9.14.1", latestVersion = "9.14.1-beta.2"))
    }

    @Test
    fun stableUsersNeverGetABetaFromTheList() {
        val releases = listOf(release("v9.14.0"), release("v9.14.1-beta.1", prerelease = true), release("v9.13.3"))

        assertEquals("v9.14.0", Updater.newestRelease(releases, includeBetas = false)?.tagName)
        assertEquals("v9.14.1-beta.1", Updater.newestRelease(releases, includeBetas = true)?.tagName)
    }

    @Test
    fun betaTestersStillGetAStableReleaseThatIsNewer() {
        val releases = listOf(release("v9.14.1-beta.2", prerelease = true), release("v9.14.1"))

        assertEquals("v9.14.1", Updater.newestRelease(releases, includeBetas = true)?.tagName)
    }

    @Test
    fun nothingToOfferFromAListOfOnlyBetasOnStable() {
        assertNull(Updater.newestRelease(listOf(release("v9.15.0-beta.1", prerelease = true)), includeBetas = false))
    }
}
