/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * CASSETTE design centrepiece: a 3D retro compact cassette (dark shell, cream
 * label, accent stripe, "A" side badge, "60" length mark) with a window showing
 * the two tape reels. The album art is printed faintly across the label (under
 * the printed texture) with a small full-opacity art preview beside the "A"
 * badge. The reels spin while a song plays and the tape visibly transfers from
 * the left spool to the right as playback progresses; on pause everything
 * freezes. Compose Canvas + coil — shared by the player and the design-preview.
 */

package com.blazify.music.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

/* ---- retro palette (matches the reference; deliberately static) ---- */
private val ShellTop = Color(0xFF5A4B3D)
private val ShellMid = Color(0xFF3B3128)
private val ShellBottom = Color(0xFF262019)
private val LabelCream = Color(0xFFF2E7D0)
private val LabelCreamDark = Color(0xFFE4D6BC)
private val LabelBorder = Color(0xFFC9B999)
private val RetroOrange = Color(0xFFE0812A)
private val WindowDark = Color(0xFF241D15)
private val SpoolDark = Color(0xFF14100B)
private val HubCream = Color(0xFFEFE6D2)
private val InkBrown = Color(0xFF3A2F24)
private val PenInk = Color(0xFF23395B)
private val TapeBrown = Color(0xFF3B2416)
private val TapeBrownLight = Color(0xFF5C3A22)
private val HubWhite = Color(0xFFF4F1EA)

/** Tape speed past the heads while playing, in hub radii per second: an empty hub turns about once a second. */
private const val PLAY_SPEED = 6.3f

/** The whole tape's length in hub radii: a jump across all of it winds the small reel round about 25 times. */
private const val WIND_LENGTH = 160f

/** Radius of the tape wound on a reel, in hub radii, holding [share] of the tape (area grows with the tape on it). */
private const val FULL_PACK = 1.95f

private fun packRadius(share: Float): Float =
    kotlin.math.sqrt(1f + (FULL_PACK * FULL_PACK - 1f) * share.coerceIn(0f, 1f))

/** Where each reel has turned to, and how far the tape has run, kept between frames. */
private class ReelState {
    var left by androidx.compose.runtime.mutableFloatStateOf(0f)
    var right by androidx.compose.runtime.mutableFloatStateOf(0f)
    var travel by androidx.compose.runtime.mutableFloatStateOf(0f)

    /** The tape ran [length] hub radii: each reel turns by that over its own radius. */
    fun advance(length: Float, leftRadius: Float, rightRadius: Float) {
        if (length == 0f) return
        val deg = 57.29578f
        // Both reels turn the same way (anticlockwise from the front) as the tape runs left to right.
        left = (left - length / leftRadius * deg) % 360f
        right = (right - length / rightRadius * deg) % 360f
        travel += length
    }
}

@Composable
fun CassetteTape(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    // Dynamic accent (album colour) for the label stripe; retro neutrals stay fixed.
    accent: Color = RetroOrange,
    // Album art: printed faintly across the label + a small preview beside "A".
    thumbnailUrl: String? = null,
    // The song's title, handwritten on the label's line as people wrote on their tapes.
    label: String? = null,
) {
    // The tape moves the way a real one does. It runs past the heads at one steady speed, so
    // each reel turns at that speed divided by the radius of the tape wound on it: the nearly
    // empty reel spins fast, the full one slowly, and they trade places through the song. A
    // jump in the song winds the tape there at speed, as fast-forward and rewind did. Paused,
    // nothing moves, and nothing is drawn per frame.
    val tape by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "tapeTransfer",
    )
    val reels = remember { ReelState() }
    val latestTape by rememberUpdatedState(tape)
    val target = progress.coerceIn(0f, 1f)
    LaunchedEffect(isPlaying, target) {
        var last = withFrameNanos { it }
        var lastTape = latestTape
        while (isPlaying || kotlin.math.abs(latestTape - target) > 0.0005f) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
                last = now
                val tp = latestTape
                var moved = (tp - lastTape) * WIND_LENGTH
                lastTape = tp
                if (isPlaying) moved += PLAY_SPEED * dt
                reels.advance(moved, packRadius(1f - tp), packRadius(tp))
            }
        }
    }

    BoxWithConstraints(modifier = modifier.aspectRatio(1.55f)) {
        val bw = maxWidth
        val bh = maxHeight

        // 1) Base: shadows, shell, cream label.
        Canvas(modifier = Modifier.fillMaxSize()) { drawCassetteBase() }

        // 2) Album art printed across the label — low opacity, under the texture.
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = bw * 0.075f, y = bh * 0.075f)
                    .size(width = bw * 0.83f, height = bh * 0.545f)
                    .clip(RoundedCornerShape(bw * 0.02f))
                    .alpha(0.22f),
            )
        }

        // 3) Printed texture + window + reels on top of the art.
        Canvas(modifier = Modifier.fillMaxSize()) { drawCassetteDetails(tape, reels.left, reels.right, reels.travel, accent) }

        // 4) Small full-opacity art preview, left of the "A" badge.
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = bw * 0.10f, y = bh * 0.10f)
                    .size(width = bw * 0.055f, height = bh * 0.085f)
                    .clip(RoundedCornerShape(bw * 0.008f))
                    .border(1.dp, InkBrown.copy(alpha = 0.6f), RoundedCornerShape(bw * 0.008f)),
            )
        }

        // Side badge "A" (inside the drawn badge box, right of the art preview).
        Text(
            text = "A",
            color = InkBrown,
            fontWeight = FontWeight.Black,
            fontSize = (bw.value * 0.045f).sp,
            modifier = Modifier.offset(x = bw * 0.182f, y = bh * 0.095f),
        )
        // The title in pen on the label's ruled line, beside the "A".
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = PenInk,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = (bw.value * 0.05f).sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(x = bw * 0.255f, y = bh * 0.072f)
                    .size(width = bw * 0.6f, height = bh * 0.085f),
            )
        }
        // What is printed on a real stripe: the tape's type.
        Text(
            text = "TYPE I · NORMAL",
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold,
            fontSize = (bw.value * 0.024f).sp,
            letterSpacing = (bw.value * 0.003f).sp,
            modifier = Modifier.offset(x = bw * 0.10f, y = bh * 0.478f),
        )
        // Length mark "60" on the accent stripe.
        Text(
            text = "60",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (bw.value * 0.052f).sp,
            modifier = Modifier.offset(x = bw * 0.82f, y = bh * 0.465f),
        )
    }
}

/**
 * Where a line from [from] touches the circle ([center], [radius]) on its outer side: the
 * point the tape leaves the pack for the roller.
 */
private fun tangentPoint(center: Offset, radius: Float, from: Offset, outerIsLeft: Boolean): Offset {
    val d = from - center
    val dist = kotlin.math.sqrt(d.x * d.x + d.y * d.y)
    if (dist <= radius) return Offset(center.x + if (outerIsLeft) -radius else radius, center.y)
    val base = kotlin.math.atan2(d.y, d.x)
    val off = kotlin.math.acos(radius / dist)
    val a1 = base + off
    val a2 = base - off
    val p1 = Offset(center.x + radius * cos(a1), center.y + radius * sin(a1))
    val p2 = Offset(center.x + radius * cos(a2), center.y + radius * sin(a2))
    return if (outerIsLeft) (if (p1.x < p2.x) p1 else p2) else (if (p1.x > p2.x) p1 else p2)
}

/** Pass 1 — shadows, shell body and the cream label backing. */
private fun DrawScope.drawCassetteBase() {
    val s = size.width
    val t = size.height

    /* --- layered drop shadow (soft, wide → tight, dark = 3D lift) --- */
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(s * 0.05f, t * 0.10f),
        size = Size(s * 0.97f, t * 0.93f),
        cornerRadius = CornerRadius(s * 0.06f, s * 0.06f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(s * 0.035f, t * 0.075f),
        size = Size(s * 0.965f, t * 0.925f),
        cornerRadius = CornerRadius(s * 0.055f, s * 0.055f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.24f),
        topLeft = Offset(s * 0.022f, t * 0.048f),
        size = Size(s * 0.96f, t * 0.93f),
        cornerRadius = CornerRadius(s * 0.05f, s * 0.05f),
    )

    /* --- shell --- */
    val shellTL = Offset(s * 0.005f, 0f)
    val shellSize = Size(s * 0.97f, t * 0.955f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(ShellTop, ShellMid, ShellBottom)),
        topLeft = shellTL,
        size = shellSize,
        cornerRadius = CornerRadius(s * 0.045f, s * 0.045f),
    )
    // Double-wall bevel: inner dark line + outer hairline light.
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(shellTL.x + s * 0.012f, shellTL.y + t * 0.02f),
        size = Size(shellSize.width - s * 0.024f, shellSize.height - t * 0.04f),
        cornerRadius = CornerRadius(s * 0.035f, s * 0.035f),
        style = Stroke(width = s * 0.003f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = shellTL,
        size = shellSize,
        cornerRadius = CornerRadius(s * 0.045f, s * 0.045f),
        style = Stroke(width = s * 0.0025f),
    )
    // Top catch-light + bottom inner shade.
    drawLine(
        Color.White.copy(alpha = 0.18f),
        Offset(s * 0.06f, t * 0.018f),
        Offset(s * 0.92f, t * 0.018f),
        strokeWidth = t * 0.012f,
        cap = StrokeCap.Round,
    )
    drawLine(
        Color.Black.copy(alpha = 0.30f),
        Offset(s * 0.06f, t * 0.935f),
        Offset(s * 0.92f, t * 0.935f),
        strokeWidth = t * 0.014f,
        cap = StrokeCap.Round,
    )

    // Corner screws.
    for ((sx, sy) in listOf(0.045f to 0.07f, 0.935f to 0.07f, 0.045f to 0.87f, 0.935f to 0.87f)) {
        val c = Offset(s * sx, t * sy)
        drawCircle(Color(0xFF1C1712), radius = s * 0.016f, center = c)
        drawCircle(Color.White.copy(alpha = 0.20f), radius = s * 0.016f, center = c, style = Stroke(width = s * 0.003f))
        drawLine(Color(0xFF54463A), c - Offset(s * 0.008f, 0f), c + Offset(s * 0.008f, 0f), strokeWidth = s * 0.004f)
    }

    // Write-protect notches along the top edge.
    for (nx in listOf(0.13f, 0.82f)) {
        drawRoundRect(
            color = Color(0xFF15110D),
            topLeft = Offset(s * nx, 0f),
            size = Size(s * 0.05f, t * 0.03f),
            cornerRadius = CornerRadius(s * 0.006f, s * 0.006f),
        )
    }
    // Moulded grip ridges either side of the head opening.
    for (side in listOf(0.085f, 0.745f)) {
        for (i in 0 until 9) {
            val x = s * (side + i * 0.019f)
            drawLine(Color.Black.copy(alpha = 0.28f), Offset(x, t * 0.80f), Offset(x, t * 0.90f), strokeWidth = s * 0.004f)
            drawLine(Color.White.copy(alpha = 0.07f), Offset(x + s * 0.004f, t * 0.80f), Offset(x + s * 0.004f, t * 0.90f), strokeWidth = s * 0.002f)
        }
    }

    /* --- cream label backing --- */
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(LabelCream, LabelCreamDark)),
        topLeft = Offset(s * 0.075f, t * 0.075f),
        size = Size(s * 0.83f, t * 0.545f),
        cornerRadius = CornerRadius(s * 0.02f, s * 0.02f),
    )
}

/** Pass 2 — printed texture, window, reels and gloss, drawn over the label art. */
private fun DrawScope.drawCassetteDetails(progress: Float, leftDeg: Float, rightDeg: Float, travel: Float, accent: Color) {
    val s = size.width
    val t = size.height

    // Label frame (over the art edge).
    drawRoundRect(
        color = LabelBorder,
        topLeft = Offset(s * 0.075f, t * 0.075f),
        size = Size(s * 0.83f, t * 0.545f),
        cornerRadius = CornerRadius(s * 0.02f, s * 0.02f),
        style = Stroke(width = s * 0.004f),
    )

    // "A" badge box (letter drawn as a Text overlay; sits right of the art preview).
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(s * 0.17f, t * 0.10f),
        size = Size(s * 0.055f, t * 0.085f),
        cornerRadius = CornerRadius(s * 0.008f, s * 0.008f),
        style = Stroke(width = s * 0.0045f),
    )

    // The ruled line the title is written on.
    drawLine(
        InkBrown.copy(alpha = 0.35f),
        Offset(s * 0.255f, t * 0.148f),
        Offset(s * 0.875f, t * 0.148f),
        strokeWidth = s * 0.0025f,
    )

    // Accent stripe (dynamic colour) with print texture + a thinner echo line.
    drawRect(
        color = accent,
        topLeft = Offset(s * 0.075f, t * 0.455f),
        size = Size(s * 0.83f, t * 0.105f),
    )
    for (i in 1..3) {
        val ly = t * (0.455f + 0.105f * i / 4f)
        drawLine(
            Color.Black.copy(alpha = 0.10f),
            Offset(s * 0.075f, ly),
            Offset(s * 0.905f, ly),
            strokeWidth = t * 0.006f,
        )
    }
    drawRect(
        color = accent.copy(alpha = 0.55f),
        topLeft = Offset(s * 0.075f, t * 0.575f),
        size = Size(s * 0.83f, t * 0.022f),
    )

    /* --- reel window: smoked clear plastic --- */
    val winTL = Offset(s * 0.285f, t * 0.165f)
    val winSize = Size(s * 0.43f, t * 0.26f)
    val winRadius = s * 0.02f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF2A2119), Color(0xFF43362A))),
        topLeft = winTL,
        size = winSize,
        cornerRadius = CornerRadius(winRadius, winRadius),
    )
    val windowPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = winTL.x,
                top = winTL.y,
                right = winTL.x + winSize.width,
                bottom = winTL.y + winSize.height,
                cornerRadius = CornerRadius(winRadius, winRadius),
            ),
        )
    }
    val reelY = winTL.y + winSize.height * 0.47f
    val leftC = Offset(s * 0.385f, reelY)
    val rightC = Offset(s * 0.615f, reelY)
    val hub = s * 0.047f
    val leftPack = hub * packRadius(1f - progress)
    val rightPack = hub * packRadius(progress)
    val winBottom = winTL.y + winSize.height
    // Guide rollers in the window's lower corners; the tape runs round them and between them.
    val roller = s * 0.011f
    val leftRoller = Offset(winTL.x + winSize.width * 0.075f, winBottom - t * 0.04f)
    val rightRoller = Offset(winTL.x + winSize.width * 0.925f, winBottom - t * 0.04f)

    clipPath(windowPath) {
        // The tape: off the left pack, round the left roller, along the bottom, round the right
        // roller and onto the right pack. Faint marks on it run along with it.
        val leftTouch = tangentPoint(leftC, leftPack, Offset(leftRoller.x - roller, leftRoller.y), outerIsLeft = true)
        val rightTouch = tangentPoint(rightC, rightPack, Offset(rightRoller.x + roller, rightRoller.y), outerIsLeft = false)
        val path = Path().apply {
            moveTo(leftTouch.x, leftTouch.y)
            lineTo(leftRoller.x - roller, leftRoller.y)
            quadraticTo(leftRoller.x - roller, leftRoller.y + roller, leftRoller.x, leftRoller.y + roller)
            lineTo(rightRoller.x, rightRoller.y + roller)
            quadraticTo(rightRoller.x + roller, rightRoller.y + roller, rightRoller.x + roller, rightRoller.y)
            lineTo(rightTouch.x, rightTouch.y)
        }
        drawPath(path, Color(0xFF1C110A), style = Stroke(width = t * 0.016f))
        drawPath(path, Color(0xFF6B4428), style = Stroke(width = t * 0.011f))
        drawPath(
            path,
            Color(0xFFB98A5E).copy(alpha = 0.8f),
            style = Stroke(
                width = t * 0.005f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(s * 0.01f, s * 0.016f),
                    phase = -(travel * hub) % (s * 0.026f),
                ),
            ),
        )
        for (r in listOf(leftRoller, rightRoller)) {
            drawCircle(HubWhite, radius = roller, center = r)
            drawCircle(Color.Black.copy(alpha = 0.3f), radius = roller * 0.45f, center = r)
        }

        for ((center, pack, angle) in listOf(Triple(leftC, leftPack, leftDeg), Triple(rightC, rightPack, rightDeg))) {
            // The wound tape: glossy brown, darker near the hub.
            if (pack > hub * 1.02f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF24160D), TapeBrown, TapeBrownLight),
                        center = center,
                        radius = pack,
                    ),
                    radius = pack,
                    center = center,
                )
            }
            rotate(degrees = angle, pivot = center) {
                // Uneven turns of tape and a few brighter bands, turning with the reel so the
                // pack is seen going round, not only the hub.
                if (pack > hub * 1.02f) {
                    for (k in 1..5) {
                        val rr = hub + (pack - hub) * k / 6f
                        drawCircle(Color.Black.copy(alpha = 0.10f + 0.04f * (k % 2)), radius = rr, center = center, style = Stroke(width = s * 0.0015f))
                    }
                    for ((start, sweep) in listOf(15f to 50f, 140f to 25f, 230f to 60f)) {
                        val rr = hub + (pack - hub) * 0.7f
                        drawArc(
                            color = Color.White.copy(alpha = 0.07f),
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = center - Offset(rr, rr),
                            size = Size(rr * 2, rr * 2),
                            style = Stroke(width = (pack - hub) * 0.35f),
                        )
                    }
                }
                // The white plastic hub with its six inward teeth.
                drawCircle(HubWhite, radius = hub, center = center)
                drawCircle(Color.Black.copy(alpha = 0.18f), radius = hub, center = center, style = Stroke(width = s * 0.003f))
                drawCircle(Color(0xFF120E0A), radius = s * 0.029f, center = center)
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                    drawLine(
                        HubWhite,
                        center + Offset(dir.x * s * 0.017f, dir.y * s * 0.017f),
                        center + Offset(dir.x * s * 0.031f, dir.y * s * 0.031f),
                        strokeWidth = s * 0.008f,
                        cap = StrokeCap.Butt,
                    )
                }
                for (i in 0 until 3) {
                    val a = Math.toRadians(i * 120.0 + 30.0)
                    drawCircle(Color.Black.copy(alpha = 0.35f), radius = s * 0.005f, center = center + Offset(cos(a).toFloat() * s * 0.039f, sin(a).toFloat() * s * 0.039f))
                }
            }
            // Light catching the pack's edge, from the same side whatever the reel does.
            if (pack > hub * 1.05f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.16f),
                    startAngle = 200f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = center - Offset(pack * 0.9f, pack * 0.9f),
                    size = Size(pack * 1.8f, pack * 1.8f),
                    style = Stroke(width = s * 0.006f, cap = StrokeCap.Round),
                )
            }
        }

        // Shadow under the window's rim, so the reels sit inside the shell.
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                startY = winTL.y,
                endY = winTL.y + winSize.height * 0.22f,
            ),
            topLeft = winTL,
            size = Size(winSize.width, winSize.height * 0.22f),
        )
        // Tick scale moulded into the clear plastic.
        for (i in 0..12) {
            val x = winTL.x + winSize.width * (0.1f + 0.8f * i / 12f)
            val h = if (i % 3 == 0) t * 0.03f else t * 0.016f
            drawLine(Color.White.copy(alpha = 0.22f), Offset(x, winBottom - t * 0.012f - h), Offset(x, winBottom - t * 0.012f), strokeWidth = s * 0.0022f)
        }
        // Glass gloss: one broad reflection and a thin bright streak.
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f), Color.Transparent),
                start = Offset(winTL.x, winTL.y),
                end = Offset(winTL.x + winSize.width * 0.7f, winTL.y + winSize.height),
            ),
            topLeft = winTL,
            size = winSize,
            cornerRadius = CornerRadius(winRadius, winRadius),
        )
        drawLine(
            Color.White.copy(alpha = 0.05f),
            Offset(winTL.x + winSize.width * 0.62f, winTL.y),
            Offset(winTL.x + winSize.width * 0.42f, winBottom),
            strokeWidth = s * 0.02f,
        )
    }
    // Clear plastic rim round the window.
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
        topLeft = winTL,
        size = winSize,
        cornerRadius = CornerRadius(winRadius, winRadius),
        style = Stroke(width = s * 0.006f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = winTL - Offset(s * 0.004f, s * 0.004f),
        size = Size(winSize.width + s * 0.008f, winSize.height + s * 0.008f),
        cornerRadius = CornerRadius(winRadius * 1.2f, winRadius * 1.2f),
        style = Stroke(width = s * 0.003f),
    )

    /* --- bottom trapezoid with capstan holes --- */
    val trap = Path().apply {
        moveTo(s * 0.30f, t * 0.945f)
        lineTo(s * 0.35f, t * 0.755f)
        lineTo(s * 0.65f, t * 0.755f)
        lineTo(s * 0.70f, t * 0.945f)
    }
    drawPath(trap, Color(0xFF6B5B49).copy(alpha = 0.65f), style = Stroke(width = s * 0.006f))
    for (hx in listOf(0.415f, 0.585f)) {
        val c = Offset(s * hx, t * 0.85f)
        drawCircle(Color(0xFF17120D), radius = s * 0.016f, center = c)
        drawCircle(Color.White.copy(alpha = 0.18f), radius = s * 0.016f, center = c, style = Stroke(width = s * 0.003f))
    }
    for (hx in listOf(0.375f, 0.625f)) {
        drawCircle(Color(0xFF17120D), radius = s * 0.009f, center = Offset(s * hx, t * 0.79f))
    }
    // The felt pressure pad in the middle of the head opening, and the two side openings.
    drawRoundRect(
        color = Color(0xFF17120D),
        topLeft = Offset(s * 0.465f, t * 0.885f),
        size = Size(s * 0.07f, t * 0.06f),
        cornerRadius = CornerRadius(s * 0.006f, s * 0.006f),
    )
    drawRoundRect(
        color = Color(0xFFCDBB98),
        topLeft = Offset(s * 0.478f, t * 0.9f),
        size = Size(s * 0.044f, t * 0.03f),
        cornerRadius = CornerRadius(s * 0.004f, s * 0.004f),
    )
    for (ox in listOf(0.335f, 0.635f)) {
        drawRoundRect(
            color = Color(0xFF17120D),
            topLeft = Offset(s * ox, t * 0.9f),
            size = Size(s * 0.03f, t * 0.045f),
            cornerRadius = CornerRadius(s * 0.004f, s * 0.004f),
        )
    }

    /* --- soft diagonal sheen over the whole shell (plastic gloss) --- */
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(s * 0.9f, t),
        ),
        topLeft = Offset(s * 0.005f, 0f),
        size = Size(s * 0.97f, t * 0.955f),
        cornerRadius = CornerRadius(s * 0.045f, s * 0.045f),
    )
}
