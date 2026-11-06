/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * The small card that asks for a star on GitHub: a gold star that breathes and glints,
 * one line of why, and three buttons. Kept small on purpose; it is a favour asked,
 * not an announcement.
 */

package com.blazify.music.ui.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.blazify.music.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Amber = Color(0xFFFFA726)
private val Coral = Color(0xFFFF7043)
private val Gold = Color(0xFFFFB300)
private val Glint = Color(0xFFFFE082)
private val ButtonInk = Color(0xFF1A0E00)

@Composable
fun StarPromptDialog(
    onStar: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit,
) {
    val context = LocalContext.current
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    // A short rise as it appears.
    val enter = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 400f)) }
    val breathing = rememberInfiniteTransition(label = "starPrompt")
    val beat by breathing.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "starBeat",
    )
    val pulse = if (still) 0f else beat

    Dialog(onDismissRequest = onLater) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier =
                Modifier
                    .widthIn(max = 320.dp)
                    .graphicsLayer {
                        alpha = enter.value.coerceIn(0f, 1f)
                        val grow = 0.96f + 0.04f * enter.value
                        scaleX = grow
                        scaleY = grow
                        translationY = (1f - enter.value) * 24.dp.toPx()
                    },
        ) {
            Column(Modifier.padding(start = 18.dp, end = 10.dp, top = 18.dp, bottom = 8.dp)) {
                Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Amber.copy(alpha = 0.14f)),
                    ) {
                        Canvas(Modifier.size(24.dp)) {
                            // The star breathes and leans a little; a glint blinks at its tip.
                            val grow = 1f + 0.12f * pulse
                            rotate(-8f * pulse) {
                                scale(grow) {
                                    drawPath(starPath(center, size.minDimension / 2f), Brush.linearGradient(listOf(Glint, Gold, Color(0xFFFF8F00))))
                                }
                            }
                            drawCircle(
                                Glint.copy(alpha = pulse),
                                radius = size.minDimension * 0.11f * (0.4f + 0.6f * pulse),
                                center = Offset(size.width * 0.9f, size.height * 0.08f),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.star_prompt_title_short),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.star_prompt_body_short),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onNever) {
                        Text(stringResource(R.string.star_prompt_never), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onLater) {
                        Text(stringResource(R.string.star_prompt_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(Amber, Coral)))
                                .clickable(onClick = onStar)
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Canvas(Modifier.size(14.dp)) { drawPath(starPath(center, size.minDimension / 2f), ButtonInk) }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.star_prompt_yes),
                            color = ButtonInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

/** A five-pointed star centred on [c] with outer radius [r]. */
private fun starPath(c: Offset, r: Float): Path =
    Path().apply {
        val inner = r * 0.45f
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else inner
            val a = -PI / 2 + i * PI / 5
            val x = c.x + (rad * cos(a)).toFloat()
            val y = c.y + (rad * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
