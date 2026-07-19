/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blazify.music.R
import kotlin.math.roundToInt

/**
 * Free colour picker: a saturation/value field under a hue rail, with a hex box
 * for typing an exact value.
 *
 * State is kept as HSV rather than a packed colour so that dragging through
 * black or white doesn't lose the hue the user had chosen.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    val hex = remember(color) { "%06X".format(color.toArgb() and 0xFFFFFF) }
    // Typing is tracked separately so a half-finished hex doesn't fight the sliders.
    var hexInput by remember(hex) { mutableStateOf(hex) }

    DefaultDialog(
        onDismiss = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(onClick = { onConfirm(color) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Saturation (x) against value (y), tinted by the current hue.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f)
                    .clip(RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            saturation = (pos.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        }
                    },
            ) {
                val pureHue = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                // White→hue horizontally, then black overlaid vertically.
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.horizontalGradient(listOf(Color.White, pureHue))),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))),
                )
                // Crosshair
                Canvas(Modifier.matchParentSize()) {
                    val cx = saturation * size.width
                    val cy = (1f - value) * size.height
                    drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    drawCircle(Color.Black.copy(alpha = 0.5f), radius = 11.dp.toPx(), center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                }
            }

            // Hue rail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            (0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) },
                        ),
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            hue = ((pos.x / size.width).coerceIn(0f, 1f)) * 360f
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            hue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                        }
                    },
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val cx = (hue / 360f) * size.width
                    drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(cx, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()))
                }
            }

            // Preview + hex entry
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.removePrefix("#").filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                        hexInput = cleaned
                        if (cleaned.length == 6) {
                            val parsed = cleaned.toLongOrNull(16)
                            if (parsed != null) {
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed.toInt() or (0xFF shl 24), hsv)
                                hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                            }
                        }
                    },
                    prefix = { Text("#") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    label = { Text(stringResource(R.string.custom_color_hex)) },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(
                    R.string.custom_color_hsv,
                    hue.roundToInt(),
                    (saturation * 100).roundToInt(),
                    (value * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}
