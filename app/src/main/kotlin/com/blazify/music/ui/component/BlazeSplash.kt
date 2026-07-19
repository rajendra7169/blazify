/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.R
import com.blazify.music.ui.theme.BlazeGradientEnd
import com.blazify.music.ui.theme.BlazeThemeColor
import kotlinx.coroutines.delay

/**
 * Cold-start splash: the Blaze glyph settles in over a warm radial glow, the
 * wordmark rises under it, and a slim gradient bar fills before the whole thing
 * fades away into the app. Continues straight on from the branded window
 * background, so the launch reads as one movement.
 */
@Composable
fun BlazeSplash(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)),
        modifier = modifier,
    ) {
        val logoScale = remember { Animatable(0.72f) }
        val logoAlpha = remember { Animatable(0f) }
        val wordmarkAlpha = remember { Animatable(0f) }
        val wordmarkLift = remember { Animatable(18f) }
        val barProgress = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            logoAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(Unit) {
            // Slight overshoot so the glyph lands with some weight.
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        LaunchedEffect(Unit) {
            delay(220)
            wordmarkAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(Unit) {
            delay(220)
            wordmarkLift.animateTo(0f, tween(460, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(Unit) {
            delay(300)
            barProgress.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
        }

        // Slow breathing halo behind the glyph.
        val haloTransition = rememberInfiniteTransition(label = "splashHalo")
        val halo by haloTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "halo",
        )
        val shimmer by haloTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            // Slowly turning conic sheen, so the backdrop has motion of its own.
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer {
                        rotationZ = shimmer * 360f
                        alpha = 0.55f * logoAlpha.value
                    }
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                BlazeThemeColor.copy(alpha = 0.22f),
                                Color.Transparent,
                                BlazeGradientEnd.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )

            // Warm glow so the black isn't flat.
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .graphicsLayer {
                        scaleX = halo
                        scaleY = halo
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(BlazeThemeColor.copy(alpha = 0.20f), Color.Transparent),
                        ),
                        shape = CircleShape,
                    ),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.blaze_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        },
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Blazify",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(BlazeThemeColor, BlazeGradientEnd)),
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = wordmarkAlpha.value
                        translationY = wordmarkLift.value
                    },
                )

                Spacer(Modifier.height(28.dp))

                // Slim progress bar that fills as the app gets ready.
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = barProgress.value
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                alpha = 0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(shimmer * Math.PI).toFloat())
                            }
                            .background(
                                Brush.horizontalGradient(listOf(BlazeThemeColor, BlazeGradientEnd)),
                            ),
                    )
                }
            }
        }
    }
}
