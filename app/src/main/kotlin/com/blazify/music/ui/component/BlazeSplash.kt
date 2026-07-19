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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.R
import com.blazify.music.ui.theme.BlazeGradientEnd
import com.blazify.music.ui.theme.BlazeThemeColor
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    onIntroFinished: () -> Unit = {},
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
            // Intro is done and the main thread is still free — this is the cue to
            // start composing the app underneath, where its startup jank is hidden.
            onIntroFinished()
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
            // Music glyphs drifting around the logo — a spinning sheen said nothing
            // about the app, and this reads as "music" straight away.
            SplashNotes(phase = shimmer, alpha = logoAlpha.value)

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

/** One drifting glyph: where it sits, how big, and how far out of step it is. */
private data class SplashNote(
    val iconRes: Int,
    /** Position on the ring, in degrees clockwise from 12 o'clock. */
    val angle: Float,
    val radius: Dp,
    val size: Dp,
    /** Offset into the shared cycle, so they don't all bob together. */
    val phaseShift: Float,
    val maxAlpha: Float,
)

/**
 * Music glyphs arranged on a ring around the logo, each bobbing and fading on its
 * own offset. Driven by one shared [phase] rather than an animation per note, and
 * applied inside graphicsLayer so the motion never triggers recomposition — cold
 * start is already tight on the main thread.
 *
 * @param phase 0..1, looping.
 * @param alpha fades the whole group in with the logo.
 */
@Composable
private fun SplashNotes(phase: Float, alpha: Float) {
    val notes = remember {
        listOf(
            // Angles avoid the bottom of the ring (roughly 130°–230°), where the
            // wordmark and progress bar sit — notes there collided with them.
            SplashNote(R.drawable.music_note, angle = 26f, radius = 104.dp, size = 22.dp, phaseShift = 0.0f, maxAlpha = 0.85f),
            SplashNote(R.drawable.queue_music, angle = 68f, radius = 128.dp, size = 18.dp, phaseShift = 0.30f, maxAlpha = 0.50f),
            SplashNote(R.drawable.music_note, angle = 108f, radius = 112.dp, size = 15.dp, phaseShift = 0.62f, maxAlpha = 0.45f),
            SplashNote(R.drawable.music_note, angle = 252f, radius = 112.dp, size = 17.dp, phaseShift = 0.18f, maxAlpha = 0.55f),
            SplashNote(R.drawable.graphic_eq, angle = 292f, radius = 130.dp, size = 20.dp, phaseShift = 0.78f, maxAlpha = 0.50f),
            SplashNote(R.drawable.music_note, angle = 334f, radius = 100.dp, size = 16.dp, phaseShift = 0.45f, maxAlpha = 0.70f),
        )
    }
    val density = LocalDensity.current

    notes.forEach { note ->
        // Resolve the ring position once; only the bob and fade change per frame.
        val base = remember(note, density) {
            val radians = Math.toRadians((note.angle - 90f).toDouble())
            with(density) {
                (note.radius.toPx() * cos(radians).toFloat()) to
                    (note.radius.toPx() * sin(radians).toFloat())
            }
        }
        val bobRange = with(density) { 7.dp.toPx() }

        Image(
            painter = painterResource(note.iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(BlazeThemeColor),
            modifier = Modifier
                .size(note.size)
                .graphicsLayer {
                    val local = ((phase + note.phaseShift) % 1f) * 2f * PI.toFloat()
                    translationX = base.first
                    translationY = base.second + sin(local) * bobRange
                    // Breathe opacity and scale together so each note feels alive
                    // without pulling attention from the logo.
                    val breath = 0.5f + 0.5f * sin(local)
                    this.alpha = alpha * note.maxAlpha * (0.45f + 0.55f * breath)
                    val s = 0.9f + 0.12f * breath
                    scaleX = s
                    scaleY = s
                },
        )
    }
}
