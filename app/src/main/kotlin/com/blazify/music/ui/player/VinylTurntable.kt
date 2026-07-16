/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * RECORD design centrepiece: a spinning vinyl record (album art as the label)
 * with a realistic J-shaped tonearm — circular gimbal bearing with a vertical
 * counterweight cap on top, a straight chrome tube that elbows smoothly onto
 * the record near the bottom, and a black cartridge headshell with finger lift
 * and stylus. Both the record and the arm cast soft drop shadows (light from
 * top-left). The arm rides the outer-mid grooves while playing and lifts off
 * when paused; the record freezes in place. Pure Compose Canvas, no assets;
 * shared by the player and the design-preview so they always match.
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/* ---- layout (fractions of the square stage) ---- */
private const val DISC_CX = 0.46f        // record slightly left — gives the arm its lane
private const val DISC_CY = 0.56f        // and a little lower
private const val DISC_R = 0.38f
private const val ARM_BX = 0.84f         // gimbal bearing centre
private const val ARM_BY = 0.155f
private const val ARM_REST_ANGLE = -16f  // CCW swing that lifts the stylus off the record

@Composable
fun VinylTurntable(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    fallbackBrush: Brush? = null,
) {
    // Continuous rotation while playing; freezes (keeps its angle) when paused.
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 8000, easing = LinearEasing),
            )
        }
    }

    // Tonearm: on the outer-mid grooves while playing (0°), lifts off when paused.
    val armAngle by animateFloatAsState(
        targetValue = if (isPlaying) 0f else ARM_REST_ANGLE,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 60f),
        label = "tonearm",
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val dim = minOf(maxWidth, maxHeight)
        val discSize = dim * (DISC_R * 2f)
        val discOffsetX = dim * (DISC_CX - 0.5f)
        val discOffsetY = dim * (DISC_CY - 0.5f)

        // Drop shadow under the record (light from top-left).
        Canvas(modifier = Modifier.size(dim)) {
            val d = size.minDimension
            val c = Offset(d * DISC_CX + d * 0.020f, d * DISC_CY + d * 0.030f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.18f), Color.Transparent),
                    center = c,
                    radius = d * DISC_R * 1.12f,
                ),
                radius = d * DISC_R * 1.12f,
                center = c,
            )
        }

        // Spinning disc (grooves + art label).
        VinylDisc(
            thumbnailUrl = thumbnailUrl,
            fallbackBrush = fallbackBrush,
            modifier = Modifier
                .size(discSize)
                .offset(x = discOffsetX, y = discOffsetY)
                .graphicsLayer { rotationZ = rotation.value },
        )

        // Fixed light sheen over the disc (does NOT rotate — sells the spin).
        Canvas(modifier = Modifier.size(discSize).offset(x = discOffsetX, y = discOffsetY)) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
                radius = size.minDimension / 2f,
            )
        }

        // Realistic J-shaped tonearm on top (casts its own shadow).
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

@Composable
private fun VinylDisc(
    thumbnailUrl: String?,
    fallbackBrush: Brush?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            // Matte-black disc with a soft radial sheen.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3A3A3D), Color(0xFF141416), Color(0xFF060607)),
                    center = Offset(c.x - r * 0.25f, c.y - r * 0.25f),
                    radius = r * 1.3f,
                ),
                radius = r,
                center = c,
            )
            // Concentric grooves.
            var gr = r * 0.46f
            while (gr < r * 0.97f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = gr,
                    center = c,
                    style = Stroke(width = 1.2f),
                )
                gr += r * 0.016f
            }
            // Brighter track-separator rings (like real pressings).
            for (sep in listOf(0.56f, 0.72f, 0.88f)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = r * sep,
                    center = c,
                    style = Stroke(width = 1.6f),
                )
            }
            // Outer rim highlight.
            drawCircle(color = Color.White.copy(alpha = 0.12f), radius = r * 0.99f, center = c, style = Stroke(width = 2f))
        }

        // Album art as the center label.
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(0.42f).clip(CircleShape),
            )
        } else if (fallbackBrush != null) {
            Box(Modifier.fillMaxSize(0.42f).clip(CircleShape).background(fallbackBrush))
        }

        // Label ring + spindle hole.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = r * 0.42f, center = c, style = Stroke(width = 2f))
            drawCircle(color = Color(0xFF0B0B0C), radius = r * 0.035f, center = c)
            drawCircle(color = Color(0xFF2A2A2C), radius = r * 0.018f, center = c)
        }
    }
}

/**
 * J-shaped tonearm like the reference: straight chrome tube hanging from the
 * gimbal, one smooth elbow near the bottom turning onto the record, vertical
 * counterweight cap stacked on top of the bearing, black cartridge headshell.
 * Drawn in its playing pose; the layer rotates around the bearing when paused.
 */
private fun DrawScope.drawTonearm() {
    val d = size.minDimension
    val b = Offset(d * ARM_BX, d * ARM_BY)     // bearing / pivot
    val e = Offset(d * ARM_BX, d * 0.60f)      // elbow start (tube is straight until here)
    val h = Offset(d * 0.70f, d * 0.75f)       // stylus point: ~0.74 × disc radius, lower-right grooves

    val chrome = Brush.linearGradient(
        listOf(Color(0xFF6E6F75), Color(0xFFEDEEF2), Color(0xFF93949A), Color(0xFF45464C)),
        start = Offset(d * 0.66f, 0f),
        end = Offset(d * 0.90f, 0f),
    )

    // The J-arm path: straight down, then one smooth elbow onto the record.
    val tube = Path().apply {
        moveTo(b.x, b.y)
        lineTo(e.x, e.y)
        cubicTo(d * ARM_BX, d * 0.70f, d * 0.76f, d * 0.71f, h.x, h.y)
    }

    /* --- shadows first (light from top-left → shadows fall down-right) --- */
    translate(left = d * 0.014f, top = d * 0.022f) {
        // Soft, wide pass then a tighter darker pass.
        drawPath(tube, Color.Black.copy(alpha = 0.10f), style = Stroke(width = d * 0.036f, cap = StrokeCap.Round))
        drawPath(tube, Color.Black.copy(alpha = 0.20f), style = Stroke(width = d * 0.020f, cap = StrokeCap.Round))
        // Gimbal + counterweight shadow.
        drawCircle(Color.Black.copy(alpha = 0.22f), radius = d * 0.078f, center = b)
        // Headshell shadow blob on the record.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent),
                center = h,
                radius = d * 0.055f,
            ),
            radius = d * 0.055f,
            center = h,
        )
    }

    /* --- chrome tube: outline → metal → bright specular core --- */
    drawPath(tube, Color(0xFF1B1B1E), style = Stroke(width = d * 0.019f, cap = StrokeCap.Round))
    drawPath(tube, brush = chrome, style = Stroke(width = d * 0.014f, cap = StrokeCap.Round))
    drawPath(tube, Color.White.copy(alpha = 0.85f), style = Stroke(width = d * 0.0045f, cap = StrokeCap.Round))

    /* --- counterweight cap stacked vertically on top of the bearing --- */
    // Stub between bearing and weight.
    drawLine(brush = chrome, start = b, end = Offset(b.x, d * 0.10f), strokeWidth = d * 0.013f, cap = StrokeCap.Round)
    // Cylinder body (horizontal chrome banding = vertical cylinder).
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF9FA0A6), Color(0xFFF4F5F7), Color(0xFF6B6C72), Color(0xFF3A3B40)),
            startX = b.x - d * 0.026f,
            endX = b.x + d * 0.026f,
        ),
        topLeft = Offset(b.x - d * 0.026f, d * 0.035f),
        size = Size(d * 0.052f, d * 0.065f),
        cornerRadius = CornerRadius(d * 0.010f, d * 0.010f),
    )
    // Knurling.
    for (i in 0..3) {
        val ky = d * 0.043f + i * d * 0.012f
        drawLine(Color.Black.copy(alpha = 0.25f), Offset(b.x - d * 0.024f, ky), Offset(b.x + d * 0.024f, ky), strokeWidth = d * 0.0022f)
    }
    // Dome cap on top.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFEDEEF2), Color(0xFF8A8B91)),
            center = Offset(b.x - d * 0.005f, d * 0.024f),
            radius = d * 0.020f,
        ),
        radius = d * 0.015f,
        center = Offset(b.x, d * 0.028f),
    )

    /* --- gimbal bearing housing --- */
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF4A4A4F), Color(0xFF1A1A1D), Color(0xFF0D0D0F)),
            center = b - Offset(d * 0.018f, d * 0.018f),
            radius = d * 0.105f,
        ),
        radius = d * 0.072f,
        center = b,
    )
    drawCircle(Color.White.copy(alpha = 0.15f), radius = d * 0.072f, center = b, style = Stroke(width = d * 0.003f))
    // Grey centre disc (like the reference).
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFB9BAC0), Color(0xFF7C7D83), Color(0xFF4E4F55)),
            center = b - Offset(d * 0.012f, d * 0.012f),
            radius = d * 0.055f,
        ),
        radius = d * 0.040f,
        center = b,
    )
    drawCircle(Color.Black.copy(alpha = 0.30f), radius = d * 0.040f, center = b, style = Stroke(width = d * 0.0025f))
    // Top-left catch light on the housing.
    drawArc(
        color = Color.White.copy(alpha = 0.22f),
        startAngle = -160f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = b - Offset(d * 0.063f, d * 0.063f),
        size = Size(d * 0.126f, d * 0.126f),
        style = Stroke(width = d * 0.005f, cap = StrokeCap.Round),
    )
    // Pivot cap.
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFE6E6E8), Color(0xFF77777D)), center = b, radius = d * 0.018f),
        radius = d * 0.013f,
        center = b,
    )
    drawCircle(Color(0xFF232327), radius = d * 0.005f, center = b)

    /* --- headshell: black cartridge along the elbow's tangent (~146°) --- */
    drawCircle(brush = chrome, radius = d * 0.011f, center = h)
    rotate(degrees = 146f, pivot = h) {
        // Cartridge body.
        drawRoundRect(
            color = Color(0xFF131316),
            topLeft = Offset(h.x, h.y - d * 0.014f),
            size = Size(d * 0.075f, d * 0.028f),
            cornerRadius = CornerRadius(d * 0.007f, d * 0.007f),
        )
        // Metal top strip.
        drawRoundRect(
            brush = chrome,
            topLeft = Offset(h.x, h.y - d * 0.014f),
            size = Size(d * 0.075f, d * 0.008f),
            cornerRadius = CornerRadius(d * 0.004f, d * 0.004f),
        )
        // Finger lift.
        drawLine(
            brush = chrome,
            start = Offset(h.x + d * 0.008f, h.y - d * 0.012f),
            end = Offset(h.x - d * 0.010f, h.y - d * 0.026f),
            strokeWidth = d * 0.0045f,
            cap = StrokeCap.Round,
        )
        // Stylus + glint.
        drawCircle(Color(0xFF0A0A0C), radius = d * 0.0055f, center = Offset(h.x + d * 0.064f, h.y + d * 0.017f))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = d * 0.002f, center = Offset(h.x + d * 0.063f, h.y + d * 0.015f))
    }
}
