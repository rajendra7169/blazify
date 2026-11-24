package com.blazify.music.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewGateTest {
    private fun entry(
        id: String,
        available: Boolean = true,
    ) = FeatureEntry(
        id = id,
        heading = 0,
        description = 0,
        demo = DemoId.TAP_ICON,
        icon = 0,
        available = { available },
    )

    private val catalog =
        listOf(
            WhatsNewRelease("9.22.0", listOf(entry("future"))),
            WhatsNewRelease("9.21.0", listOf(entry("c1"), entry("c2"))),
            WhatsNewRelease("9.20.1", emptyList()),
            WhatsNewRelease("9.20.0", listOf(entry("b1"))),
            WhatsNewRelease("9.19.0", listOf(entry("a1"), entry("a2"))),
        )

    private fun decide(
        stored: String?,
        current: String,
        freshInstall: Boolean = false,
        onboardingDone: Boolean = true,
        releases: List<WhatsNewRelease> = catalog,
    ) = WhatsNewGate.decide(stored, current, freshInstall, onboardingDone, releases)

    private fun WhatsNewGate.Decision.versions() = show.map { it.version }

    @Test
    fun aFreshInstallShowsNothingAndRemembersTheVersion() {
        val decision = decide(stored = null, current = "9.21.0", freshInstall = true)
        assertTrue(decision.show.isEmpty())
        assertEquals("9.21.0", decision.store)
    }

    @Test
    fun beforeOnboardingIsFinishedNothingShowsEvenAfterAnUpdate() {
        val decision = decide(stored = "9.19.0", current = "9.21.0", onboardingDone = false)
        assertTrue(decision.show.isEmpty())
        assertEquals("9.21.0", decision.store)
    }

    @Test
    fun anUpdateFromABuildWithoutThePageShowsOnlyThisVersion() {
        val decision = decide(stored = null, current = "9.21.0")
        assertEquals(listOf("9.21.0"), decision.versions())
        assertEquals("9.21.0", decision.store)
    }

    @Test
    fun anUpdateShowsEveryVersionSinceTheStoredOneNewestFirst() {
        val decision = decide(stored = "9.19.0", current = "9.21.0")
        assertEquals(listOf("9.21.0", "9.20.0"), decision.versions())
        assertEquals("9.21.0", decision.store)
    }

    @Test
    fun versionsNewerThanTheAppAreNeverShown() {
        val decision = decide(stored = "9.20.0", current = "9.21.0")
        assertEquals(listOf("9.21.0"), decision.versions())
    }

    @Test
    fun theSameVersionShowsNothingAndStoresNothing() {
        val decision = decide(stored = "9.21.0", current = "9.21.0")
        assertTrue(decision.show.isEmpty())
        assertNull(decision.store)
    }

    @Test
    fun aDowngradeShowsNothingAndKeepsTheNewerStoredVersion() {
        val decision = decide(stored = "9.21.0", current = "9.20.0")
        assertTrue(decision.show.isEmpty())
        assertNull(decision.store)
    }

    @Test
    fun aReleaseWithNoEntriesShowsNothingButMovesTheStoredVersionOn() {
        val decision = decide(stored = "9.20.0", current = "9.20.1")
        assertTrue(decision.show.isEmpty())
        assertEquals("9.20.1", decision.store)
    }

    @Test
    fun aVersionMissingFromTheCatalogShowsNothingButIsRemembered() {
        val decision = decide(stored = null, current = "9.20.5")
        assertTrue(decision.show.isEmpty())
        assertEquals("9.20.5", decision.store)
    }

    @Test
    fun entriesThisBuildDoesNotHaveAreLeftOut() {
        val releases = listOf(WhatsNewRelease("9.21.0", listOf(entry("cast", available = false), entry("ok"))))
        val decision = decide(stored = "9.20.0", current = "9.21.0", releases = releases)
        assertEquals(listOf("ok"), decision.show.single().entries.map { it.id })
    }

    @Test
    fun aReleaseWithOnlyUnavailableEntriesDoesNotOpenThePage() {
        val releases = listOf(WhatsNewRelease("9.21.0", listOf(entry("cast", available = false))))
        val decision = decide(stored = "9.20.0", current = "9.21.0", releases = releases)
        assertTrue(decision.show.isEmpty())
        assertEquals("9.21.0", decision.store)
    }

    @Test
    fun aBetaOfTheNextVersionDoesNotShowThatVersionsNews() {
        val decision = decide(stored = "9.20.0", current = "9.21.0-beta.1")
        assertTrue(decision.show.isEmpty())
        assertEquals("9.21.0-beta.1", decision.store)
    }

    @Test
    fun newEntriesKeepTheOrderDropRepeatsAndStopAtTheLimit() {
        val many =
            listOf(
                WhatsNewRelease("2.0.0", (1..4).map { entry("n$it") }),
                WhatsNewRelease("1.0.0", listOf(entry("n1")) + (5..9).map { entry("n$it") }),
            )
        val ids = WhatsNewCatalog.newEntries(many).map { it.id }
        assertEquals(listOf("n1", "n2", "n3", "n4", "n5", "n6"), ids)
        assertEquals(WhatsNewCatalog.MAX_NEW, ids.size)
    }

    @Test
    fun settingsShowsTheNewestReleaseNotNewerThanTheApp() {
        assertEquals(listOf("9.21.0"), WhatsNewCatalog.latestUpTo("9.21.3", catalog).map { it.version })
        assertEquals(listOf("9.20.0"), WhatsNewCatalog.latestUpTo("9.20.1", catalog).map { it.version })
        assertTrue(WhatsNewCatalog.latestUpTo("9.18.0", catalog).isEmpty())
    }

    @Test
    fun theCatalogIsNewestFirstWithUniqueIds() {
        val versions = WhatsNewCatalog.releases.map { it.version }
        assertEquals(versions.sortedWith { a, b -> com.blazify.music.utils.Updater.compareVersions(b, a) }, versions)
        val coreIds = WhatsNewCatalog.coreFeatures.map { it.id }
        assertEquals(coreIds.distinct(), coreIds)
    }
}
