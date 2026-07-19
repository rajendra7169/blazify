/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component.shimmer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.defaultShimmerTheme
import com.valentinilk.shimmer.shimmer

@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    showGradient: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val baseModifier = modifier
        .shimmer()
        .graphicsLayer(alpha = 0.99f)

    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        modifier = if (showGradient) {
            baseModifier.drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent)),
                    blendMode = BlendMode.DstIn,
                )
            }
        } else {
            baseModifier
        },
        content = content,
    )
}

/**
 * Blazify's own skeleton tone: a soft, warm block that sits quietly under the
 * content instead of the harsh solid bar the stock placeholders used. Every
 * placeholder shares this so loading states look the same everywhere.
 */
val skeletonBlockColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)

/** Corner rounding shared by all skeleton blocks — softer than square placeholders. */
val SkeletonCorner = 10.dp

/**
 * Our shimmer: a slower, gentler diagonal sweep with a wide soft highlight,
 * rather than the stock library's quick bright flash.
 */
val ShimmerTheme =
    defaultShimmerTheme.copy(
        animationSpec =
        infiniteRepeatable(
            animation =
            tween(
                durationMillis = 1500,
                easing = LinearEasing,
                delayMillis = 180,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        rotation = 18f,
        shaderColors =
        listOf(
            Color.Unspecified.copy(alpha = 0.05f),
            Color.Unspecified.copy(alpha = 0.38f),
            Color.Unspecified.copy(alpha = 0.05f),
        ),
        shaderColorStops = listOf(0f, 0.5f, 1f),
    )
