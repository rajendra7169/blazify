/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 8D is chosen and a note circles the listener's head, passing behind and in front. */
@Composable
internal fun SpatialSoundDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val labels =
        listOf(
            stringResource(R.string.spatial_sound_off),
            stringResource(R.string.spatial_sound_surround),
            stringResource(R.string.spatial_sound_orbit),
        )
    DemoCanvas(loopMs = 4800, stillAt = 0.62f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val cx = 140f
            val cy = 54f
            val orbit = seg(t, 0.22f, 0.32f) * (1f - seg(t, 0.94f, 0.99f))
            drawOval(
                c.line,
                topLeft = Offset((cx - 92f) * u, (cy - 20f) * u),
                size = Size(184f * u, 40f * u),
                style = Stroke(1.5f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * u, 5f * u))),
            )
            val angle = t * 4f * PI.toFloat()
            val depth = sin(angle)
            val nx = cx + 92f * cos(angle)
            val ny = cy + 20f * depth
            val scale = 0.8f + 0.3f * (depth + 1f) / 2f
            if (depth <= 0f) icon(note, nx, ny, 20f * scale, c.accent.fade(0.45f * orbit), u)

            // The listener, headphones on.
            drawCircle(c.ink.fade(0.85f), 18f * u, Offset(cx * u, cy * u))
            drawArc(
                color = c.accent,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset((cx - 25f) * u, (cy - 25f) * u),
                size = Size(50f * u, 50f * u),
                style = Stroke(4f * u, cap = StrokeCap.Round),
            )
            pill(c.accent, cx - 30f, cy - 6f, 8f, 16f, u)
            pill(c.accent, cx + 22f, cy - 6f, 8f, 16f, u)
            if (depth > 0f) icon(note, nx, ny, 20f * scale, c.accent.fade(orbit), u)

            // Off, Surround and 8D, as in Player settings; 8D gets chosen.
            val style = TextStyle(fontSize = (11f * u).toSp(), fontWeight = FontWeight.Bold)
            val layouts = labels.map { measurer.measure(it, style) }
            val widths = layouts.map { it.size.width / u + 24f }
            var x = (DESIGN_W - (widths.sum() + 8f * (labels.size - 1))) / 2f
            val chosen = if (t > 0.19f && t < 0.97f) 2 else 0
            var chosenCentre = 0f
            layouts.forEachIndexed { i, layout ->
                val selected = i == chosen
                pill(if (selected) c.accent else c.control, x, 102f, widths[i], 24f, u)
                drawText(
                    layout,
                    color = if (selected) c.onAccent else c.onControl,
                    topLeft = Offset((x + widths[i] / 2f) * u - layout.size.width / 2f, 114f * u - layout.size.height / 2f),
                )
                if (i == 2) chosenCentre = x + widths[i] / 2f
                x += widths[i] + 8f
            }

            val k = ease(seg(t, 0.04f, 0.14f))
            finger(
                c,
                cx = mix(250f, chosenCentre, k),
                cy = mix(130f, 114f, k),
                visible = seg(t, 0.02f, 0.06f) * (1f - seg(t, 0.30f, 0.36f)),
                tap = seg(t, 0.16f, 0.22f),
                u = u,
            )
        }
    }
}
