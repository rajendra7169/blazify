/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.blazify.music.constants.StarPromptDoneKey
import com.blazify.music.constants.StarPromptFirstSeenKey
import com.blazify.music.constants.StarPromptLastShownKey
import com.blazify.music.constants.StarPromptShownKey
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Asking for a star, on a fixed schedule: 3 days, a week, 15 days and a month after the
 * app was first opened, and after that every two months.
 *
 * - The clock starts on first launch, so an app opened once and abandoned never asks.
 * - "Later" waits for the next date. "No thanks" or starring ends it for good, with no
 *   way back into the schedule; the button in About stays for anyone who changes their mind.
 * - Never shown over music: the caller only asks when nothing is playing.
 * - Reopening the app many times in a day still asks once: the next date only moves when
 *   an ask is actually shown.
 */
object StarPrompt {
    const val REPO = "https://github.com/rajendra7169/blazify"

    /** Days after the first launch for the first asks; after the last, every [REPEAT_DAYS]. */
    private val FIRST_ASKS_DAYS = listOf(3L, 7L, 15L, 30L)
    private const val REPEAT_DAYS = 60L

    /** Test copies built with -PforceStarPrompt=true ask on every launch. */
    private val forced get() = com.blazify.music.BuildConfig.FORCE_STAR_PROMPT

    /** Records the launch and says whether to ask now. */
    suspend fun onOpened(context: Context): Boolean {
        if (forced) return true
        val prefs = context.dataStore.data.first()
        if (prefs[StarPromptDoneKey] == true) return false
        val now = System.currentTimeMillis()
        val firstSeen = prefs[StarPromptFirstSeenKey]
        if (firstSeen == null) {
            // First launch with this schedule: start the clock, ask nothing yet.
            context.dataStore.edit { it[StarPromptFirstSeenKey] = now }
            return false
        }
        val shown = prefs[StarPromptShownKey] ?: 0
        val due =
            if (shown < FIRST_ASKS_DAYS.size) {
                firstSeen + TimeUnit.DAYS.toMillis(FIRST_ASKS_DAYS[shown])
            } else {
                (prefs[StarPromptLastShownKey] ?: firstSeen) + TimeUnit.DAYS.toMillis(REPEAT_DAYS)
            }
        return now >= due
    }

    /** Shown once: the schedule moves on to the next date. */
    suspend fun onShown(context: Context) {
        if (forced) return
        val shown = (context.dataStore.data.first()[StarPromptShownKey] ?: 0) + 1
        context.dataStore.edit {
            it[StarPromptShownKey] = shown
            it[StarPromptLastShownKey] = System.currentTimeMillis()
        }
    }

    /** Starred, or "No thanks". Either way it never asks again. */
    suspend fun stop(context: Context) {
        if (forced) return
        context.dataStore.edit { it[StarPromptDoneKey] = true }
    }
}
