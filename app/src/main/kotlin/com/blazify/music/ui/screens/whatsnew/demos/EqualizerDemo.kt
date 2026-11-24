/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.rememberTextMeasurer

private const val DRAGGED = 3
private val GAINS = floatArrayOf(0.25f, 0.35f, 0.3f, 0.05f, -0.1f, -0.15f, 0f, 0.2f, 0.35f, 0.3f)

// The ten bands' frequencies, as the equalizer labels them.
private val BANDS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/** Ten bands with the curve through them; the finger drags one band up and the curve follows. */
@Composable
internal fun EqualizerDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val curve = remember { Path() }
    val measurer = rememberTextMeasurer()
    DemoCanvas(loopMs = 4400, stillAt = 0.45f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val lift = ease(seg(t, 0.20f, 0.45f)) * (1f - ease(seg(t, 0.86f, 0.96f)))

            fun gain(band: Int) = if (band == DRAGGED) mix(GAINS[band], 0.8f, lift) else GAINS[band]

            fun bandX(band: Int) = 32f + band * 24f

            fun bandY(band: Int) = 58f - gain(band) * 38f

            curve.reset()
            for (band in 0 until 10) {
                pill(c.line, bandX(band) - 1.5f, 14f, 3f, 88f, u)
                centredText(measurer, BANDS[band], bandX(band), 118f, 8f, c.inkSoft, u, bold = false)
                if (band == 0) {
                    curve.moveTo(bandX(band) * u, bandY(band) * u)
                } else {
                    val mid = (bandX(band - 1) + bandX(band)) / 2f
                    curve.cubicTo(
                        mid * u,
                        bandY(band - 1) * u,
                        mid * u,
                        bandY(band) * u,
                        bandX(band) * u,
                        bandY(band) * u,
                    )
                }
            }
            drawPath(curve, c.accent, style = Stroke(2.5f * u, cap = StrokeCap.Round))
            val dragging = lift > 0f && t < 0.5f
            for (band in 0 until 10) {
                val radius = if (band == DRAGGED && dragging) 8f else 6f
                val centre = Offset(bandX(band) * u, bandY(band) * u)
                drawCircle(c.accent, radius * u, centre)
                drawCircle(c.panel, (radius - 3f) * u, centre)
            }

            finger(
                c,
                cx = bandX(DRAGGED),
                cy = bandY(DRAGGED),
                visible = seg(t, 0.06f, 0.10f) * (1f - seg(t, 0.50f, 0.56f)),
                tap = seg(t, 0.12f, 0.16f),
                u = u,
            )
        }
    }
}
