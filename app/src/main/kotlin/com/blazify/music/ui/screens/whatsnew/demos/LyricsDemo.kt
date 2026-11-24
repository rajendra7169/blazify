/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.abs
import kotlin.math.floor

// Sample lyrics written for the demo. Four lines that repeat, so the loop's end matches its start.
private val LINES =
    listOf(
        "City lights are fading slow",
        "I can hear the river call",
        "Hold the moment, let it glow",
        "Every echo finds its home",
    )

/** Lyric lines scroll up, and the line being sung fills with colour from left to right. */
@Composable
internal fun LyricsDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    DemoCanvas(loopMs = 4800, stillAt = 0.62f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val beat = t * 4f
            val line = floor(beat)
            val within = beat - line
            val scroll = line + ease(seg(within, 0f, 0.15f))
            val sung = line.toInt() + 1
            for (i in -2..8) {
                val y = 66f + (i - 1 - scroll) * 30f
                if (y < -12f || y > DESIGN_H + 12f) continue
                val words = LINES[((i % 4) + 4) % 4]
                val active = i == sung
                val size = if (active) 14f else 11f
                val layout =
                    measurer.measure(
                        words,
                        TextStyle(fontSize = (size * u).toSp(), fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold),
                    )
                val left = (DESIGN_W * u - layout.size.width) / 2f
                val top = y * u - layout.size.height / 2f
                if (active) {
                    drawText(layout, color = c.ink.fade(0.3f), topLeft = Offset(left, top))
                    val fill = seg(within, 0.15f, 0.95f)
                    clipRect(left = left, top = top, right = left + layout.size.width * fill, bottom = top + layout.size.height) {
                        drawText(layout, color = c.accent, topLeft = Offset(left, top))
                    }
                } else {
                    val nearMiddle = (1f - abs(y - 66f) / 90f).coerceIn(0f, 1f)
                    drawText(layout, color = c.inkSoft.fade(nearMiddle), topLeft = Offset(left, top))
                }
            }
        }
    }
}
