/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** The finger taps the sound wave in Home's search bar, it listens, and the song appears. */
@Composable
internal fun RecognitionDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val search = painterResource(R.drawable.search)
    val wave = painterResource(R.drawable.sound_wave)
    val play = painterResource(R.drawable.play)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val hint = stringResource(R.string.search)
    val listeningLabel = stringResource(R.string.recognition_notification_listening)
    DemoCanvas(loopMs = 4400, stillAt = 0.40f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            // Home's search bar, with the recognition button at its end.
            pill(c.panel, 20f, 14f, 240f, 36f, u)
            icon(search, 38f, 32f, 18f, c.ink.fade(0.7f), u)
            text(measurer, hint, 54f, 25f, 11f, c.inkSoft, u, maxWidth = 150f)
            val listening = t > 0.22f && t < 0.97f
            if (listening) drawCircle(c.accent, 15f * u, Offset(238f * u, 32f * u))
            icon(wave, 238f, 32f, 18f, if (listening) c.onAccent else c.ink, u)

            // Listening.
            val waves = seg(t, 0.28f, 0.34f) * (1f - seg(t, 0.60f, 0.66f))
            if (waves > 0f) {
                for (i in 0 until 11) {
                    val h = (6f + 26f * abs(sin(t * 2f * PI.toFloat() * 5f + i * 0.7f))) * waves
                    pill(c.accent.fade(0.85f * waves), 87.5f + i * 10f, 82f - h / 2f, 5f, h, u)
                }
                centredText(measurer, listeningLabel, 140f, 112f, 9.5f, c.inkSoft.fade(waves), u, bold = false)
            }

            // Found.
            val card = easeOut(seg(t, 0.64f, 0.76f)) * (1f - seg(t, 0.93f, 0.98f))
            if (card > 0f) {
                val dy = (1f - card) * 30f
                pill(c.panel.fade(card), 40f, 66f + dy, 200f, 52f, u, radius = 14f)
                cover(c, 2, note, 48f, 74f + dy, 36f, 8f, u, alpha = card)
                songLines(measurer, c, SAMPLE_SONGS[2], 94f, 76f + dy, u, titleSize = 11f, maxWidth = 112f, alpha = card)
                icon(play, 222f, 92f + dy, 18f, c.accent.fade(card), u)
            }

            val k = ease(seg(t, 0.06f, 0.18f))
            finger(
                c,
                cx = mix(200f, 238f, k),
                cy = mix(120f, 32f, k),
                visible = seg(t, 0.04f, 0.08f) * (1f - seg(t, 0.46f, 0.52f)),
                tap = seg(t, 0.20f, 0.26f),
                u = u,
            )
        }
    }
}
