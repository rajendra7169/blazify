/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * Blazify's loading animation: the logo telling a short story, over and over.
 * A play button settles into place, the flame grows around it, the headband
 * sweeps over and the ear cups come down and bounce, sound rolls out of them
 * and music notes drift up, and then it all eases back into the play button.
 * Something is always about to happen, so a wait is watched rather than
 * counted. One component, so every place that waits shows the same thing.
 * The signature is drop-in compatible with CircularProgressIndicator so
 * existing call sites keep working.
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** The logo's parts, parsed once for the whole app. */
private object LogoPaths {
    private fun parse(d: String) = PathParser().parsePathString(d).toPath()

    val band by lazy { parse(BlazeLogoShapes.BAND) }
    val cupLeft by lazy { parse(BlazeLogoShapes.CUP_LEFT) }
    val cupRight by lazy { parse(BlazeLogoShapes.CUP_RIGHT) }
    val flame by lazy { parse(BlazeLogoShapes.FLAME + BlazeLogoShapes.PLAY).apply { fillType = PathFillType.EvenOdd } }
    val play by lazy { parse(BlazeLogoShapes.PLAY) }
    val sweep by lazy { parse(BlazeLogoShapes.BAND_SWEEP) }
    val noteSingle by lazy { parse(BlazeLogoShapes.NOTE_SINGLE) }
    val notePair by lazy { parse(BlazeLogoShapes.NOTE_PAIR) }
    val rippleLeft by lazy { parse("M150 545 a110 110 0 0 0 0 150") }
    val rippleRight by lazy { parse("M874 545 a110 110 0 0 1 0 150") }
}

/** The story's length; the life on top of it runs on its own shorter rhythms. */
private const val STORY_SECONDS = 4f

@Composable
fun BlazeLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    // Compatibility with CircularProgressIndicator call sites — ignored.
    strokeWidth: Dp = Dp.Unspecified,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val base = if (color.isSpecified) color else MaterialTheme.colorScheme.primary
    val palette = remember(base) { LoaderPalette(base) }
    val bandLine = remember { Path() }

    // Seconds since this loader appeared, read every frame. A single clock, so the
    // story and the life on top of it can never drift apart or stutter at a seam.
    val seconds by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
    }

    Canvas(modifier = modifier.size(44.dp)) {
        val side = size.minDimension
        val k = side / BlazeLogoShapes.VIEW_SIZE
        withTransform({
            translate((size.width - side) / 2f, (size.height - side) / 2f)
            scale(k, k, pivot = Offset.Zero)
            translate(-BlazeLogoShapes.VIEW_LEFT, -BlazeLogoShapes.VIEW_TOP)
        }) {
            drawStory(seconds, palette, bandLine)
        }
    }
}

private class LoaderPalette(base: Color) {
    val top = base
    val bottom = lerp(base, Color(0xFFFF4D1A), 0.35f)
    val cups = lerp(base, Color.White, 0.25f)
    val cream = lerp(base, Color.White, 0.88f)
    val notes = lerp(base, Color.White, 0.55f)
    val glow = base
}

private fun DrawScope.drawStory(t: Float, c: LoaderPalette, bandLine: Path) {
    val u = (t % STORY_SECONDS) / STORY_SECONDS
    val logoBrush = Brush.verticalGradient(listOf(c.top, c.bottom), startY = 180f, endY = 780f)

    // A warm glow gathering behind the logo while it is whole.
    val glow = envelope(u, 0.08f, 0.45f, 0.78f, 1f)
    if (glow > 0f) {
        val r = 300f * (0.5f + 0.5f * glow)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c.glow.copy(alpha = 0.42f * glow), Color.Transparent),
                center = Offset(512f, 560f),
                radius = r,
            ),
            radius = r,
            center = Offset(512f, 560f),
        )
    }

    // Sound rolling out of the ear cups, and music notes rising off the flame,
    // while the logo is whole.
    val alive = envelope(u, 0.48f, 0.56f, 0.76f, 0.88f)
    if (alive > 0f) {
        drawRipples(t, alive, c)
        drawNotes(t, alive, c)
    }

    // The headband sweeps in along its own line, holds, then lets go from where it began.
    val reveal = ease(ramp(u, 0.20f, 0.46f))
    val erase = ease(ramp(u, 0.78f, 0.96f))
    if (reveal > erase) {
        val measure = PathMeasure().apply { setPath(LogoPaths.sweep, false) }
        bandLine.reset()
        measure.getSegment(erase * measure.length, reveal * measure.length, bandLine, true)
        drawContext.canvas.saveLayer(Rect(0f, 0f, 1024f, 1024f), Paint())
        drawPath(LogoPaths.band, logoBrush)
        drawPath(bandLine, Color.White, style = Stroke(width = 200f, cap = StrokeCap.Round), blendMode = BlendMode.DstIn)
        drawContext.canvas.restore()
    }

    // The ear cups float down into place, then bounce, one after the other.
    val cups = envelope(u, 0.34f, 0.50f, 0.78f, 0.92f)
    if (cups > 0f) {
        drawCup(LogoPaths.cupLeft, t, 0f, cups, c.cups)
        drawCup(LogoPaths.cupRight, t, CUP_BOUNCE_SECONDS / 2f, cups, c.cups)
    }

    // The flame grows around the play button and keeps swaying.
    val flame = envelope(u, 0.08f, 0.30f, 0.80f, 0.96f)
    if (flame > 0f) {
        val b = LogoPaths.flame.getBounds()
        val pivot = Offset(b.center.x, b.top + b.height * 0.85f)
        val sway = sin(t * 2f * PI.toFloat() / 3.2f)
        withTransform({
            scale(flame, flame, pivot)
            rotate(2.2f * sway, pivot)
            scale(1f - 0.012f * sway, 1f + 0.015f * sway, pivot)
        }) {
            drawPath(LogoPaths.flame, logoBrush, alpha = flame)
        }
    }

    // The play button: large and warm at the start, settled and pale inside the flame.
    val settled = envelope(u, 0.06f, 0.26f, 0.80f, 1f)
    val pale = envelope(u, 0.08f, 0.26f, 0.80f, 0.98f)
    val playBounds = LogoPaths.play.getBounds()
    scale(2.2f - 1.2f * settled, playBounds.center) {
        drawPath(LogoPaths.play, lerp(c.top, c.cream, pale))
    }
}

/** One full hop and landing of an ear cup. */
private const val CUP_BOUNCE_SECONDS = 0.9f

private fun DrawScope.drawCup(path: Path, t: Float, offset: Float, shown: Float, color: Color) {
    val b = path.getBounds()
    val foot = Offset(b.center.x, b.bottom)
    // A small hop, then a soft squash as it lands.
    val p = ((t + offset) % CUP_BOUNCE_SECONDS) / CUP_BOUNCE_SECONDS
    var lift = 0f
    var squash = 0f
    if (p < 0.55f) lift = sin(PI.toFloat() * p / 0.55f) else squash = sin(PI.toFloat() * (p - 0.55f) / 0.45f)
    val hop = 16f * lift * shown
    withTransform({
        translate(0f, -40f * (1f - shown) - hop)
        scale(0.6f + 0.4f * shown, 0.6f + 0.4f * shown, Offset(b.center.x, b.top))
        scale(1f + 0.05f * squash, 1f - 0.07f * squash, foot)
    }) {
        drawPath(path, color, alpha = shown)
    }
}

private fun DrawScope.drawRipples(t: Float, alive: Float, c: LoaderPalette) {
    for (start in floatArrayOf(0f, 0.8f)) {
        val k = ((t + start) % 1.6f) / 1.6f
        val out = 70f * easeOut(k)
        val a = 0.9f * (1f - k) * alive
        val stroke = Stroke(width = 20f, cap = StrokeCap.Round)
        translate(-out, 0f) { drawPath(LogoPaths.rippleLeft, c.top, alpha = a, style = stroke) }
        translate(out, 0f) { drawPath(LogoPaths.rippleRight, c.top, alpha = a, style = stroke) }
    }
}

private fun DrawScope.drawNotes(t: Float, alive: Float, c: LoaderPalette) {
    // Where each note leaves the flame, which note it is, and which way it drifts.
    val notes = listOf(
        Triple(Offset(470f, 300f), LogoPaths.noteSingle, -1f),
        Triple(Offset(548f, 286f), LogoPaths.notePair, 1f),
        Triple(Offset(512f, 262f), LogoPaths.noteSingle, -1f),
    )
    notes.forEachIndexed { i, (from, shape, side) ->
        val k = ((t + 2.2f - i * 0.73f) % 2.2f) / 2.2f
        val a = (if (k < 0.2f) k / 0.2f else 1f - (k - 0.2f) / 0.8f) * alive
        if (a <= 0f) return@forEachIndexed
        // Up and out, then back across: a gentle S as it rises.
        val first = ease(ramp(k, 0f, 0.5f))
        val second = ease(ramp(k, 0.5f, 1f))
        val x = side * (14f * first - 20f * second)
        val y = -70f * first - 80f * second
        val turn = side * (6f * first - 10f * second)
        val size = 0.7f + 0.3f * first - 0.1f * second
        withTransform({
            translate(from.x + x, from.y + y)
            rotate(turn, Offset.Zero)
            scale(size, size, Offset.Zero)
        }) {
            drawPath(shape, c.notes, alpha = a)
        }
    }
}

/** 0 before [a], 1 after [b], straight in between. */
private fun ramp(u: Float, a: Float, b: Float) = ((u - a) / (b - a)).coerceIn(0f, 1f)

/** Slow in, slow out. */
private fun ease(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f

private fun easeOut(x: Float) = 1f - (1f - x).let { it * it * it }

/** Eases up from 0 to 1 between [inStart] and [inEnd], holds, then eases back to 0 by [outEnd]. */
private fun envelope(u: Float, inStart: Float, inEnd: Float, outStart: Float, outEnd: Float) =
    ease(ramp(u, inStart, inEnd)) * (1f - ease(ramp(u, outStart, outEnd)))

/**
 * The loader for a download: the logo's headphones hold still while an arrow in
 * the flame's colours drops through them into a tray, again and again. The
 * headphones say it is Blazify, the arrow says what it is doing, so a spinner
 * beside a song reads as "saving to the phone" rather than as any other wait.
 * Same signature as [BlazeLoader], so a call site can swap one for the other.
 */
@Composable
fun BlazeDownloadLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    // Compatibility with CircularProgressIndicator call sites — ignored.
    strokeWidth: Dp = Dp.Unspecified,
) {
    val base = if (color.isSpecified) color else MaterialTheme.colorScheme.primary
    val palette = remember(base) { LoaderPalette(base) }

    val seconds by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
    }

    Canvas(modifier = modifier.size(44.dp)) {
        val side = size.minDimension
        val k = side / BlazeLogoShapes.VIEW_SIZE
        withTransform({
            translate((size.width - side) / 2f, (size.height - side) / 2f)
            scale(k, k, pivot = Offset.Zero)
            translate(-BlazeLogoShapes.VIEW_LEFT, -BlazeLogoShapes.VIEW_TOP)
        }) {
            drawDownload(seconds, palette)
        }
    }
}

/** One drop of the arrow into the tray. */
private const val DROP_SECONDS = 1.3f

private val ArrowShape by lazy {
    Path().apply {
        // Shaft and head as one shape, so it fills in one go and keeps round joins.
        moveTo(476f, 330f)
        lineTo(548f, 330f)
        lineTo(548f, 536f)
        lineTo(640f, 536f)
        lineTo(512f, 676f)
        lineTo(384f, 536f)
        lineTo(476f, 536f)
        close()
    }
}

private val TrayShape by lazy {
    Path().apply {
        moveTo(380f, 700f)
        lineTo(380f, 770f)
        lineTo(644f, 770f)
        lineTo(644f, 700f)
    }
}

private fun DrawScope.drawDownload(t: Float, c: LoaderPalette) {
    val logoBrush = Brush.verticalGradient(listOf(c.top, c.bottom), startY = 180f, endY = 780f)
    val u = (t % DROP_SECONDS) / DROP_SECONDS

    // The headphones: the part of the logo people know, held still as the frame.
    drawPath(LogoPaths.band, logoBrush, alpha = 0.9f)
    drawPath(LogoPaths.cupLeft, c.cups)
    drawPath(LogoPaths.cupRight, c.cups)

    // The arrow falls in from above, lands, and sinks into the tray as it fades,
    // while the next one is already on its way.
    val fall = easeOut(ramp(u, 0f, 0.5f))
    val sink = ease(ramp(u, 0.62f, 0.95f))
    val y = -170f * (1f - fall) + 70f * sink
    val a = ramp(u, 0f, 0.18f) * (1f - sink)
    if (a > 0f) {
        translate(0f, y) {
            drawPath(ArrowShape, logoBrush, alpha = a)
            drawPath(
                ArrowShape,
                logoBrush,
                alpha = a,
                style = Stroke(width = 34f, join = StrokeJoin.Round),
            )
        }
    }

    // The tray gives a little as each arrow lands, and glows while it does.
    val land = envelope(u, 0.46f, 0.56f, 0.62f, 0.86f)
    withTransform({
        scale(1f + 0.06f * land, 1f - 0.10f * land, Offset(512f, 770f))
    }) {
        drawPath(TrayShape, lerp(c.cups, c.top, land), style = Stroke(width = 46f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
