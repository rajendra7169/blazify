/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0
 */

package com.blazify.music.ui.screens.recognition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
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

/**
 * Music notes drifting up the screen behind the button while it listens: a few, not a shower, and
 * faint, so they are something going on behind what matters rather than something to read.
 */
@Composable
fun FlyingNotes(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "flyingNotes")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(11000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "phase",
    )
    Canvas(modifier) {
        val sway = 22.dp.toPx()
        NOTES.forEach { flight ->
            val along = (phase * flight.laps + flight.start) % 1f
            val side = size.width * flight.across + sway * sin(2f * PI.toFloat() * (along * 2f + flight.start))
            val height = size.height * (1.05f - 1.15f * along)
            val fade = minOf(along / 0.15f, 1f) * minOf((1f - along) / 0.3f, 1f)
            val tall = flight.size.dp.toPx()
            withTransform({
                translate(side - tall / 2f, height)
                rotate(sway * 0.5f * sin(2f * PI.toFloat() * along), Offset(tall / 2f, tall / 2f))
            }) {
                drawNote(tall, color.copy(alpha = fade * 0.42f))
            }
        }
    }
}

/**
 * What came of listening when nothing was found: the wave falls flat, a few notes drop away out of
 * it, and a question mark stays. It plays once and rests, since nothing is happening any more.
 */
@Composable
fun SongNotFoundArt(modifier: Modifier = Modifier) {
    val settle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        settle.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
    }
    val circle = MaterialTheme.colorScheme.surfaceContainerHighest
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.36f
            drawCircle(circle, radius)

            val across = radius * 1.12f
            val barWidth = across / (BAR_SHAPE.size * 2f - 1f)
            val left = center.x - across / 2f
            BAR_SHAPE.forEachIndexed { index, tallness ->
                val height = radius * 1.05f * (tallness + (0.12f - tallness) * settle.value)
                drawRoundRect(
                    color = muted,
                    topLeft = Offset(left + index * barWidth * 2f, center.y - height / 2f),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }

            DROPPED_NOTES.forEach { (side, delay) ->
                val along = ((settle.value - delay) / (1f - delay)).coerceIn(0f, 1f)
                val tall = 22.dp.toPx()
                withTransform({
                    translate(center.x + radius * side - tall / 2f, center.y + radius * 0.3f + size.height * 0.34f * along)
                }) {
                    drawNote(tall, muted.copy(alpha = sin(PI.toFloat() * along) * 0.7f))
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-24).dp, y = 22.dp)
                    .graphicsLayer {
                        scaleX = settle.value
                        scaleY = settle.value
                    }.size(52.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
        ) {
            Text(
                text = "?",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** A note drawn as shapes: one icon stretched to several sizes each frame leaves marks behind it. */
private fun DrawScope.drawNote(tall: Float, color: Color) {
    val stem = tall * 0.09f
    val stemLeft = tall * 0.56f
    drawOval(color, Offset(tall * 0.2f, tall * 0.7f), Size(tall * 0.44f, tall * 0.3f))
    drawRect(color, Offset(stemLeft, tall * 0.08f), Size(stem, tall * 0.78f))
    val flag =
        Path().apply {
            moveTo(stemLeft + stem, tall * 0.08f)
            cubicTo(tall * 0.72f, tall * 0.26f, tall * 0.98f, tall * 0.28f, tall * 0.88f, tall * 0.56f)
            cubicTo(tall * 0.86f, tall * 0.4f, tall * 0.74f, tall * 0.36f, stemLeft + stem, tall * 0.32f)
            close()
        }
    drawPath(flag, color)
}

/** Sparks drawn in from the edges to the middle while the song is being looked up. */
@Composable
fun FindingSparks(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "findingSparks")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "phase",
    )
    Canvas(modifier) {
        val reach = size.minDimension * 0.62f
        val inner = 84.dp.toPx()
        repeat(SPARKS) { spark ->
            val along = (phase + spark * 0.37f) % 1f
            val angle = 2f * PI.toFloat() * (spark / SPARKS.toFloat()) + along * 0.5f
            val distance = inner + (reach - inner) * (1f - along)
            drawCircle(
                color = color.copy(alpha = sin(PI.toFloat() * along) * 0.6f),
                radius = (2.5f + spark % 3).dp.toPx(),
                center = center + Offset(cos(angle) * distance, sin(angle) * distance),
            )
        }
    }
}

private class NoteFlight(val across: Float, val start: Float, val laps: Int, val size: Int)

private val NOTES =
    listOf(
        NoteFlight(0.10f, 0.05f, 1, 26),
        NoteFlight(0.88f, 0.22f, 1, 20),
        NoteFlight(0.26f, 0.41f, 2, 18),
        NoteFlight(0.72f, 0.58f, 1, 28),
        NoteFlight(0.50f, 0.74f, 1, 22),
        NoteFlight(0.05f, 0.87f, 2, 16),
        NoteFlight(0.94f, 0.66f, 2, 18),
        NoteFlight(0.36f, 0.14f, 1, 20),
    )
private val DROPPED_NOTES = listOf(-0.55f to 0.2f, 0.1f to 0.05f, 0.62f to 0.32f)
private const val SPARKS = 9
private const val RIPPLES = 3
private val BAR_SHAPE = listOf(0.42f, 0.68f, 1f, 0.8f, 1f, 0.68f, 0.42f)
