/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * RECORD design centrepiece: a spinning vinyl record (album art as the label)
 * with a realistic tonearm modelled on a Technics-style arm — circular gimbal
 * bearing, angled knurled counterweight, anti-skate dial, gently curved chrome
 * tube (no hard S-bend) and a black cartridge headshell with finger lift and
 * stylus. The arm rides the outer-mid grooves while playing and lifts off when
 * paused; the record freezes in place. Drawn in Compose Canvas, no assets;
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

/* ---- layout (fractions of the square stage) ---- */
private const val DISC_CY = 0.56f        // record sits a little lower
private const val DISC_R = 0.38f         // record radius
private const val ARM_BX = 0.80f         // gimbal bearing centre
private const val ARM_BY = 0.16f
private const val ARM_REST_ANGLE = -22f  // CCW swing that lifts the stylus off the record

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
        val discOffsetY = dim * (DISC_CY - 0.5f)

        // Spinning disc (grooves + art label).
        VinylDisc(
            thumbnailUrl = thumbnailUrl,
            fallbackBrush = fallbackBrush,
            modifier = Modifier
                .size(discSize)
                .offset(y = discOffsetY)
                .graphicsLayer { rotationZ = rotation.value },
        )

        // Fixed light sheen over the disc (does NOT rotate — sells the spin).
        Canvas(modifier = Modifier.size(discSize).offset(y = discOffsetY)) {
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

        // Realistic tonearm on top.
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
 * Ultra-real tonearm (reference: Technics-style arm — only the arm, no deck):
 * circular gimbal bearing housing, knurled chrome counterweight angled up-right,
 * anti-skate dial, gently curved chrome tube (single soft bow, no S), black
 * cartridge headshell with finger lift and stylus. Drawn in its playing pose;
 * the whole layer rotates around the bearing to lift off when paused.
 */
private fun DrawScope.drawTonearm() {
    val d = size.minDimension
    val b = Offset(d * ARM_BX, d * ARM_BY)                 // bearing / pivot
    val h = Offset(d * 0.67f, d * 0.75f)                   // stylus point: ~0.70 × disc radius, lower-right grooves
    val q = Offset(d * 0.794f, d * 0.468f)                 // control point → gentle rightward bow

    val chromeTube = Brush.linearGradient(
        listOf(Color(0xFF6E6F75), Color(0xFFEDEEF2), Color(0xFF93949A), Color(0xFF45464C)),
        start = Offset(d * 0.60f, 0f),
        end = Offset(d * 0.86f, 0f),
    )

    /* --- soft shadows first --- */
    // Gimbal shadow.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
            center = b + Offset(d * 0.008f, d * 0.016f),
            radius = d * 0.105f,
        ),
        radius = d * 0.105f,
        center = b + Offset(d * 0.008f, d * 0.016f),
    )
    // Headshell shadow on the record.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent),
            center = h + Offset(-d * 0.018f, d * 0.022f),
            radius = d * 0.055f,
        ),
        radius = d * 0.055f,
        center = h + Offset(-d * 0.018f, d * 0.022f),
    )

    /* --- chrome tube: gentle single curve (outline → metal → specular core) --- */
    val tube = Path().apply {
        moveTo(b.x, b.y)
        quadraticBezierTo(q.x, q.y, h.x, h.y)
    }
    drawPath(tube, Color(0xFF1B1B1E), style = Stroke(width = d * 0.019f, cap = StrokeCap.Round))
    drawPath(tube, brush = chromeTube, style = Stroke(width = d * 0.014f, cap = StrokeCap.Round))
    drawPath(tube, Color.White.copy(alpha = 0.85f), style = Stroke(width = d * 0.0045f, cap = StrokeCap.Round))

    /* --- counterweight: knurled chrome cylinder angled up-right behind the bearing --- */
    val cwAngleDeg = -40f
    val cwDir = Offset(cos(Math.toRadians(cwAngleDeg.toDouble())).toFloat(), sin(Math.toRadians(cwAngleDeg.toDouble())).toFloat())
    val cwC = b + Offset(cwDir.x * d * 0.115f, cwDir.y * d * 0.115f)
    // Stub shaft between bearing and weight.
    drawLine(brush = chromeTube, start = b, end = b + Offset(cwDir.x * d * 0.07f, cwDir.y * d * 0.07f), strokeWidth = d * 0.013f, cap = StrokeCap.Round)
    rotate(degrees = cwAngleDeg, pivot = cwC) {
        // Cylinder body with perpendicular chrome banding.
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF9FA0A6), Color(0xFFF4F5F7), Color(0xFF6B6C72), Color(0xFF3A3B40)),
                startY = cwC.y - d * 0.028f,
                endY = cwC.y + d * 0.028f,
            ),
            topLeft = cwC - Offset(d * 0.043f, d * 0.028f),
            size = Size(d * 0.086f, d * 0.056f),
            cornerRadius = CornerRadius(d * 0.010f, d * 0.010f),
        )
        // Knurling on the outer half.
        for (i in 0..4) {
            val kx = cwC.x + d * 0.004f + i * d * 0.008f
            drawLine(Color.Black.copy(alpha = 0.28f), Offset(kx, cwC.y - d * 0.026f), Offset(kx, cwC.y + d * 0.026f), strokeWidth = d * 0.0025f)
        }
        // End cap + rim light.
        drawRoundRect(
            color = Color(0xFF2C2C30),
            topLeft = cwC + Offset(d * 0.036f, -d * 0.028f),
            size = Size(d * 0.008f, d * 0.056f),
            cornerRadius = CornerRadius(d * 0.004f, d * 0.004f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.14f),
            topLeft = cwC - Offset(d * 0.043f, d * 0.028f),
            size = Size(d * 0.086f, d * 0.056f),
            cornerRadius = CornerRadius(d * 0.010f, d * 0.010f),
            style = Stroke(width = d * 0.0025f),
        )
    }

    /* --- gimbal bearing housing --- */
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF4A4A4F), Color(0xFF1A1A1D), Color(0xFF0D0D0F)),
            center = b - Offset(d * 0.018f, d * 0.018f),
            radius = d * 0.105f,
        ),
        radius = d * 0.075f,
        center = b,
    )
    drawCircle(Color.White.copy(alpha = 0.15f), radius = d * 0.075f, center = b, style = Stroke(width = d * 0.003f))
    drawCircle(Color(0xFF202024), radius = d * 0.048f, center = b)
    drawCircle(Color.White.copy(alpha = 0.08f), radius = d * 0.048f, center = b, style = Stroke(width = d * 0.0025f))
    // Top-left catch light on the housing.
    drawArc(
        color = Color.White.copy(alpha = 0.22f),
        startAngle = -160f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = b - Offset(d * 0.066f, d * 0.066f),
        size = Size(d * 0.132f, d * 0.132f),
        style = Stroke(width = d * 0.005f, cap = StrokeCap.Round),
    )
    // Yoke screws.
    drawCircle(Color(0xFF6E6E74), radius = d * 0.006f, center = b + Offset(-d * 0.030f, 0f))
    drawCircle(Color(0xFF6E6E74), radius = d * 0.006f, center = b + Offset(d * 0.030f, 0f))

    /* --- anti-skate dial --- */
    val dial = b + Offset(d * 0.065f, d * 0.055f)
    drawCircle(Color(0xFF0F0F11), radius = d * 0.021f, center = dial)
    drawCircle(Color.White.copy(alpha = 0.25f), radius = d * 0.021f, center = dial, style = Stroke(width = d * 0.0025f))
    drawLine(Color.White.copy(alpha = 0.6f), dial, dial + Offset(0f, -d * 0.014f), strokeWidth = d * 0.0025f)

    /* --- pivot cap --- */
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFE6E6E8), Color(0xFF77777D)), center = b, radius = d * 0.022f),
        radius = d * 0.016f,
        center = b,
    )
    drawCircle(Color(0xFF232327), radius = d * 0.006f, center = b)

    /* --- headshell: black cartridge along the groove tangent --- */
    // Collar joining tube to headshell.
    drawCircle(brush = chromeTube, radius = d * 0.011f, center = h)
    rotate(degrees = 130f, pivot = h) {
        // Cartridge body.
        drawRoundRect(
            color = Color(0xFF131316),
            topLeft = Offset(h.x, h.y - d * 0.014f),
            size = Size(d * 0.075f, d * 0.028f),
            cornerRadius = CornerRadius(d * 0.007f, d * 0.007f),
        )
        // Metal top strip.
        drawRoundRect(
            brush = chromeTube,
            topLeft = Offset(h.x, h.y - d * 0.014f),
            size = Size(d * 0.075f, d * 0.008f),
            cornerRadius = CornerRadius(d * 0.004f, d * 0.004f),
        )
        // Finger lift.
        drawLine(
            brush = chromeTube,
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
