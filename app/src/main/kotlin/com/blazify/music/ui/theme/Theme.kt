/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

// Blazify brand colors (amber -> deep orange gradient)
val BlazeThemeColor = Color(0xFFFFA726)
val BlazeGradientEnd = Color(0xFFFF7043)

/**
 * Cards and sheets in pure-black dark.
 *
 * The iPhone uses #0A0A0A here, but it reaches that colour from a #000000 page
 * only when someone has deliberately turned pure black on — its own dark mode
 * is a tonal one that starts the page at 4% and the card at 9%, so the card is
 * more than twice the brightness of what it sits on. Copying the iPhone's
 * pure-black number onto a page that is genuinely #000000 left a 4% card that
 * did not read as a card at all. These keep the true-black page and restore
 * the step up to it.
 */
val BlazeBlackSurface = Color(0xFF141414)

/** A step above a card: search fields, chips, anything resting on one. */
val BlazeBlackSurfaceHigh = Color(0xFF1F1F1F)

@Composable
fun BlazifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot // Keep existing style
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Use the defined AppTypography
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

/**
 * The pure-black dark scheme, matched to the iPhone build.
 *
 * Material generates every surface tone from the seed colour, so with an amber
 * seed a "black" theme still drew its cards in a warm brown-grey, and album-art
 * theming moved them about as the artwork changed. Blacking out `surface` and
 * `background` alone left every container role behind, which is most of what a
 * settings page is made of.
 *
 * These are the three steps the iPhone uses — black page, #0A0A0A card,
 * #121212 for anything sitting on a card — applied across the whole ramp so a
 * card is the same colour whatever is playing.
 */
fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = BlazeBlackSurface,
        surfaceContainer = BlazeBlackSurface,
        surfaceContainerHigh = BlazeBlackSurfaceHigh,
        surfaceContainerHighest = BlazeBlackSurfaceHigh,
        surfaceBright = BlazeBlackSurfaceHigh,
        surfaceVariant = BlazeBlackSurfaceHigh,
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
