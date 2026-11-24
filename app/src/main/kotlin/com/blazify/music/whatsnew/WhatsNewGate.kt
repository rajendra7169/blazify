/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.whatsnew

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.blazify.music.BuildConfig
import com.blazify.music.constants.OnboardingCompletedKey
import com.blazify.music.constants.WhatsNewSeenVersionKey
import com.blazify.music.utils.Updater
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Decides whether the What's new page opens after an update, and for which versions. */
object WhatsNewGate {
    /**
     * [show] is empty when the page stays closed. [store] is the version to remember as seen,
     * or null to leave what is stored as it is.
     */
    data class Decision(
        val show: List<WhatsNewRelease>,
        val store: String?,
    )

    /**
     * @param stored the version the page was last shown or skipped for, null when never.
     * @param freshInstall the app has never been updated: first install and last update are
     *   the same moment.
     */
    fun decide(
        stored: String?,
        current: String,
        freshInstall: Boolean,
        onboardingDone: Boolean,
        releases: List<WhatsNewRelease>,
    ): Decision {
        // Someone new to the app gets the introduction, not a list of changes.
        if (!onboardingDone || (stored == null && freshInstall)) return Decision(emptyList(), current)

        // Same version, or an older one installed over a newer one: nothing new to tell.
        if (stored != null && Updater.compareVersions(stored, current) >= 0) return Decision(emptyList(), null)

        val due =
            releases
                .filter { release ->
                    val notNewerThanApp = Updater.compareVersions(release.version, current) <= 0
                    // Updated from a build that did not have this page: only this version's news.
                    val unseen =
                        if (stored == null) {
                            Updater.compareVersions(release.version, current) == 0
                        } else {
                            Updater.compareVersions(release.version, stored) > 0
                        }
                    notNewerThanApp && unseen
                }.map { release -> release.copy(entries = release.entries.filter { it.available() }) }
                .filter { it.entries.isNotEmpty() }

        // Either way this version now counts as seen, so a bug-fix release with nothing to show
        // still moves the stored version forward.
        return Decision(due, current)
    }

    /**
     * Reads what [decide] needs off the main thread, stores the version at once (so a crash or
     * a killed app cannot bring the page back forever) and returns what to show.
     */
    suspend fun check(context: Context): List<WhatsNewRelease> =
        withContext(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            val decision =
                decide(
                    stored = prefs[WhatsNewSeenVersionKey],
                    current = BuildConfig.VERSION_NAME,
                    freshInstall = isFreshInstall(context),
                    onboardingDone = prefs[OnboardingCompletedKey] ?: false,
                    releases = WhatsNewCatalog.releases,
                )
            decision.store?.let { version -> context.safeDataStoreEdit { it[WhatsNewSeenVersionKey] = version } }
            decision.show
        }

    private fun isFreshInstall(context: Context): Boolean =
        runCatching {
            val packageManager = context.packageManager
            val info =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(context.packageName, 0)
                }
            info.firstInstallTime == info.lastUpdateTime
        }.getOrDefault(false)
}
