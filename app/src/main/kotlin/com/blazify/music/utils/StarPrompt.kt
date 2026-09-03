/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.blazify.music.constants.StarPromptAsksKey
import com.blazify.music.constants.StarPromptDaysUsedKey
import com.blazify.music.constants.StarPromptDoneKey
import com.blazify.music.constants.StarPromptLastDayKey
import com.blazify.music.constants.StarPromptNextAtKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Asking for a star, at most three times, and then never again.
 *
 * The rules matter more than the feature. A prompt that returns forever is
 * how an app earns a one-star review from someone who liked it, so:
 *
 * - Days are counted by opening the app, not by the calendar since install.
 *   Somebody who installed it and forgot has not used it for three days.
 * - Later means later, and the gap grows: three days, then a fortnight, then
 *   a month, and after the third ask it stops on its own.
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

    private const val DAYS_BEFORE_FIRST_ASK = 3
    private val LATER_GAPS = listOf(15L, 30L)
    private const val MAX_ASKS = 3

    /**
     * Records that the app was opened today and says whether to ask.
     *
     * Safe to call on every launch: the day counter moves at most once per
     * calendar day.
     */
    suspend fun onOpened(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[StarPromptDoneKey] == true) return false

        val today = LocalDate.now().toString()
        var daysUsed = prefs[StarPromptDaysUsedKey] ?: 0

        if (prefs[StarPromptLastDayKey] != today) {
            daysUsed += 1
            context.dataStore.edit {
                it[StarPromptLastDayKey] = today
                it[StarPromptDaysUsedKey] = daysUsed
            }
        }

        if (daysUsed < DAYS_BEFORE_FIRST_ASK) return false
        if ((prefs[StarPromptAsksKey] ?: 0) >= MAX_ASKS) return false

        val nextAt = prefs[StarPromptNextAtKey] ?: 0L
        return System.currentTimeMillis() >= nextAt
    }

    /** Shown once. The next gap is longer, and after the last one it stops. */
    suspend fun onShown(context: Context) {
        val asks = (context.dataStore.data.first()[StarPromptAsksKey] ?: 0) + 1
        context.dataStore.edit {
            it[StarPromptAsksKey] = asks
            val gap = LATER_GAPS.getOrNull(asks - 1)
            if (gap == null) {
                it[StarPromptDoneKey] = true
            } else {
                it[StarPromptNextAtKey] = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(gap)
            }
        }
    }

    /** Starred, or declined. Either way there is nothing left to ask. */
    suspend fun stop(context: Context) {
        context.dataStore.edit { it[StarPromptDoneKey] = true }
    }
}
