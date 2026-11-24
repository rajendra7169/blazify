/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R

// Colour choices as Look & Feel offers them: the song's own colour first, then fixed seeds.
private val SEEDS = listOf(Color(0xFFFFA726), Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFF66BB6A))

/** Picking colours and pure black in Look & Feel, with the phone preview changing as it goes. */
@Composable
internal fun LookAndFeelDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val play = painterResource(R.drawable.play)
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val title = stringResource(R.string.look_and_feel)
    val pureBlackLabel = stringResource(R.string.pure_black)
    DemoCanvas(loopMs = 5200, stillAt = 0.58f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val swatches = listOf(c.accent) + SEEDS
            val back = seg(t, 0.95f, 1f)
            var accent = lerp(lerp(swatches[0], swatches[2], seg(t, 0.28f, 0.36f)), swatches[3], seg(t, 0.54f, 0.62f))
            accent = lerp(accent, swatches[0], back)
            val pureBlack = seg(t, 0.76f, 0.84f) * (1f - back)
            val chosen =
                when {
                    t < 0.28f || t >= 0.95f -> 0
                    t < 0.54f -> 2
                    else -> 3
                }

            // The live preview.
            val screen = lerp(c.panel, Color.Black, pureBlack)
            val onScreen = lerp(c.ink, Color.White, pureBlack)
            drawRoundRect(screen, Offset(24f * u, 6f * u), Size(82f * u, 120f * u), CornerRadius(12f * u))
            drawRoundRect(c.line, Offset(24f * u, 6f * u), Size(82f * u, 120f * u), CornerRadius(12f * u), style = Stroke(1f * u))
            text(measurer, "Blazify", 32f, 12f, 8f, onScreen, u, bold = true)
            for (i in 0 until 4) cover(c, i, note, 32f + (i % 2) * 34f, 28f + (i / 2) * 32f, 28f, 5f, u)
            drawRoundRect(accent, Offset(28f * u, 96f * u), Size(74f * u, 24f * u), CornerRadius(8f * u))
            cover(c, 0, null, 31f, 99f, 18f, 4f, u)
            pill(Color.White.fade(0.8f), 53f, 104f, 30f, 3.5f, u)
            pill(Color.White.fade(0.55f), 53f, 111f, 20f, 3f, u)
            icon(play, 93f, 108f, 12f, Color.White, u)

            // The choices.
            text(measurer, title, 122f, 8f, 12f, c.ink, u, bold = true, maxWidth = 150f)
            swatches.forEachIndexed { i, colour ->
                val cx = 134f + i * 28f
                drawCircle(colour, 11f * u, Offset(cx * u, 44f * u))
                if (i == chosen) drawCircle(c.ink, 14.5f * u, Offset(cx * u, 44f * u), style = Stroke(2f * u))
            }
            text(measurer, pureBlackLabel, 122f, 69f, 9.5f, c.ink, u, maxWidth = 96f)
            pill(lerp(c.control, c.accent, pureBlack), 222f, 68f, 30f, 16f, u)
            drawCircle(lerp(c.onControl, c.onAccent, pureBlack), 6f * u, Offset((230f + 14f * pureBlack) * u, 76f * u))

            val toBlue = ease(seg(t, 0.14f, 0.22f))
            val toPurple = ease(seg(t, 0.40f, 0.48f))
            val toSwitch = ease(seg(t, 0.62f, 0.70f))
            finger(
                c,
                cx = mix(mix(mix(250f, 190f, toBlue), 218f, toPurple), 237f, toSwitch),
                cy = mix(mix(mix(122f, 44f, toBlue), 44f, toPurple), 76f, toSwitch),
                visible = seg(t, 0.12f, 0.16f) * (1f - seg(t, 0.84f, 0.90f)),
                tap =
                    when {
                        t < 0.40f -> seg(t, 0.22f, 0.28f)
                        t < 0.62f -> seg(t, 0.48f, 0.54f)
                        else -> seg(t, 0.70f, 0.76f)
                    },
                u = u,
            )
        }
    }
}
