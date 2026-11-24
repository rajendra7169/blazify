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

/** A queue row is swiped right, shows Play next, and moves up to play after this song. */
@Composable
internal fun QueueSwipeDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val playNext = painterResource(R.drawable.playlist_play)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val playNextLabel = stringResource(R.string.play_next)
    DemoCanvas(loopMs = 4400, stillAt = 0.40f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val swipe = ease(seg(t, 0.18f, 0.40f)) * (1f - ease(seg(t, 0.46f, 0.56f)))
            val moved = if (t < 0.96f) ease(seg(t, 0.58f, 0.74f)) else 0f
            // Fades out and back in around the loop's restart, so the rows never jump.
            val shown =
                when {
                    t < 0.92f -> 1f
                    t < 0.96f -> 1f - seg(t, 0.92f, 0.96f)
                    else -> seg(t, 0.96f, 1f)
                }
            val swipedY = mix(52f, 14f, moved)

            fun row(
                index: Int,
                y: Float,
                dx: Float,
                marked: Boolean,
            ) {
                pill(c.panel.fade(shown), 20f + dx, y, 240f, 32f, u, radius = 10f)
                cover(c, index, note, 26f + dx, y + 4f, 24f, 5f, u, alpha = shown)
                songLines(measurer, c, SAMPLE_SONGS[index], 58f + dx, y + 3.5f, u, titleSize = 10.5f, maxWidth = 160f, alpha = shown)
                pill(c.inkSoft.fade(0.6f * shown), 236f + dx, y + 12f, 14f, 2.2f, u)
                pill(c.inkSoft.fade(0.6f * shown), 236f + dx, y + 18f, 14f, 2.2f, u)
                if (marked) pill(c.accent.fade(shown), 20f + dx, y + 8f, 3f, 16f, u)
            }

            row(3, mix(14f, 52f, moved), 0f, marked = false)
            row(5, 90f, 0f, marked = false)
            if (swipe > 0.01f) {
                pill(c.accent.fade(shown), 20f, swipedY, 240f, 32f, u, radius = 10f)
                icon(playNext, 38f, swipedY + 16f, 18f, c.onAccent.fade(shown), u)
                text(measurer, playNextLabel, 52f, swipedY + 9.5f, 10f, c.onAccent.fade(shown), u, bold = true, maxWidth = 100f)
            }
            row(4, swipedY, swipe * 120f, marked = t > 0.5f && t < 0.96f)

            finger(
                c,
                cx = 70f + swipe * 120f,
                cy = 68f,
                visible = seg(t, 0.06f, 0.10f) * (1f - seg(t, 0.42f, 0.48f)),
                tap = seg(t, 0.12f, 0.16f),
                u = u,
            )
        }
    }
}
