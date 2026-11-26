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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R

private const val CX = 140f
private const val CY = 48f
private val VinylBlack = Color(0xFF151311)

/** The player turns from Classic into Ring, Record, Cassette and Radio, each named under it. */
@Composable
internal fun PlayerDesignsDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val note = painterResource(R.drawable.music_note)
    val measurer = rememberTextMeasurer()
    val names =
        listOf(
            stringResource(R.string.player_design_classic),
            stringResource(R.string.player_design_ring),
            stringResource(R.string.player_design_record),
            stringResource(R.string.player_design_cassette),
            stringResource(R.string.player_design_radio),
        )
    DemoCanvas(loopMs = 8000, stillAt = 0.30f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val step = (t * 5f).toInt().coerceAtMost(4)
            val within = t * 5f - step
            val change = ease(seg(within, 0f, 0.18f))
            val previous = (step + 4) % 5
            val spin = t * 720f

            fun design(
                which: Int,
                alpha: Float,
                grow: Float,
            ) {
                if (alpha <= 0f) return
                scale(grow, pivot = Offset(CX * u, CY * u)) {
                    when (which) {
                        0 -> cover(c, 0, note, CX - 35f, CY - 35f, 70f, 12f, u, alpha = alpha)
                        1 -> ringDesign(c, note, spin, alpha, u)
                        2 -> recordDesign(c, note, spin, alpha, u)
                        3 -> cassetteDesign(c, spin, alpha, u)
                        else -> radioDesign(c, note, within, alpha, u)
                    }
                }
            }
            design(previous, 1f - change, 1f - 0.1f * change)
            design(step, change, 0.9f + 0.1f * change)

            centredText(measurer, SAMPLE_SONGS[0].title, CX, 94f, 10.5f, c.ink, u)

            // The design's name, tapped to move on to the next one.
            val name = names[step]
            val width = textWidth(measurer, name, 10f, u, bold = true) + 24f
            pill(c.accent, CX - width / 2f, 106f, width, 22f, u)
            centredText(measurer, name, CX, 117f, 10f, c.onAccent, u)

            finger(
                c,
                cx = CX,
                cy = 117f,
                visible = seg(within, 0.74f, 0.80f) * (1f - seg(within, 0.95f, 1f)),
                tap = seg(within, 0.84f, 0.92f),
                u = u,
            )
        }
    }
}

private fun DrawScope.ringDesign(
    c: DemoColors,
    note: Painter,
    spin: Float,
    alpha: Float,
    u: Float,
) {
    val centre = Offset(CX * u, CY * u)
    val (from, to) = c.covers[0]
    drawCircle(Brush.linearGradient(listOf(from, to), centre - Offset(32f * u, 32f * u), centre + Offset(32f * u, 32f * u)), 32f * u, centre, alpha = alpha)
    icon(note, CX, CY, 14f, Color.White.fade(0.55f * alpha), u)
    drawCircle(c.line.fade(alpha), 40f * u, centre, style = Stroke(3f * u))
    drawArc(
        color = c.accent.fade(alpha),
        startAngle = -90f,
        sweepAngle = 110f + (spin % 360f) / 6f,
        useCenter = false,
        topLeft = centre - Offset(40f * u, 40f * u),
        size = Size(80f * u, 80f * u),
        style = Stroke(3f * u, cap = StrokeCap.Round),
    )
}

private fun DrawScope.recordDesign(
    c: DemoColors,
    note: Painter,
    spin: Float,
    alpha: Float,
    u: Float,
) {
    val centre = Offset(CX * u, CY * u)
    drawCircle(VinylBlack.fade(alpha), 42f * u, centre)
    drawCircle(Color.White.fade(0.08f * alpha), 34f * u, centre, style = Stroke(1f * u))
    drawCircle(Color.White.fade(0.08f * alpha), 26f * u, centre, style = Stroke(1f * u))
    rotate(spin, pivot = centre) {
        val (from, to) = c.covers[0]
        drawCircle(Brush.linearGradient(listOf(from, to), centre - Offset(15f * u, 15f * u), centre + Offset(15f * u, 15f * u)), 15f * u, centre, alpha = alpha)
        icon(note, CX, CY - 7f, 8f, Color.White.fade(0.6f * alpha), u)
    }
    drawCircle(VinylBlack.fade(alpha), 2.5f * u, centre)
}

private fun DrawScope.cassetteDesign(
    c: DemoColors,
    spin: Float,
    alpha: Float,
    u: Float,
) {
    val (from, to) = c.covers[0]
    drawRoundRect(
        brush = Brush.linearGradient(listOf(from, to), Offset((CX - 52f) * u, (CY - 32f) * u), Offset((CX + 52f) * u, (CY + 32f) * u)),
        topLeft = Offset((CX - 52f) * u, (CY - 32f) * u),
        size = Size(104f * u, 64f * u),
        cornerRadius = CornerRadius(8f * u),
        alpha = alpha,
    )
    pill(c.panel.fade(alpha), CX - 38f, CY - 18f, 76f, 28f, u, radius = 14f)
    for (side in listOf(-1f, 1f)) {
        val reel = Offset((CX + side * 20f) * u, (CY - 4f) * u)
        drawCircle(c.ink.fade(0.75f * alpha), 10f * u, reel)
        drawCircle(c.panel.fade(alpha), 4f * u, reel)
        rotate(spin, pivot = reel) {
            for (spoke in 0 until 3) {
                rotate(spoke * 120f, pivot = reel) {
                    drawLine(c.panel.fade(alpha), reel, reel + Offset(0f, -8f * u), strokeWidth = 1.5f * u)
                }
            }
        }
    }
}

private val RadioWalnut = Color(0xFF5B3A24)
private val RadioGlass = Color(0xFFFFE3A6)
private val RadioNeedle = Color(0xFFD7261E)

/** A small tabletop radio: the cover in its window, a grille, and the dial's needle sweeping along. */
private fun DrawScope.radioDesign(
    c: DemoColors,
    note: Painter,
    sweep: Float,
    alpha: Float,
    u: Float,
) {
    val body = androidx.compose.ui.graphics.lerp(c.accent, RadioWalnut, 0.55f)
    pill(Color.Black.fade(0.25f * alpha), CX - 50f, CY + 30f, 12f, 5f, u, radius = 2f)
    pill(Color.Black.fade(0.25f * alpha), CX + 38f, CY + 30f, 12f, 5f, u, radius = 2f)
    pill(body.fade(alpha), CX - 56f, CY - 36f, 112f, 68f, u, radius = 10f)
    cover(c, 0, note, CX - 48f, CY - 29f, 38f, 5f, u, alpha = alpha)
    // Speaker grille.
    var y = CY - 25f
    var row = 0
    while (y < CY + 6f) {
        var x = CX + 2f + if (row % 2 == 0) 2.5f else 5f
        while (x < CX + 48f) {
            drawCircle(Color.Black.fade(0.45f * alpha), 1.4f * u, Offset(x * u, y * u))
            x += 5f
        }
        y += 4.4f
        row++
    }
    // Dial with its needle following the song.
    pill(RadioGlass.fade(alpha), CX - 48f, CY + 14f, 96f, 12f, u, radius = 3f)
    for (i in 0..10) {
        val tx = CX - 42f + 84f * i / 10f
        val len = if (i % 2 == 0) 4f else 2.5f
        drawLine(c.ink.fade(0.6f * alpha), Offset(tx * u, (CY + 23f - len) * u), Offset(tx * u, (CY + 23f) * u), strokeWidth = 0.8f * u)
    }
    val nx = CX - 42f + 84f * (0.15f + 0.7f * sweep.coerceIn(0f, 1f))
    drawLine(RadioNeedle.fade(alpha), Offset(nx * u, (CY + 15.5f) * u), Offset(nx * u, (CY + 24.5f) * u), strokeWidth = 1.4f * u, cap = StrokeCap.Round)
}
