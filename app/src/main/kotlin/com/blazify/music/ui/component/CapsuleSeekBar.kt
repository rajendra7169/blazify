/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SliderColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.utils.makeTimeString
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val SEEK_STEP_MS = 10_000L

/**
 * Seek bar where the thumb is a capsule carrying the "elapsed / total" readout,
 * flanked by ten-second skip buttons.
 *
 * Everything is driven by [colors], so the fill and the capsule follow whatever
 * accent the dynamic album-art theming is currently using; the capsule's label
 * flips between black and white to stay legible on that accent.
 */
@Composable
fun CapsuleSeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    colors: SliderColors,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
) {
    // While dragging, show where the finger is rather than where playback still is.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction
        ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownPosition = if (dragFraction != null && duration > 0) {
        (dragFraction!! * duration).toLong()
    } else {
        position
    }

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor
    val label = "${makeTimeString(shownPosition)} / ${makeTimeString(duration.coerceAtLeast(0L))}"
    // Pick the label colour from the capsule's own fill, not the player background.
    val labelColor = if (activeColor.luminance() > 0.5f) Color.Black else Color.White

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeekTenButton(
            forward = false,
            color = contentColor,
            enabled = enabled,
            onClick = { onSeek((position - SEEK_STEP_MS).coerceAtLeast(0L)) },
        )
        Spacer(Modifier.width(12.dp))

        val textMeasurer = rememberTextMeasurer()
        val labelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        val density = LocalDensity.current
        // Measure the capsule up front so it can be positioned in the same frame —
        // reacting to onSizeChanged instead would make it jump on first layout.
        val capsuleWidth = remember(label, density) {
            with(density) {
                textMeasurer.measure(label, labelStyle).size.width.toDp() + 24.dp
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(CAPSULE_HEIGHT)
                .then(
                    if (!enabled) Modifier else Modifier
                        .pointerInput(duration) {
                            detectTapGestures { offset ->
                                if (duration > 0) {
                                    onSeek((offset.x / size.width * duration).toLong().coerceIn(0L, duration))
                                }
                            }
                        }
                        .pointerInput(duration) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    dragFraction?.let {
                                        if (duration > 0) onSeek((it * duration).toLong())
                                    }
                                    dragFraction = null
                                },
                                onDragCancel = { dragFraction = null },
                            )
                        }
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackWidth = maxWidth
            // The capsule rides between the track ends rather than overhanging them.
            val travel = (trackWidth - capsuleWidth).coerceAtLeast(0.dp)
            val capsuleStart = travel * fraction

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT)
                    .clip(CircleShape)
                    .background(inactiveColor),
            )
            // Filled portion runs up to the middle of the capsule.
            Box(
                modifier = Modifier
                    .width(capsuleStart + capsuleWidth / 2)
                    .height(TRACK_HEIGHT)
                    .clip(CircleShape)
                    .background(activeColor),
            )
            Box(
                modifier = Modifier
                    .offset(x = capsuleStart)
                    .width(capsuleWidth)
                    .height(CAPSULE_HEIGHT)
                    .clip(CircleShape)
                    .background(activeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = labelStyle.fontSize,
                    fontWeight = labelStyle.fontWeight,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        SeekTenButton(
            forward = true,
            color = contentColor,
            enabled = enabled,
            onClick = {
                val target = position + SEEK_STEP_MS
                onSeek(if (duration > 0) target.coerceAtMost(duration) else target)
            },
        )
    }
}

private val TRACK_HEIGHT = 4.dp
private val CAPSULE_HEIGHT = 26.dp
private val SEEK_BUTTON_SIZE = 26.dp

/**
 * Ten-second skip control: an almost-closed ring with an arrowhead on its top
 * corner and "10" in the middle. The ring is drawn rather than shipped as a
 * vector so the back and forward variants stay a mirrored pair.
 */
@Composable
private fun SeekTenButton(
    forward: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SEEK_BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            // Mirror only the ring; the label must stay the right way round.
            modifier = Modifier
                .size(SEEK_BUTTON_SIZE)
                .graphicsLayer { if (!forward) scaleX = -1f },
        ) {
            val stroke = 1.4.dp.toPx()
            val radius = (min(size.width, size.height) - stroke) / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Gap centred on the top, leaving room for the arrowhead.
            drawArc(
                color = color,
                startAngle = -55f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Arrowhead sitting at the open end, pointing the way the arc travels.
            val theta = Math.toRadians(-55.0)
            val px = cx + radius * cos(theta).toFloat()
            val py = cy + radius * sin(theta).toFloat()
            val tx = -sin(theta).toFloat()
            val ty = cos(theta).toFloat()
            val nx = cos(theta).toFloat()
            val ny = sin(theta).toFloat()
            val a = 3.2.dp.toPx()
            val path = Path().apply {
                moveTo(px + tx * a, py + ty * a)
                lineTo(px - tx * a * 0.3f + nx * a, py - ty * a * 0.3f + ny * a)
                lineTo(px - tx * a * 0.3f - nx * a, py - ty * a * 0.3f - ny * a)
                close()
            }
            drawPath(path, color)
        }
        Text(
            text = "10",
            color = color,
            fontSize = 9.sp,
            lineHeight = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}
