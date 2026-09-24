/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Convert a touch point (relative to a square's box) into a 0..1 fraction along the ring,
 * clockwise from the top. With a gap at the top, the ring starts at the gap's right edge and
 * ends at its left edge; a touch inside the gap counts as the nearer end.
 */
private fun angleFraction(x: Float, y: Float, width: Int, height: Int, gapDegrees: Float = 0f): Float {
    val angle = atan2((y - height / 2f).toDouble(), (x - width / 2f).toDouble()) * 180.0 / PI
    val clockwise = ((angle + 90.0 + 360.0) % 360.0).toFloat()
    if (gapDegrees <= 0f) return clockwise / 360f
    return ((clockwise - gapDegrees / 2f) / (360f - gapDegrees)).coerceIn(0f, 1f)
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
    // Double-tap the artwork inside the ring to jump back or forward.
    onDoubleTapArt: ((forward: Boolean) -> Unit)? = null,
    rtl: Boolean = false,
    // Shown in a gap cut into the top of the ring, e.g. the elapsed and total time. The ring
    // then runs from the gap's right edge round to its left edge.
    topLabel: (@Composable () -> Unit)? = null,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    // The touch handlers below are set up once, so they read the latest callbacks
    // here instead of keeping the ones from the first song they saw.
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestOnDoubleTapArt by rememberUpdatedState(onDoubleTapArt)
    val shown = (dragFraction ?: progress).coerceIn(0f, 1f)

    // The gap is as wide as the label plus a little air (and the round caps of the ring's ends).
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var labelWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val gapDegrees =
        if (topLabel == null || labelWidth == 0 || boxSize.width == 0) {
            0f
        } else {
            with(density) {
                val stroke = ringStrokeDp.dp.toPx()
                val d = minOf(boxSize.width, boxSize.height) - stroke
                ((labelWidth + 12.dp.toPx() + stroke) / (PI.toFloat() * d) * 360f).coerceIn(0f, 120f)
            }
        }
    val latestGap by rememberUpdatedState(gapDegrees)

    Box(modifier = modifier.onSizeChanged { boxSize = it }, contentAlignment = Alignment.Center) {
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
                            onDragStart = { pos -> dragFraction = angleFraction(pos.x, pos.y, size.width, size.height, latestGap) },
                            onDrag = { change, _ ->
                                dragFraction = angleFraction(change.position.x, change.position.y, size.width, size.height, latestGap)
                            },
                            onDragEnd = { dragFraction?.let(latestOnSeek); dragFraction = null },
                            onDragCancel = { dragFraction = null },
                        )
                    }
                    .pointerInput(onDoubleTapArt != null, rtl) {
                        // A tap on the ring jumps to that point. When double-tap seeking is
                        // on, taps on the artwork inside the ring are left for that instead.
                        val artRadius = minOf(size.width, size.height) / 2f - artPaddingDp.dp.toPx()
                        fun onArt(pos: Offset) =
                            hypot(pos.x - size.width / 2f, pos.y - size.height / 2f) < artRadius
                        val artSeeks = onDoubleTapArt != null
                        detectTapGestures(
                            onTap = { pos ->
                                if (!artSeeks || !onArt(pos)) {
                                    latestOnSeek(angleFraction(pos.x, pos.y, size.width, size.height, latestGap))
                                }
                            },
                            onDoubleTap = if (!artSeeks) null else { pos ->
                                if (onArt(pos)) {
                                    latestOnDoubleTapArt?.invoke((pos.x < size.width / 2f) == rtl)
                                } else {
                                    latestOnSeek(angleFraction(pos.x, pos.y, size.width, size.height, latestGap))
                                }
                            },
                        )
                    },
        ) {
            val stroke = ringStrokeDp.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val start = -90f + gapDegrees / 2f
            val span = 360f - gapDegrees
            drawArc(
                color = trackColor,
                startAngle = start,
                sweepAngle = span,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = start,
                sweepAngle = span * shown,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (thumbColor != null) {
                val angleRad = (start + span * shown) * PI / 180.0
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

        if (topLabel != null) {
            val strokePx = with(density) { ringStrokeDp.dp.toPx() }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .onSizeChanged { labelWidth = it.width }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minHeight = 0))
                            layout(placeable.width, placeable.height) {
                                // Sit on the ring's line rather than below it.
                                placeable.place(0, (strokePx / 2f - placeable.height / 2f).roundToInt())
                            }
                        },
            ) { topLabel() }
        }
    }
}
