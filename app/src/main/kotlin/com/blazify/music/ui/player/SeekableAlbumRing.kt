/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Circular album art wrapped by a real, seekable progress ring — the "CD-player"
 * player design. Tap or drag anywhere around the ring to scrub. Shared by the
 * live player (Player.kt) and the design-preview gallery so both behave the same.
 */

package com.blazify.music.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.atan2

/** Convert a touch point (relative to a square's box) into a 0..1 fraction, 0 = top, clockwise. */
private fun angleFraction(x: Float, y: Float, width: Int, height: Int): Float {
    val angle = atan2((y - height / 2f).toDouble(), (x - width / 2f).toDouble()) * 180.0 / PI
    return (((angle + 90.0 + 360.0) % 360.0) / 360.0).toFloat()
}

@Composable
fun SeekableAlbumRing(
    thumbnailUrl: String?,
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    ringStrokeDp: Float = 8f,
    artPaddingDp: Float = 16f,
    fallbackBrush: Brush? = null,
    thumbColor: Color? = null,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shown = (dragFraction ?: progress).coerceIn(0f, 1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Album art (circular)
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(artPaddingDp.dp)
                        .clip(CircleShape),
            )
        } else if (fallbackBrush != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(artPaddingDp.dp)
                        .clip(CircleShape)
                        .background(fallbackBrush),
            )
        }

        // Progress ring + touch handling
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos -> dragFraction = angleFraction(pos.x, pos.y, size.width, size.height) },
                            onDrag = { change, _ ->
                                dragFraction = angleFraction(change.position.x, change.position.y, size.width, size.height)
                            },
                            onDragEnd = { dragFraction?.let(onSeek); dragFraction = null },
                            onDragCancel = { dragFraction = null },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            onSeek(angleFraction(pos.x, pos.y, size.width, size.height))
                        }
                    },
        ) {
            val stroke = ringStrokeDp.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * shown,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (thumbColor != null) {
                val angleRad = (-90.0 + 360.0 * shown) * PI / 180.0
                val r = d / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val knob = Offset(
                    (cx + r * kotlin.math.cos(angleRad)).toFloat(),
                    (cy + r * kotlin.math.sin(angleRad)).toFloat(),
                )
                drawCircle(color = Color.White, radius = stroke * 0.9f, center = knob)
                drawCircle(color = thumbColor, radius = stroke * 0.6f, center = knob)
            }
        }
    }
}
