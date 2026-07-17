/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.blazify.music.R
import com.blazify.music.lyrics.LyricsEntry
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

sealed class LyricsListItem {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItem()
    data class Indicator(
        val afterLineIndex: Int,
        val gapMs: Long,
        val gapStartMs: Long,
        val gapEndMs: Long,
        val nextAgent: String?
    ) : LyricsListItem()
}

/**
 * Instrumental-break indicator — Blazify's own composition: the Blaze logo that
 * breathes and fills bottom-up with a white→accent gradient as the interlude
 * progresses (like the flame lighting up), with little music notes rising and
 * fading upward around it. Everything is in the dynamic theme colour.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun IntervalIndicator(
    gapStartMs: Long,
    gapEndMs: Long,
    currentPositionMs: Long,
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val rowHeightPx = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rowHeightPx.animateTo(1f, tween(220))
            alpha.animateTo(1f, tween(220))
        } else {
            alpha.animateTo(0f, tween(200))
            rowHeightPx.animateTo(0f, tween(200))
        }
    }

    val targetHeightDp = 132.dp

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "intervalProgress"
    )

    val transition = rememberInfiniteTransition(label = "interludeAnim")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing)),
        label = "interludePhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(targetHeightDp * rowHeightPx.value)
            .padding(top = 16.dp * rowHeightPx.value)
            .graphicsLayer {
                this.alpha = alpha.value
                this.clip = true
            },
        contentAlignment = Alignment.Center
    ) {
        FloatingNotes(color = color, t = t)

        // Breathing logo, driven by a slow sine on the loop phase.
        val breathe = 1f + 0.06f * abs(sin(t * PI)).toFloat()
        BlazeLogoFill(
            fill = animatedProgress,
            accent = color,
            scale = breathe,
        )
    }
}

/** Blaze logo: dim base glyph, filled bottom-up with a white→accent gradient. */
@Composable
private fun BlazeLogoFill(
    fill: Float,
    accent: Color,
    scale: Float,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // Dim base glyph.
        Icon(
            painter = painterResource(R.drawable.blaze_logo_white),
            contentDescription = null,
            tint = accent.copy(alpha = 0.26f),
            modifier = Modifier.fillMaxSize()
        )
        // Filled portion: white icon, recoloured with a vertical gradient, clipped to progress.
        // The flame glyph's opaque pixels only span ~16%..77% of the PNG canvas
        // (transparent padding below), so map the fill onto those bounds — otherwise
        // nothing appears until ~25% progress and the fill looks like it starts late.
        val glyphTopFrac = 0.16f
        val glyphBottomFrac = 0.77f
        Icon(
            painter = painterResource(R.drawable.blaze_logo_white),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    val f = fill.coerceIn(0f, 1f)
                    clipRect(top = size.height * (glyphBottomFrac - (glyphBottomFrac - glyphTopFrac) * f)) {
                        this@drawWithContent.drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color.White, accent.copy(alpha = 0.95f)),
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                }
        )
    }
}

private data class FloatingNote(
    val xDp: Float,
    val sizeDp: Float,
    val phase: Float,
    val driftDp: Float,
)

/** Small music notes rising and fading around the clef, continuously and staggered. */
@Composable
private fun BoxScope.FloatingNotes(color: Color, t: Float) {
    val notes = remember {
        listOf(
            FloatingNote(xDp = -78f, sizeDp = 13f, phase = 0.00f, driftDp = -7f),
            FloatingNote(xDp = -50f, sizeDp = 10f, phase = 0.42f, driftDp = 6f),
            FloatingNote(xDp = 52f, sizeDp = 12f, phase = 0.68f, driftDp = 7f),
            FloatingNote(xDp = 82f, sizeDp = 14f, phase = 0.20f, driftDp = -6f),
            FloatingNote(xDp = -26f, sizeDp = 9f, phase = 0.85f, driftDp = 5f),
            FloatingNote(xDp = 30f, sizeDp = 9f, phase = 0.55f, driftDp = -5f),
        )
    }
    notes.forEach { n ->
        val p = (t + n.phase) % 1f
        // Rise from below the clef up past the top, fading in then out — the
        // whole travel stays inside the taller row so nothing gets clipped.
        val y = (30f - 84f * p)
        val x = n.xDp + n.driftDp * sin(p * 2f * PI).toFloat()
        // Ease-in/out fade that reaches zero at both ends so notes appear and
        // vanish softly instead of being cut.
        val a = (sin(p * PI).toFloat()) * (sin(p * PI).toFloat()) * 0.55f
        Icon(
            painter = painterResource(R.drawable.music_note),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = x.dp, y = y.dp)
                .size(n.sizeDp.dp)
                .graphicsLayer {
                    this.alpha = a
                    rotationZ = 8f * sin((p + n.phase) * 2f * PI).toFloat()
                }
        )
    }
}
