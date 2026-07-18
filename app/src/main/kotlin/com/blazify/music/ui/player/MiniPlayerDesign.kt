/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Selectable mini-player LAYOUTS. Colours stay dynamic (album-art driven);
 * only the shape / arrangement of the collapsed mini-player changes.
 */

package com.blazify.music.ui.player

import androidx.annotation.StringRes
import com.blazify.music.R

enum class MiniPlayerDesign(
    val id: String, // stable, persisted — never rename
    @StringRes val nameRes: Int,
) {
    FLAT("flat", R.string.mini_player_design_flat), // the original edge-to-edge flat bar
    MODERN("modern", R.string.mini_player_design_modern), // current rounded pill (tap art to play)
    ROUNDED("rounded", R.string.mini_player_design_rounded), // rounded pill with prev / play / next
    FLOATING("floating", R.string.mini_player_design_floating), // boxy rounded floating card
    ;

    companion object {
        fun fromId(id: String?): MiniPlayerDesign = entries.firstOrNull { it.id == id } ?: MODERN
    }
}
