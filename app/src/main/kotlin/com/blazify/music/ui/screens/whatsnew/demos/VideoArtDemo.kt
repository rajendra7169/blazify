/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Screen = Color(0xFF0E0B14)

/** Video Art is chosen and the song's video takes over the player, muted, behind the controls. */
@Composable
internal fun VideoArtDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val classic = stringResource(R.string.player_design_classic)
    val video = stringResource(R.string.player_design_video)
    val card = remember { Path() }
    DemoCanvas(loopMs = 5200, stillAt = 0.60f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val on = ease(seg(t, 0.24f, 0.40f)) * (1f - ease(seg(t, 0.90f, 0.98f)))
            card.reset()
            card.addRoundRect(RoundRect(20f * u, 4f * u, 260f * u, 128f * u, CornerRadius(14f * u)))
            clipPath(card) {
                // The Classic player.
                drawRect(c.panel, Offset(20f * u, 4f * u), Size(240f * u, 124f * u))
                cover(c, 1, note, 184f, 34f, 56f, 10f, u)
                // The video: moving light over a dark picture.
                if (on > 0f) {
                    drawRect(Screen.fade(on), Offset(20f * u, 4f * u), Size(240f * u, 124f * u))
                    for (k in 0 until 3) {
                        val a = t * 2f * PI.toFloat() + k * 2.1f
                        val centre = Offset((140f + 80f * sin(a)) * u, (60f + 30f * cos(a * 1.3f)) * u)
                        val (shade, _) = c.covers[k]
                        drawCircle(
                            Brush.radialGradient(listOf(shade.fade(0.75f * on), Color.Transparent), centre, (48f + k * 10f) * u),
                            (48f + k * 10f) * u,
                            centre,
                        )
                    }
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.fade(0.55f * on)), 60f * u, 128f * u),
                        Offset(20f * u, 60f * u),
                        Size(240f * u, 68f * u),
                    )
                }
            }

            // Design choice at the top.
            val chosen = t > 0.22f && t < 0.94f
            var x = 30f
            listOf(classic, video).forEachIndexed { i, name ->
                val selected = (i == 1) == chosen
                val width = textWidth(measurer, name, 8.5f, u, bold = true) + 16f
                val fill = if (selected) c.accent else lerp(c.control, Color.Black.fade(0.35f), on)
                pill(fill, x, 12f, width, 18f, u)
                centredText(measurer, name, x + width / 2f, 21f, 8.5f, if (selected) c.onAccent else lerp(c.onControl, Color.White, on), u)
                x += width + 6f
            }

            // The song and its place, light over the video.
            val words = lerp(c.ink, Color.White, on)
            val soft = lerp(c.inkSoft, Color.White.fade(0.8f), on)
            text(measurer, SAMPLE_SONGS[1].title, 32f, 88f, 11f, words, u, bold = true, maxWidth = 140f)
            text(measurer, SAMPLE_SONGS[1].artist, 32f, 103f, 9f, soft, u, maxWidth = 140f)
            pill(soft.fade(0.4f), 32f, 119f, 216f, 2.5f, u)
            pill(words, 32f, 119f, 216f * (0.3f + 0.4f * t), 2.5f, u)

            val videoChipCentre = 30f + textWidth(measurer, classic, 8.5f, u, bold = true) + 16f + 6f +
                (textWidth(measurer, video, 8.5f, u, bold = true) + 16f) / 2f
            val k = ease(seg(t, 0.04f, 0.12f))
            finger(
                c,
                cx = mix(200f, videoChipCentre, k),
                cy = mix(110f, 21f, k),
                visible = seg(t, 0.02f, 0.06f) * (1f - seg(t, 0.30f, 0.36f)),
                tap = seg(t, 0.14f, 0.20f),
                u = u,
            )
        }
    }
}
