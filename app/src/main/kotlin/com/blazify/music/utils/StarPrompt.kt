/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.blazify.music.constants.StarPromptAsksKey
import com.blazify.music.constants.StarPromptDoneKey
import com.blazify.music.constants.StarPromptNextAtKey
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Asking for a star: once a day after the first launch, then weekly for two
 * months, and then never again.
 *
 * The rules matter more than the feature. A prompt that returns forever is
 * how an app earns a one-star review from someone who liked it, so:
 *
 * - The clock starts on first launch and the first ask is a day later, so an
 *   app opened once and abandoned never asks at all.
 * - Later means later: a week each time, and after the last of them it stops
 *   on its own rather than carrying on.
 * - No thanks means never, permanently, with no way back into the schedule.
 * - It is never shown over the player or while anything is playing, which the
 *   caller enforces, because interrupting music to ask a favour is worse than
 *   not asking.
 *
 * The button in About stays regardless, for anyone who says no and changes
 * their mind.
 */
object StarPrompt {
    const val REPO = "https://github.com/rajendra7169/blazify"

    private const val HOURS_BEFORE_FIRST_ASK = 24L
    private const val DAYS_BETWEEN_ASKS = 7L

    /** The first ask a day in, then one a week: the last lands just short of two months. */
    private const val MAX_ASKS = 9

    /**
     * Records the launch and says whether to ask.
     *
     * Safe to call on every launch: the schedule only moves when an ask is
     * actually shown, so reopening the app ten times in an evening still gets
     * asked once.
     */
    suspend fun onOpened(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[StarPromptDoneKey] == true) return false
        if ((prefs[StarPromptAsksKey] ?: 0) >= MAX_ASKS) return false

        val nextAt = prefs[StarPromptNextAtKey] ?: 0L
        if (nextAt == 0L) {
            // First launch. Start the clock and ask nothing yet — an app that
            // begs for a star before it has played a song has not earned one.
            context.dataStore.edit {
                it[StarPromptNextAtKey] =
                    System.currentTimeMillis() + TimeUnit.HOURS.toMillis(HOURS_BEFORE_FIRST_ASK)
            }
            return false
        }
        return System.currentTimeMillis() >= nextAt
    }

    /** Shown once. A week until the next, and after the last one it stops. */
    suspend fun onShown(context: Context) {
        val asks = (context.dataStore.data.first()[StarPromptAsksKey] ?: 0) + 1
        context.dataStore.edit {
            it[StarPromptAsksKey] = asks
            if (asks >= MAX_ASKS) {
                it[StarPromptDoneKey] = true
            } else {
                it[StarPromptNextAtKey] =
                    System.currentTimeMillis() + TimeUnit.DAYS.toMillis(DAYS_BETWEEN_ASKS)
            }
        }
    }

    /** Starred, or declined. Either way there is nothing left to ask. */
    suspend fun stop(context: Context) {
        context.dataStore.edit { it[StarPromptDoneKey] = true }
    }
}
