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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R

private const val QUERY = "mira"

// What the saved songs on the phone answer for the search: the same artist, three songs.
private val RESULTS =
    listOf(
        SampleSong("Lantern Season", "Mira Vale"),
        SampleSong("Glass Harbour", "Mira Vale"),
        SampleSong("Slow Orbit", "Mira Vale"),
    )

/** With no connection, a search is typed and the songs saved on the phone come up. */
@Composable
internal fun SearchOfflineDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val search = painterResource(R.drawable.search)
    val cloud = painterResource(R.drawable.cloud)
    val saved = painterResource(R.drawable.offline)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val hint = stringResource(R.string.search_offline)
    DemoCanvas(loopMs = 4400, stillAt = 0.80f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            pill(c.panel, 20f, 14f, 240f, 36f, u)
            icon(search, 38f, 32f, 18f, c.ink.fade(0.7f), u)
            // No connection: a crossed-out cloud.
            icon(cloud, 238f, 32f, 18f, c.ink.fade(0.55f), u)
            drawLine(
                c.ink.fade(0.55f),
                Offset(229f * u, 23f * u),
                Offset(247f * u, 41f * u),
                strokeWidth = 1.8f * u,
                cap = StrokeCap.Round,
            )

            val typing = t > 0.22f && t < 0.97f
            if (typing) {
                val typed = QUERY.take((seg(t, 0.30f, 0.50f) * QUERY.length).toInt())
                text(measurer, typed, 54f, 24.5f, 11.5f, c.ink, u, bold = true)
                val width = textWidth(measurer, typed, 11.5f, u, bold = true)
                val caretOn = t < 0.55f || (t * 4400f / 450f).toInt() % 2 == 0
                if (caretOn) drawRect(c.accent, Offset((55f + width) * u, 24f * u), Size(2f * u, 16f * u))
            } else {
                text(measurer, hint, 54f, 25.5f, 10.5f, c.inkSoft, u, maxWidth = 165f)
            }

            // Songs that play offline, each marked as saved.
            RESULTS.forEachIndexed { i, song ->
                val shown = easeOut(seg(t, 0.56f + i * 0.06f, 0.68f + i * 0.06f)) * (1f - seg(t, 0.93f, 0.98f))
                if (shown > 0f) {
                    val y = 60f + i * 24f + (1f - shown) * 8f
                    pill(c.panel.fade(shown), 28f, y, 224f, 21f, u, radius = 7f)
                    cover(c, i, note, 31f, y + 3f, 15f, 4f, u, alpha = shown)
                    text(measurer, song.title, 52f, y + 4f, 9.5f, c.ink.fade(shown), u, bold = true, maxWidth = 100f)
                    text(measurer, song.artist, 152f, y + 4.5f, 9f, c.inkSoft.fade(shown), u, maxWidth = 76f)
                    icon(saved, 241f, y + 10.5f, 12f, c.accent.fade(shown), u)
                }
            }

            val k = ease(seg(t, 0.06f, 0.18f))
            finger(
                c,
                cx = mix(220f, 130f, k),
                cy = mix(122f, 32f, k),
                visible = seg(t, 0.04f, 0.08f) * (1f - seg(t, 0.30f, 0.35f)),
                tap = seg(t, 0.20f, 0.25f),
                u = u,
            )
        }
    }
}
