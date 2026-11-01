/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0
 */

package com.blazify.music.ui.screens.recognition

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A sound wave standing where a microphone used to: bars rising and falling, tall in the middle.
 * It says "sound" at a glance without being one more picture of the phone's own hardware.
 * Barely stirring while it waits for a tap, and dancing once it is really listening.
 */
@Composable
fun SoundWaveBars(
    color: Color,
    lively: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "soundWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(if (lively) 900 else 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "phase",
    )
    Canvas(modifier) {
        val count = BAR_SHAPE.size
        val barWidth = size.width / (count * 2f - 1f)
        val swing = if (lively) 0.5f else 0.14f
        BAR_SHAPE.forEachIndexed { index, tallness ->
            val wave = sin(2f * PI.toFloat() * (phase + index * 0.17f))
            val share = (tallness * (1f - swing + swing * wave)).coerceIn(0.14f, 1f)
            val height = size.height * share
            drawRoundRect(
                color = color,
                topLeft = Offset(index * barWidth * 2f, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

/** Rings of sound travelling outward from the button and fading as they go. */
@Composable
fun SoundRipples(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "soundRipples")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "travel",
    )
    Canvas(modifier) {
        val outer = size.minDimension / 2f
        val inner = outer * 0.6f
        repeat(RIPPLES) { ring ->
            val along = (travel + ring.toFloat() / RIPPLES) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - along) * 0.55f),
                radius = inner + (outer - inner) * along,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

private const val RIPPLES = 3
private val BAR_SHAPE = listOf(0.42f, 0.68f, 1f, 0.8f, 1f, 0.68f, 0.42f)
