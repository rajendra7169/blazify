/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R

private val MINUTES = listOf("15m", "30m", "45m", "60m")

/** The finger taps the moon on the mini player, picks 30 minutes, and the countdown starts. */
@Composable
internal fun SleepTimerDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val moon = painterResource(R.drawable.bedtime)
    val play = painterResource(R.drawable.play)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val timerLabel = stringResource(R.string.sleep_timer)
    DemoCanvas(loopMs = 4000, stillAt = 0.55f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            // The mini player.
            pill(c.panel, 20f, 12f, 240f, 44f, u, radius = 12f)
            cover(c, 0, note, 26f, 18f, 32f, 6f, u)
            songLines(measurer, c, SAMPLE_SONGS[0], 66f, 19f, u, titleSize = 11f, maxWidth = 112f)

            val moonOn = t > 0.22f && t < 0.97f
            val counting = seg(t, 0.70f, 0.76f) * (1f - seg(t, 0.96f, 1f))
            if (counting > 0f) {
                val left = 1f - 0.12f * seg(t, 0.70f, 1f)
                drawArc(
                    color = c.accent.fade(counting),
                    startAngle = -90f,
                    sweepAngle = 360f * left,
                    useCenter = false,
                    topLeft = Offset(189f * u, 17f * u),
                    size = Size(34f * u, 34f * u),
                    style = Stroke(2.5f * u, cap = StrokeCap.Round),
                )
            }
            icon(moon, 206f, 34f, 22f, if (moonOn) c.accent else c.ink, u)
            icon(play, 240f, 34f, 22f, c.ink, u)

            // The minutes rise under the bar, as in the real sleep timer.
            val rise = if (t > 0.30f && t < 0.72f) overshoot(seg(t, 0.30f, 0.42f)) * (1f - seg(t, 0.66f, 0.72f)) else 0f
            if (rise > 0f) {
                val alpha = rise.coerceIn(0f, 1f)
                val dy = (1f - rise) * 18f
                text(measurer, timerLabel, 21f, 64f + dy, 9f, c.inkSoft.fade(alpha), u, bold = true)
                MINUTES.forEachIndexed { i, label ->
                    val x = 21f + i * 62f
                    val selected = i == 1 && t > 0.54f
                    pill((if (selected) c.accent else c.control).fade(alpha), x, 80f + dy, 52f, 30f, u)
                    centredText(
                        measurer,
                        label,
                        x + 26f,
                        95f + dy,
                        11f,
                        (if (selected) c.onAccent else c.onControl).fade(alpha),
                        u,
                    )
                }
            }
            if (counting > 0f) {
                val seconds = 1800 - ((t - 0.70f).coerceAtLeast(0f) * 30f).toInt()
                val clock = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                centredText(measurer, clock, 140f, 95f, 24f, c.accent.fade(counting), u)
            }

            val toMoon = ease(seg(t, 0.06f, 0.18f))
            val toChip = ease(seg(t, 0.40f, 0.50f))
            finger(
                c,
                cx = mix(mix(250f, 206f, toMoon), 109f, toChip),
                cy = mix(mix(120f, 34f, toMoon), 95f, toChip),
                visible = seg(t, 0.04f, 0.08f) * (1f - seg(t, 0.60f, 0.65f)),
                tap = if (t < 0.40f) seg(t, 0.20f, 0.26f) else seg(t, 0.52f, 0.57f),
                u = u,
            )
        }
    }
}
