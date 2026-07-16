/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * RECORD design centrepiece, modelled on the reference mock: the vinyl sits
 * slightly left with a belt pulley at top-left, and a straight tonearm hangs
 * vertically at rest on the right (big counterweight at the top). On play the
 * arm swings in so the stylus lands on the OUTER-MID grooves (never the
 * label), and the record spins; on pause the record freezes and the arm
 * swings back out. Drawn in Compose Canvas — no assets; shared by the player
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

// Layout constants (fractions of the square stage) — tuned to the reference mock.
private const val DISC_CENTER_X = 0.44f   // record sits slightly left
private const val DISC_RADIUS = 0.40f
private const val ARM_X = 0.88f           // arm rests vertically at this x
private const val ARM_PIVOT_Y = 0.16f
private const val ARM_TIP_Y = 0.70f
private const val ARM_PLAY_ANGLE = 20f    // clockwise swing that lands the stylus on outer-mid grooves

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

    // Tonearm: vertical at rest (paused), swings clockwise onto the grooves when playing.
    val armAngle by animateFloatAsState(
        targetValue = if (isPlaying) ARM_PLAY_ANGLE else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 60f),
        label = "tonearm",
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val dim = minOf(maxWidth, maxHeight)
        val discSize = dim * (DISC_RADIUS * 2f)
        val discOffsetX = dim * (DISC_CENTER_X - 0.5f)

        // Static turntable deco behind the disc: belt pulley knob at top-left + belt lines.
        Canvas(modifier = Modifier.size(dim)) {
            val d = size.minDimension
            val knob = Offset(d * 0.10f, d * 0.12f)
            val discC = Offset(d * DISC_CENTER_X, d * 0.5f)
            val r = d * DISC_RADIUS
            // Belt lines to the record edge (upper-left tangents).
            val p1 = Offset(discC.x - r * 0.87f, discC.y - r * 0.50f)
            val p2 = Offset(discC.x - r * 0.50f, discC.y - r * 0.87f)
            drawLine(Color.White.copy(alpha = 0.22f), knob, p1, strokeWidth = d * 0.004f)
            drawLine(Color.White.copy(alpha = 0.22f), knob, p2, strokeWidth = d * 0.004f)
            // Pulley knob.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF57575C), Color(0xFF232326)),
                    center = knob - Offset(d * 0.008f, d * 0.008f),
                    radius = d * 0.05f,
                ),
                radius = d * 0.038f,
                center = knob,
            )
            drawCircle(Color.White.copy(alpha = 0.18f), radius = d * 0.038f, center = knob, style = Stroke(width = d * 0.004f))
            drawCircle(Color(0xFF0E0E10), radius = d * 0.012f, center = knob)
        }

        // Spinning disc (grooves + art label).
        VinylDisc(
            thumbnailUrl = thumbnailUrl,
            fallbackBrush = fallbackBrush,
            modifier = Modifier
                .size(discSize)
                .offset(x = discOffsetX)
                .graphicsLayer { rotationZ = rotation.value },
        )

        // Fixed light sheen over the disc (does NOT rotate — sells the spin).
        Canvas(modifier = Modifier.size(discSize).offset(x = discOffsetX)) {
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

        // Tonearm on top (vertical at rest, swings in while playing).
        Tonearm(
            armAngle = armAngle,
            modifier = Modifier.size(dim),
        )
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
 * Straight tonearm like the reference: big round counterweight at the very top,
 * bearing collar at the pivot, a vertical tube, and an angled headshell at the
 * bottom. Rotation happens around the pivot (top of the tube).
 */
@Composable
private fun Tonearm(
    armAngle: Float,
    modifier: Modifier = Modifier,
) {
    val armMetal = Brush.linearGradient(listOf(Color(0xFFF2F2F4), Color(0xFFB4B4BA), Color(0xFF7C7C82)))
    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = armAngle
            transformOrigin = TransformOrigin(ARM_X, ARM_PIVOT_Y)
        },
    ) {
        val d = size.minDimension
        val pivot = Offset(d * ARM_X, d * ARM_PIVOT_Y)
        val tip = Offset(d * ARM_X, d * ARM_TIP_Y)
        val weight = Offset(d * ARM_X, d * 0.075f)
        val tube = d * 0.015f

        // Short shaft up to the counterweight.
        drawLine(brush = armMetal, start = pivot, end = weight, strokeWidth = tube * 0.9f, cap = StrokeCap.Round)

        // Big counterweight (stacked discs, like the reference).
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF8E8E94), Color(0xFF3F3F44)),
                center = weight - Offset(d * 0.012f, d * 0.012f),
                radius = d * 0.07f,
            ),
            radius = d * 0.052f,
            center = weight,
        )
        drawCircle(Color.White.copy(alpha = 0.25f), radius = d * 0.052f, center = weight, style = Stroke(width = d * 0.004f))
        drawCircle(Color(0xFF232327), radius = d * 0.030f, center = weight)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFD9D9DD), Color(0xFF6E6E74)), center = weight, radius = d * 0.02f),
            radius = d * 0.014f,
            center = weight,
        )

        // Bearing collar at the pivot.
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFE6E6E8), Color(0xFF8A8A90)), center = pivot, radius = d * 0.035f),
            radius = d * 0.026f,
            center = pivot,
        )
        drawCircle(Color(0xFF303034), radius = d * 0.010f, center = pivot)

        // Main vertical tube (dark outline + metal core for depth).
        drawLine(Color(0xFF2A2A2E), pivot, tip, strokeWidth = tube * 1.5f, cap = StrokeCap.Round)
        drawLine(brush = armMetal, start = pivot, end = tip, strokeWidth = tube, cap = StrokeCap.Round)

        // Headshell: dark cartridge block angled down-left, with stylus tip.
        rotate(degrees = -38f, pivot = tip) {
            drawRoundRect(
                color = Color(0xFF26262A),
                topLeft = Offset(tip.x - d * 0.060f, tip.y - d * 0.015f),
                size = Size(d * 0.062f, d * 0.030f),
                cornerRadius = CornerRadius(d * 0.008f, d * 0.008f),
            )
            drawRoundRect(
                brush = armMetal,
                topLeft = Offset(tip.x - d * 0.060f, tip.y - d * 0.015f),
                size = Size(d * 0.062f, d * 0.010f),
                cornerRadius = CornerRadius(d * 0.005f, d * 0.005f),
            )
            // Stylus.
            drawCircle(Color(0xFF111114), radius = d * 0.006f, center = Offset(tip.x - d * 0.052f, tip.y + d * 0.018f))
        }
    }
}
