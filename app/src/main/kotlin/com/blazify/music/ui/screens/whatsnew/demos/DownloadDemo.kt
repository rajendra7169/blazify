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

/** The finger taps a song's download arrow, a ring fills, and the song is marked Downloaded. */
@Composable
internal fun DownloadDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val download = painterResource(R.drawable.download)
    val check = painterResource(R.drawable.check)
    val saved = painterResource(R.drawable.offline)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val downloaded = stringResource(R.string.offline)
    DemoCanvas(loopMs = 3600, stillAt = 0.42f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            // A song row.
            pill(c.panel, 20f, 30f, 240f, 60f, u, radius = 14f)
            cover(c, 1, note, 28f, 38f, 44f, 8f, u)
            songLines(measurer, c, SAMPLE_SONGS[1], 84f, 39f, u, titleSize = 12f, maxWidth = 122f)

            val button = Offset(232f * u, 60f * u)
            val doneIn = seg(t, 0.66f, 0.74f)
            val doneOut = ease(seg(t, 0.90f, 0.96f))
            if (doneIn > 0f && t < 0.96f) {
                val tick = overshoot(doneIn) * (1f - doneOut)
                drawCircle(c.accent, 17f * u * tick, button)
                icon(check, 232f, 60f, 20f * tick, c.onAccent, u)
                // The row now says the song is saved.
                val mark = (1f - doneOut) * doneIn
                icon(saved, 89f, 78f, 10f, c.accent.fade(mark), u)
                text(measurer, downloaded, 96f, 72.5f, 9f, c.accent.fade(mark), u, bold = true, maxWidth = 110f)
            } else {
                if (t > 0.26f && t < 0.96f) {
                    val progress = ease(seg(t, 0.26f, 0.66f))
                    drawCircle(c.line, 17f * u, button, style = Stroke(3f * u))
                    drawArc(
                        color = c.accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(215f * u, 43f * u),
                        size = Size(34f * u, 34f * u),
                        style = Stroke(3f * u, cap = StrokeCap.Round),
                    )
                }
                icon(download, 232f, 60f, 22f, if (t > 0.22f && t < 0.96f) c.accent else c.ink, u)
            }

            val k = ease(seg(t, 0.06f, 0.18f))
            finger(
                c,
                cx = mix(200f, 232f, k),
                cy = mix(122f, 60f, k),
                visible = seg(t, 0.04f, 0.08f) * (1f - seg(t, 0.44f, 0.50f)),
                tap = seg(t, 0.20f, 0.26f),
                u = u,
            )
        }
    }
}
