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

// Folder names as a phone's storage has them.
private val FOLDERS = listOf("Music", "Download", "Recordings")
private val FILES = listOf(5, 1, 3, 0)

/** The phone's folders; the finger opens Music and one of its songs starts playing. */
@Composable
internal fun LocalMusicDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val folder = painterResource(R.drawable.folder)
    val chevron = painterResource(R.drawable.navigate_next)
    val back = painterResource(R.drawable.arrow_back)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val foldersTitle = stringResource(R.string.local_folders)
    DemoCanvas(loopMs = 4800, stillAt = 0.60f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            // The folder list slides away to the left, the folder's songs come in from the right.
            val dx = -DESIGN_W * (ease(seg(t, 0.30f, 0.40f)) - ease(seg(t, 0.90f, 0.98f)))

            text(measurer, foldersTitle, 20f + dx, 4f, 11f, c.ink, u, bold = true)
            FOLDERS.forEachIndexed { i, name ->
                val y = 24f + i * 34f
                pill(c.panel, 20f + dx, y, 240f, 30f, u, radius = 10f)
                icon(folder, 38f + dx, y + 15f, 18f, c.accent, u)
                text(measurer, name, 56f + dx, y + 8f, 10.5f, c.ink, u, bold = true)
                icon(chevron, 246f + dx, y + 15f, 16f, c.inkSoft, u)
            }

            val fx = dx + DESIGN_W
            if (fx < DESIGN_W) {
                icon(back, 27f + fx, 11f, 14f, c.ink, u)
                text(measurer, FOLDERS[0], 40f + fx, 4f, 11f, c.ink, u, bold = true)
                val started = t > 0.54f && t < 0.97f
                FILES.forEachIndexed { i, songIndex ->
                    val y = 24f + i * 27f
                    val current = i == 0 && started
                    cover(c, songIndex, note, 20f + fx, y, 22f, 5f, u)
                    songLines(
                        measurer,
                        c,
                        SAMPLE_SONGS[songIndex],
                        50f + fx,
                        y + 0.5f,
                        u,
                        titleSize = 10f,
                        maxWidth = 170f,
                        titleColor = if (current) c.accent else c.ink,
                    )
                    if (current) playingBars(c, 238f + fx, y + 5f, t, u)
                }
            }

            val toFolder = ease(seg(t, 0.08f, 0.18f))
            val toSong = ease(seg(t, 0.40f, 0.47f))
            finger(
                c,
                cx = mix(mix(220f, 140f, toFolder), 140f, toSong),
                cy = mix(mix(122f, 39f, toFolder), 35f, toSong),
                visible = seg(t, 0.06f, 0.10f) * (1f - seg(t, 0.62f, 0.68f)),
                tap = if (t < 0.38f) seg(t, 0.20f, 0.26f) else seg(t, 0.48f, 0.54f),
                u = u,
            )
        }
    }
}
