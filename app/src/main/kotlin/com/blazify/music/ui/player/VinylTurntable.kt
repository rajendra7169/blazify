/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * RECORD design centrepiece: a spinning vinyl record (album art as the label)
 * with a tonearm that drops onto the record while playing and swings out when
 * paused. Drawn in Compose Canvas so it needs no assets; shared by the player
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun VinylTurntable(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    fallbackBrush: Brush? = null,
) {
    // Continuous rotation while playing; freezes (keeps angle) when paused.
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

    // Tonearm swings onto the record (0°) when playing, and out (-26°) when paused.
    val armAngle by animateFloatAsState(
        targetValue = if (isPlaying) 0f else -26f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 80f),
        label = "tonearm",
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val dim = minOf(maxWidth, maxHeight)
        val discSize = dim * 0.82f

        // Spinning disc (grooves + art label).
        VinylDisc(
            thumbnailUrl = thumbnailUrl,
            fallbackBrush = fallbackBrush,
            modifier = Modifier
                .size(discSize)
                .graphicsLayer { rotationZ = rotation.value },
        )

        // Fixed light sheen over the disc (does NOT rotate — sells the spin).
        Canvas(modifier = Modifier.size(discSize)) {
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

        // Tonearm on top.
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
            var gr = r * 0.40f
            while (gr < r * 0.98f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = gr,
                    center = c,
                    style = Stroke(width = 1.2f),
                )
                gr += r * 0.018f
            }
            // Outer rim highlight.
            drawCircle(color = Color.White.copy(alpha = 0.10f), radius = r * 0.99f, center = c, style = Stroke(width = 2f))
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

@Composable
private fun Tonearm(
    armAngle: Float,
    modifier: Modifier = Modifier,
) {
    val armMetal = Brush.linearGradient(listOf(Color(0xFFF2F2F4), Color(0xFFB4B4BA), Color(0xFF7C7C82)))
    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = armAngle
            transformOrigin = TransformOrigin(0.82f, 0.14f)
        },
    ) {
        val w = size.width
        val h = size.height
        val pivot = Offset(w * 0.82f, h * 0.14f)
        val elbow = Offset(w * 0.70f, h * 0.30f)
        val head = Offset(w * 0.52f, h * 0.50f)
        val counter = Offset(w * 0.90f, h * 0.05f)
        val armWidth = h * 0.017f

        // Counterweight arm + weight (behind the pivot).
        drawLine(brush = armMetal, start = pivot, end = counter, strokeWidth = h * 0.020f, cap = StrokeCap.Round)
        drawCircle(color = Color(0xFF3C3C40), radius = h * 0.026f, center = counter)
        drawCircle(color = Color(0xFF57575C), radius = h * 0.026f, center = counter, style = Stroke(width = 2f))

        // Main arm (pivot → elbow → headshell).
        drawLine(brush = armMetal, start = pivot, end = elbow, strokeWidth = armWidth, cap = StrokeCap.Round)
        drawLine(brush = armMetal, start = elbow, end = head, strokeWidth = armWidth, cap = StrokeCap.Round)

        // Pivot base (mount).
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFE6E6E8), Color(0xFF8A8A90)), center = pivot, radius = h * 0.06f),
            radius = h * 0.05f,
            center = pivot,
        )
        drawCircle(color = Color(0xFF303034), radius = h * 0.02f, center = pivot)

        // Headshell + stylus.
        val headAngle = Math.toDegrees(Math.atan2((head.y - elbow.y).toDouble(), (head.x - elbow.x).toDouble())).toFloat()
        rotate(degrees = headAngle, pivot = head) {
            drawRoundRect(
                brush = armMetal,
                topLeft = Offset(head.x - h * 0.03f, head.y - h * 0.022f),
                size = androidx.compose.ui.geometry.Size(h * 0.075f, h * 0.044f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.01f, h * 0.01f),
            )
        }
        // Stylus tip on the record.
        drawCircle(color = Color(0xFF111114), radius = h * 0.008f, center = head + Offset(-w * 0.012f, h * 0.018f))
    }
}
