/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.blazify.music.BuildConfig
import com.blazify.music.constants.StarPromptDoneKey
import com.blazify.music.constants.StarPromptShownKey
import com.blazify.music.constants.StarPromptVersionKey
import com.blazify.music.constants.StarPromptVersionSinceKey
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Asking for a star, twice per version at most: 10 days after a version is first opened, and
 * once more a month after it. A new version starts the count again.
 *
 * - The clock starts when the version is first opened, so it never asks the day of an update,
 *   and an app opened once and left never asks at all.
 * - "Star it" and "Already starred" end it for good, in this version and every later one; the
 *   button in About stays for anyone who wants it. Tapping outside only waits for the next ask.
 * - Never shown over music: the caller only asks when nothing is playing.
 * - Reopening the app many times in a day asks once: an ask counts only once it is shown.
 */
object StarPrompt {
    const val REPO = "https://github.com/rajendra7169/blazify"

    /** Days after a version is first opened for each of its asks. */
    val ASK_DAYS = listOf(10L, 30L)

    /** Test copies built with -PforceStarPrompt=true ask on every launch. */
    private val forced get() = BuildConfig.FORCE_STAR_PROMPT

    /** Whether an ask is due: [shown] asks of this version so far, the version opened at [since]. */
    fun isDue(now: Long, since: Long, shown: Int): Boolean {
        val days = ASK_DAYS.getOrNull(shown) ?: return false
        return now >= since + TimeUnit.DAYS.toMillis(days)
    }

    /** Records the launch and says whether to ask now. */
    suspend fun onOpened(context: Context, version: Int = BuildConfig.VERSION_CODE): Boolean {
        if (forced) return true
        val prefs = context.dataStore.data.first()
        if (prefs[StarPromptDoneKey] == true) return false
        val now = System.currentTimeMillis()
        if (prefs[StarPromptVersionKey] != version) {
            // A new version, or the first launch: start its clock, ask nothing yet.
            context.dataStore.edit {
                it[StarPromptVersionKey] = version
                it[StarPromptVersionSinceKey] = now
                it[StarPromptShownKey] = 0
            }
            return false
        }
        return isDue(now, prefs[StarPromptVersionSinceKey] ?: now, prefs[StarPromptShownKey] ?: 0)
    }

    /** Shown once: this version moves on to its next ask, or has none left. */
    suspend fun onShown(context: Context) {
        if (forced) return
        val shown = (context.dataStore.data.first()[StarPromptShownKey] ?: 0) + 1
        context.dataStore.edit { it[StarPromptShownKey] = shown }
    }

    /** Starred, or already starred. Either way it never asks again. */
    suspend fun stop(context: Context) {
        if (forced) return
        context.dataStore.edit { it[StarPromptDoneKey] = true }
    }
}
