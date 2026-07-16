/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * RECORD design centrepiece, modelled on a Technics-style deck reference:
 * dark brushed deck plate, platter with strobe-dot rim, deep glossy vinyl
 * (album art label) with static light fans, an S-shaped chrome tonearm on a
 * circular gimbal with angled counterweight + anti-skate dial, and deck
 * hardware (badge, glowing power knob, play/pause pad, 33/45 pills, pitch
 * fader, arm rest). The platter spins while playing and freezes on pause;
 * the arm rides the outer-mid grooves while playing and lifts off to its
 * rest when paused. Pure Compose Canvas — no assets; shared by the player
 * and the design-preview so they always match.
 */

package com.blazify.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

/* ---- layout constants (fractions of the centred square stage) ---- */
private const val CX = 0.38f            // platter centre x
private const val CY = 0.53f            // platter centre y
private const val PLATTER_R = 0.345f    // platter radius
private const val VINYL_R = 0.276f      // vinyl radius (0.80 × platter)
private const val ARM_BX = 0.82f        // tonearm gimbal centre x
private const val ARM_BY = 0.18f        // tonearm gimbal centre y
private const val ARM_REST_ANGLE = -26f // swing (CCW) that parks the arm off the record

@Composable
fun VinylTurntable(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    fallbackBrush: Brush? = null,
) {
    // Continuous platter rotation while playing; freezes (keeps angle) on pause.
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 6000, easing = LinearEasing),
            )
        }
    }

    // Tonearm: drawn in its playing pose (stylus on outer-mid grooves);
    // swings CCW off the record onto the arm rest when paused.
    val armAngle by animateFloatAsState(
        targetValue = if (isPlaying) 0f else ARM_REST_ANGLE,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 55f),
        label = "tonearm",
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val dim = minOf(maxWidth, maxHeight)
        val platterSize = dim * (PLATTER_R * 2f)
        val platterOffsetX = dim * (CX - 0.5f)
        val platterOffsetY = dim * (CY - 0.5f)

        // 1) Deck plate + static hardware (badge, knob, buttons, fader, arm rest, platter shadow).
        Canvas(modifier = Modifier.fillMaxSize()) { drawDeck() }

        // 2) Spinning platter (strobe-dot rim) + vinyl + label art.
        Box(
            modifier = Modifier
                .size(platterSize)
                .offset(x = platterOffsetX, y = platterOffsetY)
                .graphicsLayer { rotationZ = rotation.value },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawPlatterAndVinyl() }
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(0.31f).clip(CircleShape),
                )
            } else if (fallbackBrush != null) {
                Box(Modifier.fillMaxSize(0.31f).clip(CircleShape).background(fallbackBrush))
            }
            Canvas(modifier = Modifier.fillMaxSize()) { drawSpindle() }
        }

        // 3) Static light overlay (does NOT rotate — sells the spin): light fans + label glass.
        Canvas(
            modifier = Modifier
                .size(platterSize)
                .offset(x = platterOffsetX, y = platterOffsetY),
        ) { drawVinylSheen() }

        // 4) Tonearm (S-shaped chrome on gimbal), rotating around its bearing.
        Canvas(
            modifier = Modifier
                .size(dim)
                .graphicsLayer {
                    rotationZ = armAngle
                    transformOrigin = TransformOrigin(ARM_BX, ARM_BY)
                },
        ) { drawTonearm() }
    }
}

/* =================== deck plate + hardware =================== */

private fun DrawScope.drawDeck() {
    val w = size.width
    val h = size.height
    val sq = minOf(w, h)
    val ox = (w - sq) / 2f
    val oy = (h - sq) / 2f
    fun p(fx: Float, fy: Float) = Offset(ox + fx * sq, oy + fy * sq)

    // Brushed dark deck plate.
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF37322F), Color(0xFF262220), Color(0xFF1B1817))),
        cornerRadius = CornerRadius(sq * 0.05f, sq * 0.05f),
    )
    // Faint horizontal brushing.
    var by = h * 0.06f
    while (by < h * 0.96f) {
        drawLine(Color.White.copy(alpha = 0.016f), Offset(w * 0.03f, by), Offset(w * 0.97f, by), strokeWidth = 1f)
        by += h * 0.045f
    }
    // Top edge catch-light.
    drawLine(Color.White.copy(alpha = 0.10f), Offset(w * 0.05f, 2f), Offset(w * 0.95f, 2f), strokeWidth = 2f)

    // Platter drop shadow.
    val c = p(CX, CY)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.50f), Color.Transparent),
            center = c + Offset(sq * 0.012f, sq * 0.022f),
            radius = sq * PLATTER_R * 1.10f,
        ),
        radius = sq * PLATTER_R * 1.10f,
        center = c + Offset(sq * 0.012f, sq * 0.022f),
    )

    // Music-note badge (top-left).
    val badge = p(0.085f, 0.095f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFB9B9BE), Color(0xFF6E6E73), Color(0xFF3C3C40)),
            center = badge - Offset(sq * 0.012f, sq * 0.012f),
            radius = sq * 0.055f,
        ),
        radius = sq * 0.042f, center = badge,
    )
    drawCircle(Color(0xFF232326), radius = sq * 0.032f, center = badge)
    drawCircle(Color.White.copy(alpha = 0.20f), radius = sq * 0.042f, center = badge, style = Stroke(width = sq * 0.004f))
    // Tiny note glyph.
    drawCircle(Color.White.copy(alpha = 0.9f), radius = sq * 0.007f, center = badge + Offset(-sq * 0.006f, sq * 0.010f))
    drawLine(
        Color.White.copy(alpha = 0.9f),
        badge + Offset(sq * 0.001f, sq * 0.010f),
        badge + Offset(sq * 0.001f, -sq * 0.012f),
        strokeWidth = sq * 0.004f, cap = StrokeCap.Round,
    )
    drawLine(
        Color.White.copy(alpha = 0.9f),
        badge + Offset(sq * 0.001f, -sq * 0.012f),
        badge + Offset(sq * 0.010f, -sq * 0.008f),
        strokeWidth = sq * 0.004f, cap = StrokeCap.Round,
    )

    // Power knob with red glow (bottom-left).
    val knob = p(0.085f, 0.83f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFF2D20).copy(alpha = 0.32f), Color.Transparent),
            center = knob, radius = sq * 0.085f,
        ),
        radius = sq * 0.085f, center = knob,
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF4A4A4E), Color(0xFF141416)),
            center = knob - Offset(sq * 0.008f, sq * 0.008f), radius = sq * 0.042f,
        ),
        radius = sq * 0.030f, center = knob,
    )
    for (i in 0 until 8) {
        val a = i * (Math.PI / 4.0)
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        drawLine(
            Color.White.copy(alpha = 0.18f),
            knob + dir * (sq * 0.018f), knob + dir * (sq * 0.027f),
            strokeWidth = sq * 0.003f,
        )
    }
    drawCircle(Color(0xFFFF3B30), radius = sq * 0.006f, center = p(0.045f, 0.795f))

    // Play/pause pad (bottom-left corner).
    val padC = p(0.115f, 0.945f)
    val padW = sq * 0.16f
    val padH = sq * 0.055f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF57575C), Color(0xFF232326))),
        topLeft = padC - Offset(padW / 2f, padH / 2f), size = Size(padW, padH),
        cornerRadius = CornerRadius(sq * 0.012f, sq * 0.012f),
    )
    drawRoundRect(
        color = Color(0xFF17171A),
        topLeft = padC - Offset(padW / 2f - sq * 0.006f, padH / 2f - sq * 0.006f),
        size = Size(padW - sq * 0.012f, padH - sq * 0.012f),
        cornerRadius = CornerRadius(sq * 0.010f, sq * 0.010f),
    )
    val tri = Path().apply {
        moveTo(padC.x - sq * 0.030f, padC.y - sq * 0.013f)
        lineTo(padC.x - sq * 0.030f, padC.y + sq * 0.013f)
        lineTo(padC.x - sq * 0.008f, padC.y)
        close()
    }
    drawPath(tri, Color.White.copy(alpha = 0.9f))
    drawLine(Color.White.copy(alpha = 0.9f), padC + Offset(sq * 0.008f, -sq * 0.012f), padC + Offset(sq * 0.008f, sq * 0.012f), strokeWidth = sq * 0.006f)
    drawLine(Color.White.copy(alpha = 0.9f), padC + Offset(sq * 0.022f, -sq * 0.012f), padC + Offset(sq * 0.022f, sq * 0.012f), strokeWidth = sq * 0.006f)

    // 33 / 45 selector pills.
    for ((i, px) in listOf(0.255f, 0.335f).withIndex()) {
        val pc = p(px, 0.945f)
        drawRoundRect(
            color = Color(0xFF141416),
            topLeft = pc - Offset(sq * 0.030f, sq * 0.014f), size = Size(sq * 0.060f, sq * 0.028f),
            cornerRadius = CornerRadius(sq * 0.008f, sq * 0.008f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = pc - Offset(sq * 0.030f, sq * 0.014f), size = Size(sq * 0.060f, sq * 0.028f),
            cornerRadius = CornerRadius(sq * 0.008f, sq * 0.008f),
            style = Stroke(width = sq * 0.003f),
        )
        if (i == 0) drawCircle(Color(0xFFFFA726), radius = sq * 0.006f, center = pc - Offset(sq * 0.016f, 0f))
    }

    // Pitch fader (right edge).
    val fx = ox + 0.935f * sq
    val ft = oy + 0.46f * sq
    val fb = oy + 0.84f * sq
    var ty = ft
    while (ty <= fb) {
        drawLine(Color.White.copy(alpha = 0.22f), Offset(fx - sq * 0.030f, ty), Offset(fx - sq * 0.016f, ty), strokeWidth = sq * 0.0025f)
        ty += sq * 0.038f
    }
    drawRoundRect(
        color = Color(0xFF101012),
        topLeft = Offset(fx - sq * 0.009f, ft), size = Size(sq * 0.018f, fb - ft),
        cornerRadius = CornerRadius(sq * 0.006f, sq * 0.006f),
    )
    val knobY = oy + 0.645f * sq
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFCFCFD4), Color(0xFF6A6A70))),
        topLeft = Offset(fx - sq * 0.026f, knobY - sq * 0.012f), size = Size(sq * 0.052f, sq * 0.024f),
        cornerRadius = CornerRadius(sq * 0.005f, sq * 0.005f),
    )
    drawLine(Color(0xFF232326), Offset(fx - sq * 0.026f, knobY), Offset(fx + sq * 0.026f, knobY), strokeWidth = sq * 0.004f)
    drawCircle(Color(0xFF74E934), radius = sq * 0.005f, center = Offset(fx + sq * 0.036f, knobY))

    // Tonearm rest post (bottom-right of the platter, where the arm parks).
    val rest = p(0.765f, 0.735f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF6E6E74), Color(0xFF2E2E32))),
        topLeft = rest - Offset(sq * 0.006f, sq * 0.024f), size = Size(sq * 0.012f, sq * 0.034f),
        cornerRadius = CornerRadius(sq * 0.004f, sq * 0.004f),
    )
    drawCircle(Color(0xFF3A3A3E), radius = sq * 0.012f, center = rest + Offset(0f, sq * 0.012f))
}

/* =================== platter + vinyl (rotating) =================== */

private fun DrawScope.drawPlatterAndVinyl() {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f

    // Platter body (dark metal).
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF2C2927), Color(0xFF191716), Color(0xFF100E0D)),
            center = c - Offset(r * 0.2f, r * 0.2f), radius = r * 1.4f,
        ),
        radius = r, center = c,
    )
    drawCircle(Color.White.copy(alpha = 0.10f), radius = r * 0.995f, center = c, style = Stroke(width = r * 0.008f))

    // Strobe-dot rows on the platter rim.
    for (rowR in listOf(0.955f, 0.91f, 0.865f)) {
        var deg = 0.0
        while (deg < 360.0) {
            val a = Math.toRadians(deg)
            drawCircle(
                color = Color(0xFFD8D8DC).copy(alpha = 0.38f),
                radius = r * 0.0095f,
                center = c + Offset((cos(a) * r * rowR).toFloat(), (sin(a) * r * rowR).toFloat()),
            )
            deg += 4.0
        }
    }

    // Vinyl record.
    val rv = r * 0.80f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF19191B), Color(0xFF0B0B0C), Color(0xFF050506)),
            center = c - Offset(rv * 0.3f, rv * 0.3f), radius = rv * 1.5f,
        ),
        radius = rv, center = c,
    )
    // Fine grooves.
    var gr = rv * 0.45f
    while (gr < rv * 0.97f) {
        drawCircle(Color.White.copy(alpha = 0.035f), radius = gr, center = c, style = Stroke(width = 1.1f))
        gr += rv * 0.014f
    }
    // Track-separator bands.
    for (sep in listOf(0.52f, 0.63f, 0.74f, 0.85f, 0.94f)) {
        drawCircle(Color.White.copy(alpha = 0.07f), radius = rv * sep, center = c, style = Stroke(width = 1.7f))
    }
    // Bevelled edge.
    drawCircle(Color.Black.copy(alpha = 0.55f), radius = rv, center = c, style = Stroke(width = rv * 0.016f))
    drawCircle(Color.White.copy(alpha = 0.13f), radius = rv * 0.992f, center = c, style = Stroke(width = 1.4f))

    // Label backing + dead-wax ring (art image is drawn above by the composable).
    drawCircle(Color(0xFF0E0E10), radius = rv * 0.40f, center = c)
    drawCircle(Color.Black.copy(alpha = 0.5f), radius = rv * 0.405f, center = c, style = Stroke(width = rv * 0.012f))
}

private fun DrawScope.drawSpindle() {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFE8E8EC), Color(0xFF8A8A90), Color(0xFF3F3F44)),
            center = c - Offset(r * 0.008f, r * 0.008f), radius = r * 0.030f,
        ),
        radius = r * 0.022f, center = c,
    )
    drawCircle(Color(0xFF0A0A0C), radius = r * 0.009f, center = c)
}

/* ============== static sheen (non-rotating light) ============== */

private fun DrawScope.drawVinylSheen() {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f
    val rv = r * 0.80f

    // Light fans across the grooves (upper-left + top-right) and a warm glow at the bottom.
    val sweep = Brush.sweepGradient(
        0.00f to Color.Transparent,
        0.20f to Color.Transparent,
        0.25f to Color(0xFFFFB067).copy(alpha = 0.055f),
        0.30f to Color.Transparent,
        0.55f to Color.Transparent,
        0.60f to Color.White.copy(alpha = 0.10f),
        0.65f to Color.White.copy(alpha = 0.03f),
        0.68f to Color.Transparent,
        0.80f to Color.Transparent,
        0.85f to Color.White.copy(alpha = 0.08f),
        0.90f to Color.Transparent,
        1.00f to Color.Transparent,
        center = c,
    )
    val bandMid = (rv * 0.42f + rv) / 2f
    drawCircle(brush = sweep, radius = bandMid, center = c, style = Stroke(width = rv - rv * 0.42f))

    // Glass highlight on the label.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
            center = c + Offset(rv * 0.06f, -rv * 0.12f), radius = rv * 0.16f,
        ),
        radius = rv * 0.16f,
        center = c + Offset(rv * 0.06f, -rv * 0.12f),
    )
}

/* =================== S-shaped chrome tonearm =================== */

private fun DrawScope.drawTonearm() {
    val d = size.minDimension
    val b = Offset(d * ARM_BX, d * ARM_BY)
    val chrome = Brush.linearGradient(
        listOf(Color(0xFF6E6E74), Color(0xFFF6F6F8), Color(0xFF9C9CA2), Color(0xFF4A4A50)),
        start = Offset(d * 0.45f, 0f),
        end = Offset(d * 0.95f, 0f),
    )

    // Shadow under the gimbal assembly.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent),
            center = b + Offset(d * 0.010f, d * 0.018f), radius = d * 0.115f,
        ),
        radius = d * 0.115f, center = b + Offset(d * 0.010f, d * 0.018f),
    )

    // Gimbal housing (big dark circle).
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF46464B), Color(0xFF1B1B1E), Color(0xFF0E0E10)),
            center = b - Offset(d * 0.02f, d * 0.02f), radius = d * 0.12f,
        ),
        radius = d * 0.085f, center = b,
    )
    drawCircle(Color.White.copy(alpha = 0.14f), radius = d * 0.085f, center = b, style = Stroke(width = d * 0.004f))
    drawCircle(Color(0xFF19191C), radius = d * 0.055f, center = b)
    drawCircle(Color.White.copy(alpha = 0.08f), radius = d * 0.055f, center = b, style = Stroke(width = d * 0.003f))

    // Counterweight — cylinder angled up-right from the bearing.
    val cwDir = Offset(cos(-0.7).toFloat(), sin(-0.7).toFloat()) // ≈ 40° up-right
    val cwC = b + cwDir * (d * 0.125f)
    drawLine(brush = chrome, start = b, end = b + cwDir * (d * 0.085f), strokeWidth = d * 0.016f, cap = StrokeCap.Round)
    rotate(degrees = -40f, pivot = cwC) {
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF8A8A90), Color(0xFF2E2E32), Color(0xFF6A6A70))),
            topLeft = cwC - Offset(d * 0.040f, d * 0.026f), size = Size(d * 0.080f, d * 0.052f),
            cornerRadius = CornerRadius(d * 0.012f, d * 0.012f),
        )
        // Knurling.
        for (i in 1..4) {
            val kx = cwC.x - d * 0.040f + i * d * 0.016f
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(kx, cwC.y - d * 0.024f), Offset(kx, cwC.y + d * 0.024f), strokeWidth = d * 0.003f)
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = cwC - Offset(d * 0.040f, d * 0.026f), size = Size(d * 0.080f, d * 0.052f),
            cornerRadius = CornerRadius(d * 0.012f, d * 0.012f),
            style = Stroke(width = d * 0.003f),
        )
    }

    // Anti-skate dial.
    val dial = b + Offset(d * 0.075f, d * 0.062f)
    drawCircle(Color(0xFF0F0F11), radius = d * 0.024f, center = dial)
    drawCircle(Color.White.copy(alpha = 0.25f), radius = d * 0.024f, center = dial, style = Stroke(width = d * 0.003f))
    drawLine(Color.White.copy(alpha = 0.6f), dial, dial + Offset(0f, -d * 0.016f), strokeWidth = d * 0.003f)

    // Pivot cap.
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFE2E2E6), Color(0xFF77777D)), center = b, radius = d * 0.024f),
        radius = d * 0.018f, center = b,
    )
    drawCircle(Color(0xFF232327), radius = d * 0.007f, center = b)

    // S-shaped chrome tube: bearing → bulge right → sweep left-down → headshell.
    val h = Offset(d * 0.55f, d * 0.64f)
    val tube = Path().apply {
        moveTo(b.x, b.y)
        cubicTo(d * 0.88f, d * 0.36f, d * 0.50f, d * 0.36f, h.x, h.y)
    }
    drawPath(tube, Color(0xFF17171A), style = Stroke(width = d * 0.021f, cap = StrokeCap.Round))
    drawPath(tube, brush = chrome, style = Stroke(width = d * 0.015f, cap = StrokeCap.Round))
    drawPath(tube, Color.White.copy(alpha = 0.85f), style = Stroke(width = d * 0.005f, cap = StrokeCap.Round))

    // Headshell: black cartridge angled onto the groove, with finger-lift + stylus.
    rotate(degrees = -52f, pivot = h) {
        drawRoundRect(
            color = Color(0xFF131316),
            topLeft = Offset(h.x - d * 0.085f, h.y - d * 0.015f), size = Size(d * 0.085f, d * 0.030f),
            cornerRadius = CornerRadius(d * 0.008f, d * 0.008f),
        )
        drawRoundRect(
            brush = chrome,
            topLeft = Offset(h.x - d * 0.085f, h.y - d * 0.015f), size = Size(d * 0.085f, d * 0.009f),
            cornerRadius = CornerRadius(d * 0.004f, d * 0.004f),
        )
        // Finger lift.
        drawLine(brush = chrome, start = Offset(h.x, h.y - d * 0.006f), end = Offset(h.x + d * 0.022f, h.y - d * 0.020f), strokeWidth = d * 0.005f, cap = StrokeCap.Round)
        // Stylus + glint.
        drawCircle(Color(0xFF0A0A0C), radius = d * 0.006f, center = Offset(h.x - d * 0.075f, h.y + d * 0.019f))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = d * 0.002f, center = Offset(h.x - d * 0.076f, h.y + d * 0.017f))
    }
}
