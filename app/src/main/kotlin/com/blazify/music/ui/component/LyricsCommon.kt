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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
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
 * Instrumental-break indicator: Blazify's own treble-clef mark that gently
 * breathes and fills up white from the bottom as the interlude progresses
 * toward the next sung line (dim base in the dynamic theme colour).
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
            rowHeightPx.animateTo(1f, tween(200))
            alpha.animateTo(1f, tween(200))
        } else {
            alpha.animateTo(0f, tween(200))
            rowHeightPx.animateTo(0f, tween(200))
        }
    }

    val targetHeightDp = 72.dp

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "intervalProgress"
    )

    // Continuous staggered bounce so it reads as "loading" through the whole gap.
    val transition = rememberInfiniteTransition(label = "notesBounce")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "notesPhase"
    )

    Box(
        modifier = modifier
            .height(targetHeightDp * rowHeightPx.value)
            .padding(top = 16.dp * rowHeightPx.value)
            .graphicsLayer {
                this.alpha = alpha.value
                this.clip = true
            },
        contentAlignment = Alignment.Center
    ) {
        // Gentle breathing pulse so it reads as "loading" through the whole gap.
        val hop = abs(sin(((t) % 1f) * PI)).toFloat()
        val scale = 1f + 0.08f * hop
        NoteFill(
            fill = animatedProgress,
            baseColor = color.copy(alpha = 0.28f),
            fillColor = Color.White,
            scale = scale,
        )
    }
}

/** The treble-clef mark drawn dim, then filled white bottom-up to [fill] (0..1). */
@Composable
private fun NoteFill(
    fill: Float,
    baseColor: Color,
    fillColor: Color,
    scale: Float,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            painter = painterResource(R.drawable.treble_clef),
            contentDescription = null,
            tint = baseColor,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            painter = painterResource(R.drawable.treble_clef),
            contentDescription = null,
            tint = fillColor,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    // Reveal the filled fraction from the bottom of the glyph upward.
                    clipRect(top = size.height * (1f - fill.coerceIn(0f, 1f))) {
                        this@drawWithContent.drawContent()
                    }
                }
        )
    }
}
