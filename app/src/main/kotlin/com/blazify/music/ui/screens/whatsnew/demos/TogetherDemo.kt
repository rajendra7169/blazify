/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R

private const val CODE = "K7Q2"

// Sample listeners, the same names onboarding uses.
private val LISTENERS = listOf("Alex", "Maya", "Sam")

/** A room code is shared, friends join one by one, and everyone's song moves together. */
@Composable
internal fun TogetherDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val group = painterResource(R.drawable.group)
    val measurer = rememberTextMeasurer()
    val title = stringResource(R.string.blaze_together)
    val codeLabel = stringResource(R.string.room_code)
    val usersLabel = stringResource(R.string.connected_users)
    DemoCanvas(loopMs = 5200, stillAt = 0.70f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val reset = 1f - seg(t, 0.94f, 0.99f)
            icon(group, 28f, 13f, 16f, c.accent, u)
            text(measurer, title, 42f, 5f, 12f, c.ink, u, bold = true)

            // The room code.
            text(measurer, codeLabel, 20f, 26f, 8.5f, c.inkSoft, u, bold = true, maxWidth = 120f)
            CODE.forEachIndexed { i, char ->
                pill(c.control, 20f + i * 30f, 38f, 26f, 30f, u, radius = 8f)
                centredText(measurer, char.toString(), 33f + i * 30f, 53f, 14f, c.onControl, u)
            }

            // What is playing for everyone.
            songLines(measurer, c, SAMPLE_SONGS[2], 20f, 78f, u, titleSize = 10f, maxWidth = 118f)
            val together = seg(t, 0.55f, 0.60f) * reset
            if (together > 0f) playingBars(c, 122f, 78f, t, u, together)

            // Friends joining.
            val faces = listOf(c.accent to c.onAccent, c.control to c.onControl, c.ink.fade(0.8f) to c.panel)
            text(measurer, usersLabel, 156f, 26f, 8.5f, c.inkSoft, u, bold = true, maxWidth = 110f)
            LISTENERS.forEachIndexed { i, name ->
                val join = seg(t, 0.15f + i * 0.12f, 0.25f + i * 0.12f) * reset
                if (join > 0f) {
                    val cx = 174f + i * 36f
                    val pop = overshoot(join)
                    val (bg, fg) = faces[i]
                    avatar(measurer, name.take(1), cx, 56f, 14f * pop, bg, fg, u)
                    if (i == 0) drawCircle(c.accent.fade(join), 17f * u, Offset(cx * u, 56f * u), style = Stroke(1.6f * u))
                    centredText(measurer, name, cx, 80f, 8.5f, c.inkSoft.fade(join), u, bold = false)
                }
            }

            // One position for everyone.
            val progress = 0.2f + 0.5f * seg(t, 0.55f, 1f)
            pill(c.line, 20f, 112f, 240f, 3f, u)
            pill(c.accent.fade(0.4f + 0.6f * together), 20f, 112f, 240f * progress, 3f, u)
            // Everyone's place in the song, on the same spot.
            for (i in LISTENERS.indices) {
                val mark = seg(t, 0.15f + i * 0.12f, 0.25f + i * 0.12f) * reset
                if (mark > 0f) {
                    val (bg, _) = faces[i]
                    drawCircle(bg.fade(mark), (6f - i * 1.5f) * u, Offset((20f + 240f * progress) * u, 113.5f * u))
                }
            }

            val k = ease(seg(t, 0.03f, 0.10f))
            finger(
                c,
                cx = mix(150f, 78f, k),
                cy = mix(120f, 53f, k),
                visible = seg(t, 0.02f, 0.05f) * (1f - seg(t, 0.20f, 0.26f)),
                tap = seg(t, 0.10f, 0.16f),
                u = u,
            )
        }
    }
}
