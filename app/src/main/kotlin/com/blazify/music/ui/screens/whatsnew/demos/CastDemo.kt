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

// Sample devices on the same network.
private val DEVICES = listOf("Living room TV", "Bedroom speaker")

/** The cast button lists the TVs nearby; the finger picks one and the song plays there. */
@Composable
internal fun CastDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val cast = painterResource(R.drawable.cast)
    val connected = painterResource(R.drawable.cast_connected)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val castLabel = stringResource(R.string.cast)
    DemoCanvas(loopMs = 4800, stillAt = 0.47f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val casting = t > 0.50f && t < 0.96f

            // The player.
            pill(c.panel, 20f, 8f, 240f, 44f, u, radius = 12f)
            cover(c, 4, note, 26f, 14f, 32f, 6f, u)
            text(measurer, SAMPLE_SONGS[4].title, 66f, 15f, 11f, c.ink, u, bold = true, maxWidth = 140f)
            text(
                measurer,
                if (casting) DEVICES[0] else SAMPLE_SONGS[4].artist,
                66f,
                31f,
                9.5f,
                if (casting) c.accent else c.inkSoft,
                u,
                bold = casting,
                maxWidth = 140f,
            )
            icon(if (casting) connected else cast, 236f, 30f, 20f, if (casting) c.accent else c.ink, u)

            // The devices nearby.
            val sheet = easeOut(seg(t, 0.24f, 0.34f)) * (1f - seg(t, 0.56f, 0.64f))
            if (sheet > 0f) {
                val dy = (1f - sheet) * 12f
                pill(c.panel.fade(sheet), 20f, 60f + dy, 240f, 66f, u, radius = 12f)
                text(measurer, castLabel, 32f, 64f + dy, 9f, c.inkSoft.fade(sheet), u, bold = true)
                DEVICES.forEachIndexed { i, device ->
                    val cy = 89f + i * 24f + dy
                    icon(cast, 40f, cy, 16f, c.ink.fade(sheet), u)
                    text(measurer, device, 56f, cy - 7f, 10.5f, c.ink.fade(sheet), u, bold = true, maxWidth = 180f)
                }
            }

            val toButton = ease(seg(t, 0.06f, 0.16f))
            val toDevice = ease(seg(t, 0.34f, 0.42f))
            finger(
                c,
                cx = mix(mix(200f, 236f, toButton), 140f, toDevice),
                cy = mix(mix(122f, 30f, toButton), 89f, toDevice),
                visible = seg(t, 0.04f, 0.08f) * (1f - seg(t, 0.56f, 0.62f)),
                tap = if (t < 0.34f) seg(t, 0.18f, 0.24f) else seg(t, 0.44f, 0.50f),
                u = u,
            )
        }
    }
}
