/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R

/** An album's last song plays out, similar songs join the queue, and the music carries on. */
@Composable
internal fun KeepsPlayingDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val queueLabel = stringResource(R.string.queue)
    val similarLabel = stringResource(R.string.similar_content)
    DemoCanvas(loopMs = 4800, stillAt = 0.70f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val reset = 1f - seg(t, 0.94f, 0.99f)
            val carriedOn = t > 0.40f && t < 0.97f

            fun row(
                song: SampleSong,
                coverIndex: Int,
                y: Float,
                current: Boolean,
                alpha: Float,
            ) {
                cover(c, coverIndex, note, 24f, y, 20f, 4f, u, alpha = alpha)
                songLines(
                    measurer,
                    c,
                    song,
                    52f,
                    y - 0.5f,
                    u,
                    titleSize = 10f,
                    maxWidth = 170f,
                    alpha = alpha,
                    titleColor = if (current) c.accent else c.ink,
                )
                if (current) playingBars(c, 238f, y + 4f, t, u, alpha)
            }

            text(measurer, queueLabel, 20f, 0f, 10.5f, c.ink, u, bold = true)
            // The album: its last song is playing.
            row(SAMPLE_SONGS[0], 0, 16f, current = false, alpha = 0.6f)
            row(SAMPLE_SONGS[1], 0, 40f, current = !carriedOn, alpha = 1f)
            val progress = if (carriedOn) 1f else seg(t, 0.05f, 0.38f)
            pill(c.line, 52f, 63f, 180f, 2f, u)
            pill(c.accent, 52f, 63f, 180f * progress, 2f, u)

            // Similar songs join the end and the next one starts.
            text(measurer, similarLabel, 24f, 68f, 8.5f, c.accent.fade(seg(t, 0.34f, 0.40f) * reset), u, bold = true)
            for (i in 0 until 2) {
                val shown = easeOut(seg(t, 0.36f + i * 0.06f, 0.48f + i * 0.06f)) * reset
                if (shown <= 0f) continue
                val song = SAMPLE_SONGS[if (i == 0) 2 else 5]
                row(song, if (i == 0) 2 else 5, 84f + i * 24f + (1f - shown) * 10f, current = i == 0 && carriedOn, alpha = shown)
            }
        }
    }
}
