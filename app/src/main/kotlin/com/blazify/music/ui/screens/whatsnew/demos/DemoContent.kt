/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/*
 * What fills the demos so they look like the real screens: sample songs, text, album covers,
 * the playing indicator and account pictures.
 */

internal class SampleSong(
    val title: String,
    val artist: String,
)

/**
 * Made-up songs for the demos. Like the sample listeners in onboarding, these are example
 * content, not app text, so they are not translated.
 */
internal val SAMPLE_SONGS =
    listOf(
        SampleSong("Lantern Season", "Mira Vale"),
        SampleSong("Paper Skylines", "The Low Tides"),
        SampleSong("Monsoon Postcards", "Aarav Sen"),
        SampleSong("Neon Riverside", "Kite & Co"),
        SampleSong("Late Bus Home", "Juniper Ave"),
        SampleSong("Echo Valley", "Sherpa Sound"),
    )

private fun DrawScope.layout(
    measurer: TextMeasurer,
    text: String,
    size: Float,
    u: Float,
    bold: Boolean,
    maxWidth: Float,
    lines: Int = 1,
) = measurer.measure(
    text,
    TextStyle(fontSize = (size * u).toSp(), fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
    overflow = TextOverflow.Ellipsis,
    maxLines = lines,
    constraints = Constraints(maxWidth = (maxWidth * u).toInt().coerceAtLeast(1)),
)

/** Text ([lines] lines at most) with its top-left at ([x], [y]), cut with an ellipsis past [maxWidth]. */
internal fun DrawScope.text(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    u: Float,
    bold: Boolean = false,
    maxWidth: Float = 200f,
    lines: Int = 1,
) {
    if (color.alpha <= 0f || text.isEmpty()) return
    drawText(layout(measurer, text, size, u, bold, maxWidth, lines), color = color, topLeft = Offset(x * u, y * u))
}

/** How wide [text] is, in design units. */
internal fun DrawScope.textWidth(
    measurer: TextMeasurer,
    text: String,
    size: Float,
    u: Float,
    bold: Boolean = false,
    maxWidth: Float = 260f,
): Float = if (text.isEmpty()) 0f else layout(measurer, text, size, u, bold, maxWidth).size.width / u

/** An album cover in one of the theme's shades, with a faint note on it. */
internal fun DrawScope.cover(
    colors: DemoColors,
    index: Int,
    note: Painter?,
    x: Float,
    y: Float,
    side: Float,
    radius: Float,
    u: Float,
    alpha: Float = 1f,
) {
    if (alpha <= 0f) return
    val (from, to) = colors.covers[index % colors.covers.size]
    drawRoundRect(
        brush = Brush.linearGradient(listOf(from, to), start = Offset(x * u, y * u), end = Offset((x + side) * u, (y + side) * u)),
        topLeft = Offset(x * u, y * u),
        size = Size(side * u, side * u),
        cornerRadius = CornerRadius(radius * u),
        alpha = alpha.coerceIn(0f, 1f),
    )
    if (note != null && side >= 20f) icon(note, x + side / 2f, y + side / 2f, side * 0.45f, Color.White.fade(0.55f * alpha), u)
}

/** A song's title above its artist, as in the app's lists. */
internal fun DrawScope.songLines(
    measurer: TextMeasurer,
    colors: DemoColors,
    song: SampleSong,
    x: Float,
    y: Float,
    u: Float,
    titleSize: Float = 11f,
    maxWidth: Float = 140f,
    alpha: Float = 1f,
    titleColor: Color = colors.ink,
) {
    text(measurer, song.title, x, y, titleSize, titleColor.fade(alpha), u, bold = true, maxWidth = maxWidth)
    text(measurer, song.artist, x, y + titleSize * 1.4f, titleSize * 0.85f, colors.inkSoft.fade(alpha), u, maxWidth = maxWidth)
}

/** The three moving bars that mark the song that is playing. */
internal fun DrawScope.playingBars(
    colors: DemoColors,
    x: Float,
    y: Float,
    t: Float,
    u: Float,
    alpha: Float = 1f,
) {
    for (k in 0 until 3) {
        val h = 4f + 8f * abs(sin(t * 2f * PI.toFloat() * 6f + k * 1.3f))
        pill(colors.accent.fade(alpha), x + k * 4.5f, y + 12f - h, 2.8f, h, u, radius = 1.4f)
    }
}

/** A round account picture with the first letter of the name. */
internal fun DrawScope.avatar(
    measurer: TextMeasurer,
    letter: String,
    cx: Float,
    cy: Float,
    radius: Float,
    background: Color,
    foreground: Color,
    u: Float,
) {
    if (radius <= 0f) return
    drawCircle(background, radius * u, Offset(cx * u, cy * u))
    centredText(measurer, letter, cx, cy, radius * 0.95f, foreground, u)
}
