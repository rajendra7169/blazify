/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Blazify's loading animation: three theme-coloured dots bouncing in a
 * staggered wave with soft reflection shadows underneath (dribbble-style).
 * Colours follow the dynamic scheme (primary / tertiary / secondary) unless an
 * explicit colour is passed. The signature is drop-in compatible with
 * CircularProgressIndicator so existing call sites keep working.
 */

package com.blazify.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun BlazeLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    // Compatibility with CircularProgressIndicator call sites — ignored.
    strokeWidth: Dp = Dp.Unspecified,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val cs = MaterialTheme.colorScheme
    val dotColors =
        if (color.isSpecified) {
            listOf(color, color.copy(alpha = 0.75f), color.copy(alpha = 0.55f))
        } else {
            listOf(cs.primary, cs.tertiary, cs.secondary)
        }

    val transition = rememberInfiniteTransition(label = "blazeLoader")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "phase",
    )

    // Fills whatever size the caller gives; sensible default otherwise.
    Canvas(modifier = modifier.size(44.dp)) {
        val w = size.width
        val h = size.height
        val dotR = minOf(w, h) * 0.11f
        val baseY = h * 0.60f
        val amp = h * 0.26f

        for (i in 0..2) {
            // Staggered bounce: |sin| gives a bouncy up-down with a soft landing.
            val phase = (t + i * 0.18f) % 1f
            val hop = abs(sin(phase * PI)).toFloat()
            val cx = w * (0.22f + 0.28f * i)
            val cy = baseY - amp * hop

            // Reflection shadow: shrinks and fades as the dot rises.
            drawOval(
                color = Color.Black.copy(alpha = 0.20f * (1f - hop * 0.7f)),
                topLeft = Offset(cx - dotR * (1.1f - 0.4f * hop), baseY + dotR * 1.5f),
                size = androidx.compose.ui.geometry.Size(
                    dotR * 2f * (1.1f - 0.4f * hop),
                    dotR * 0.55f,
                ),
            )
            // The dot.
            drawCircle(color = dotColors[i], radius = dotR, center = Offset(cx, cy))
        }
    }
}
