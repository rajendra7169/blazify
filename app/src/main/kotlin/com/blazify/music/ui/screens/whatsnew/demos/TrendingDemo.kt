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

/** Home's Trending shelf fills in, top three first, and the finger opens the full chart. */
@Composable
internal fun TrendingDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val arrow = painterResource(R.drawable.arrow_forward)
    val rising = painterResource(R.drawable.trending_up)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val title = stringResource(R.string.trending)
    DemoCanvas(loopMs = 4400, stillAt = 0.66f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val opened = t > 0.62f && t < 0.95f
            text(measurer, title, 20f, 4f, 13f, c.ink, u, bold = true, maxWidth = 200f)
            if (opened) drawCircle(c.accent.fade(0.18f), 13f * u, androidx.compose.ui.geometry.Offset(250f * u, 13f * u))
            icon(arrow, 250f, 13f, 16f, if (opened) c.accent else c.ink, u)

            for (i in 0 until 3) {
                val shown = easeOut(seg(t, 0.08f + i * 0.08f, 0.28f + i * 0.08f)) * (1f - seg(t, 0.94f, 0.99f))
                if (shown <= 0f) continue
                val y = 30f + i * 34f
                val dx = (1f - shown) * 30f
                pill(c.panel.fade(shown), 16f + dx, y - 2f, 248f, 32f, u, radius = 10f)
                centredText(measurer, "${i + 1}", 28f + dx, y + 14f, 12f, c.accent.fade(shown), u)
                cover(c, i + 3, note, 40f + dx, y + 1f, 26f, 6f, u, alpha = shown)
                songLines(measurer, c, SAMPLE_SONGS[(i + 3) % SAMPLE_SONGS.size], 74f + dx, y + 2f, u, titleSize = 10.5f, maxWidth = 150f, alpha = shown)
                icon(rising, 246f + dx, y + 14f, 14f, c.accent.fade(shown * (1f - i * 0.25f)), u)
            }

            val k = ease(seg(t, 0.46f, 0.56f))
            finger(
                c,
                cx = mix(200f, 250f, k),
                cy = mix(120f, 13f, k),
                visible = seg(t, 0.44f, 0.48f) * (1f - seg(t, 0.74f, 0.80f)),
                tap = seg(t, 0.58f, 0.64f),
                u = u,
                // The arrow sits at the top edge of the box: a smaller finger, and a still frame
                // just after the tap, keep its ring inside the box.
                size = 10f,
            )
        }
    }
}
