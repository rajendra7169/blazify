/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * The buttons a person can put on the mini-player, and which ones each design shows
 * until they choose. Chosen per design, so changing design never loses a choice.
 */

package com.blazify.music.ui.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.stringPreferencesKey
import com.blazify.music.R
import com.blazify.music.utils.rememberPreference

enum class MiniPlayerAction(
    val id: String, // stable, persisted — never rename
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    PLAY_PAUSE("play_pause", R.string.play_pause, R.drawable.play),
    PREVIOUS("previous", R.string.previous, R.drawable.skip_previous),
    NEXT("next", R.string.next, R.drawable.skip_next),
    LIKE("like", R.string.like, R.drawable.favorite_border),
    ADD_TO_PLAYLIST("add_to_playlist", R.string.add_to_playlist, R.drawable.add),
    SUBSCRIBE_ARTIST("subscribe_artist", R.string.subscribe, R.drawable.subscribe),
    VIEW_ARTIST("view_artist", R.string.view_artist, R.drawable.artist),
    START_RADIO("start_radio", R.string.start_radio, R.drawable.radio),
    DOWNLOAD("download", R.string.action_download, R.drawable.download),
    SLEEP_TIMER("sleep_timer", R.string.sleep_timer, R.drawable.bedtime),
    SHUFFLE("shuffle", R.string.shuffle, R.drawable.shuffle),
    REPEAT("repeat", R.string.repeat, R.drawable.repeat),
    ;

    /** Previous, play and next sit close together, as one set of controls. */
    val isTransport: Boolean get() = this == PREVIOUS || this == PLAY_PAUSE || this == NEXT

    companion object {
        /** More than this and the song's title has no room left on a small phone. */
        const val MAX = 3

        fun fromId(id: String): MiniPlayerAction? = entries.firstOrNull { it.id == id }

        /** What each design showed before there was a choice, so nobody's mini-player changes by itself. */
        fun defaultsFor(design: MiniPlayerDesign): List<MiniPlayerAction> =
            when (design) {
                MiniPlayerDesign.FLAT -> listOf(PLAY_PAUSE, NEXT)
                MiniPlayerDesign.ROUNDED -> listOf(PREVIOUS, PLAY_PAUSE, NEXT)
                MiniPlayerDesign.MODERN, MiniPlayerDesign.FLOATING -> listOf(SUBSCRIBE_ARTIST, ADD_TO_PLAYLIST, LIKE)
            }

        fun encode(actions: List<MiniPlayerAction>): String = actions.joinToString(",") { it.id }

        /** Blank means "the design's own"; an id from a later version is skipped rather than failing. */
        fun decode(text: String, design: MiniPlayerDesign): List<MiniPlayerAction> =
            if (text.isBlank()) {
                defaultsFor(design)
            } else {
                text.split(",").mapNotNull { fromId(it.trim()) }.distinct().take(MAX)
            }
    }
}

fun miniPlayerButtonsKey(design: MiniPlayerDesign) = stringPreferencesKey("miniPlayerButtons_${design.id}")

/** The buttons chosen for [design], or the design's own when none were chosen. */
@Composable
fun rememberMiniPlayerActions(design: MiniPlayerDesign): List<MiniPlayerAction> =
    // rememberPreference keeps the first key it is given, so each design gets its own.
    key(design) {
        val text by rememberPreference(miniPlayerButtonsKey(design), "")
        remember(text) { MiniPlayerAction.decode(text, design) }
    }
