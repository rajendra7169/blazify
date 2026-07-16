/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Selectable player LAYOUTS (not colours). The player's colours stay dynamic
 * (album-art driven); only the arrangement of art / controls changes.
 */

package com.blazify.music.ui.player

import androidx.annotation.StringRes
import com.blazify.music.R

enum class PlayerDesign(
    val id: String,          // stable, persisted — never rename
    @StringRes val nameRes: Int,
) {
    CLASSIC("classic", R.string.player_design_classic),
    RING("ring", R.string.player_design_ring),
    FULL_ART("full_art", R.string.player_design_full_art),
    RECORD("record", R.string.player_design_record),
    ;

    companion object {
        fun fromId(id: String?): PlayerDesign = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}
