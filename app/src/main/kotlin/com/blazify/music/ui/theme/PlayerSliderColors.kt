/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.blazify.music.constants.PlayerBackgroundStyle

/**
 * Player slider color configuration for consistent styling across all slider types
 * 
 * This object provides standardized color schemes for Default, Squiggly, and Slim sliders
 * used in the music player interface, ensuring visual consistency and proper contrast.
 */
object PlayerSliderColors {

    /**
     * Standard slider colors for all slider types
     * 
     * @param activeColor Color for active track, ticks, and thumb
     * @param playerBackground The player background style
     * @param useDarkTheme Whether dark theme is being used
     * @return SliderColors configuration
     */
    @Composable
    fun getSliderColors(
        activeColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean,
        // When set, this colour is used for the active track/thumb instead of the
        // static Blaze amber — e.g. the RING design passes the dynamic album colour.
        activeOverride: Color? = null,
    ): SliderColors {
        val inactiveTrackColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> {
                if (useDarkTheme) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            }
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                // Blazify: white24 inactive track over the artwork gradient
                Color.White.copy(alpha = 0.24f)
            }
        }

        // Blazify: amber accent progress over blur/gradient backgrounds,
        // unless an override (e.g. the dynamic album colour) is provided.
        val effectiveActiveColor = activeOverride ?: when (playerBackground) {
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> BlazeThemeColor
            else -> activeColor
        }

        return SliderDefaults.colors(
            activeTrackColor = effectiveActiveColor,
            activeTickColor = effectiveActiveColor,
            thumbColor = effectiveActiveColor,
            inactiveTrackColor = inactiveTrackColor,
            disabledActiveTrackColor = effectiveActiveColor,
            disabledInactiveTrackColor = inactiveTrackColor,
            disabledThumbColor = effectiveActiveColor
        )
    }
}
