/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * RADIO design centrepiece: a vintage tabletop radio. The cabinet takes its colour from
 * the song (the player's dynamic colours), the cover sits in a framed window on the left,
 * a speaker grille fills the right, and across the bottom a backlit tuning dial shows how
 * far the song has gone with its red needle. Tap or drag along the dial to seek.
 *
 * Drawn in four layers so a moving needle never redraws the rest: the cabinet, the cover,
 * the window glass, and the dial on top. Only the dial reads the position, and it reads it
 * while drawing, so the needle moves without recomposing anything. Paused and settled,
 * nothing is drawn at all.
 */

package com.blazify.music.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.blazify.music.R
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.ColorUtils
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import kotlin.math.cos
import kotlin.math.sin

/* ---- geometry, in fractions of the radio's width (heights too, so nothing stretches) ---- */

private const val CAB_BOTTOM = 0.885f
private const val FOOT_BOTTOM = 0.925f

/** Width over height of the cabinet with its feet. */
private const val RADIO_ASPECT = 1f / FOOT_BOTTOM

/** Height of the band above the cabinet that holds the antennas, as a share of the width. */
private const val ANT_H = 0.15f

/** Width over height of the whole radio: antennas, cabinet and feet. */
private const val WHOLE_ASPECT = 1f / (ANT_H + FOOT_BOTTOM)

private const val CAB_CORNER = 0.075f

private const val WIN_L = 0.065f
private const val WIN_T = 0.065f
private const val WIN_S = 0.46f
private const val WIN_CORNER = 0.035f

private const val GR_L = 0.575f
private const val GR_R = 0.935f

private const val DIAL_L = 0.065f
private const val DIAL_R = 0.935f
private const val DIAL_T = 0.585f
private const val DIAL_B = 0.71f

/** Share of the dial's width left empty at each end, outside the 88–108 scale. */
private const val DIAL_PAD = 0.085f

private const val KNOB_Y = 0.795f
private const val KNOB_R = 0.056f
private const val KNOB_LX = 0.155f
private const val KNOB_RX = 0.845f

private const val PLATE_L = 0.3f
private const val PLATE_R = 0.7f
private const val PLATE_T = 0.762f
private const val PLATE_B = 0.828f

private val DialLabels = listOf(88, 92, 96, 100, 104, 108)

/* ---- fixed materials; the cabinet itself is coloured by the song ---- */

private val Walnut = Color(0xFF5B3A24)
private val Ivory = Color(0xFFEFE3C8)
private val IvoryShade = Color(0xFFC9B894)
private val BrassLight = Color(0xFFE6CC8E)
private val BrassDark = Color(0xFFAE8746)
private val Ink = Color(0xFF2E2015)
private val Recess = Color(0xFF140E0A)
private val GlassOffTop = Color(0xFFB5A688)
private val GlassOffBottom = Color(0xFF978A6F)
private val GlassOnTop = Color(0xFFFFEFC6)
private val GlassOnBottom = Color(0xFFF7CF83)
private val Backlight = Color(0xFFFFB74D)
private val NeedleRed = Color(0xFFD7261E)
private val LampOff = Color(0xFF4A2C12)
private val LampOn = Color(0xFFFFB300)

private class RadioColors(
    val top: Color,
    val body: Color,
    val bottom: Color,
    val grille: Color,
    val placeholder: Color,
)

/**
 * The cabinet's colours from the song's colour [seed]: its hue, with the saturation and
 * lightness held in a range that always reads as a painted wooden case, warmed with a little
 * walnut. A grey song gets plain walnut. [dark] lifts it a touch less so it doesn't glow.
 */
private fun radioColors(seed: Color, dark: Boolean): RadioColors {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val grey = hsl[1] < 0.12f
    val hue = if (grey) 26f else hsl[0]
    val sat = if (grey) 0.36f else hsl[1].coerceIn(0.24f, 0.52f)
    val light = if (dark) 0.31f else 0.36f
    fun tone(l: Float, warm: Float) =
        lerp(Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, l.coerceIn(0f, 1f)))), Walnut, warm)
    return RadioColors(
        top = tone(light + 0.09f, 0.16f),
        body = tone(light, 0.22f),
        bottom = tone(light - 0.11f, 0.28f),
        grille = tone(light - 0.04f, 0.2f),
        placeholder = tone(light - 0.16f, 0.1f),
    )
}

/**
 * The radio. [progress] is read only while drawing, so pass a provider that reads the
 * playback position's state: the needle then moves with no recomposition. [onSeek] gets the
 * fraction of the song to jump to (null turns seeking off); [onScrub] follows the finger while
 * the needle is dragged, for the time readout.
 */
@Composable
fun RadioDial(
    isPlaying: Boolean,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    seed: Color = MaterialTheme.colorScheme.primary,
    thumbnailUrl: String? = null,
    onScrub: ((Float) -> Unit)? = null,
    onSeek: ((Float) -> Unit)? = null,
    onPlayPause: (() -> Unit)? = null,
    /** False in a small preview: its pointer still shows the volume, but it can't be turned. */
    volumeKnob: Boolean = true,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colors = remember(seed, dark) { radioColors(seed, dark) }
    val measurer = rememberTextMeasurer(cacheSize = 16)

    // The backlight: on while playing, fading over 300 ms on pause. Settled, it asks for no frames.
    val glow = remember { Animatable(if (isPlaying) 1f else 0f) }
    LaunchedEffect(isPlaying) {
        glow.animateTo(if (isPlaying) 1f else 0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
    }

    // Where the finger holds the needle; read only in the dial's draw.
    var drag by remember { mutableStateOf<Float?>(null) }

    // The right knob is the phone's media volume, the one the side buttons change. Its pointer
    // follows that volume, so it turns when the buttons are pressed too; while a finger turns
    // it, it follows the finger smoothly between the volume's steps.
    val volume = rememberMediaVolume()
    var volumeDrag by remember { mutableStateOf<Float?>(null) }
    val haptics = LocalHapticFeedback.current
    val latestPlayPause by rememberUpdatedState(onPlayPause)
    val latestProgress by rememberUpdatedState(progress)
    val latestSeek by rememberUpdatedState(onSeek)
    val latestScrub by rememberUpdatedState(onScrub)

    // A radio's dial reads left to right in every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = modifier.aspectRatio(WHOLE_ASPECT)) {
            val w = maxWidth

            // 0) The antenna standing on the cabinet's top right: drawn once.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(w * ANT_H)
                    .graphicsLayer()
                    .drawWithCache { buildAntennas() },
            )

            Box(
                Modifier
                    .offset(y = w * ANT_H)
                    .fillMaxWidth()
                    .aspectRatio(RADIO_ASPECT),
            ) {
            // 1) Cabinet, grille, recesses, knob bodies, name plate: drawn once into its own layer.
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .drawWithCache { buildCabinet(colors, measurer) },
            )

            // 2) The cover in its window, corners rounded once as it loads.
            if (thumbnailUrl != null) {
                val context = LocalContext.current
                val cornerPx = with(LocalDensity.current) { (w * WIN_CORNER).toPx() }
                val request =
                    remember(thumbnailUrl, cornerPx) {
                        ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .transformations(RoundedCornersTransformation(cornerPx))
                            .build()
                    }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // The rounding above is only for the cover itself: the picture shown while it
                    // loads (or if it fails) came out square, its corners poking past the window.
                    // The window never moves, so this clip is drawn once, not every frame.
                    modifier =
                        Modifier
                            .offset(x = w * WIN_L, y = w * WIN_T)
                            .size(w * WIN_S)
                            .clip(RoundedCornerShape(w * WIN_CORNER)),
                )
            }

            // 3) The window's glass over the cover: also drawn once.
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .drawWithCache { buildWindowGlass() },
            )

            // 4) The dial, needle, knob marks and lamp: the only layer that moves.
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .drawWithCache {
                        val dial = buildDial(measurer)
                        onDrawBehind {
                            val f = (drag ?: latestProgress()).coerceIn(0f, 1f)
                            dial.draw(this, f, glow.value, (volumeDrag ?: volume.level).coerceIn(0f, 1f))
                        }
                    },
            )

            // The knobs. Each touch area is larger than the knob drawn, so it is easy to hit.
            val knobTouch = maxOf(48.dp, w * KNOB_R * 2.6f)
            val powerLabel = stringResource(R.string.play_pause)
            val volumeLabel = stringResource(R.string.volume)
            if (onPlayPause != null) {
                // Left: the power switch. A tap plays or pauses; the knob turns to on or off.
                Box(
                    Modifier
                        .offset(x = w * KNOB_LX - knobTouch / 2, y = w * KNOB_Y - knobTouch / 2)
                        .size(knobTouch)
                        .semantics {
                            role = Role.Button
                            contentDescription = powerLabel
                        }.pointerInput(Unit) {
                            detectTapGestures {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                latestPlayPause?.invoke()
                            }
                        },
                )
            }
            // Right: the volume. Turned by dragging up or to the right for louder.
            if (volumeKnob) Box(
                Modifier
                    .offset(x = w * KNOB_RX - knobTouch / 2, y = w * KNOB_Y - knobTouch / 2)
                    .size(knobTouch)
                    .semantics {
                        contentDescription = volumeLabel
                        progressBarRangeInfo = ProgressBarRangeInfo(volume.level, 0f..1f, volume.steps)
                        setProgress { target ->
                            volume.set(target)
                            true
                        }
                    }.pointerInput(Unit) {
                        val fullTurn = 140.dp.toPx()
                        detectDragGestures(
                            onDragStart = { volumeDrag = volume.level },
                            onDrag = { change, amount ->
                                change.consume()
                                val next = ((volumeDrag ?: volume.level) + (amount.x - amount.y) / fullTurn).coerceIn(0f, 1f)
                                volumeDrag = next
                                if (volume.set(next)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragEnd = { volumeDrag = null },
                            onDragCancel = { volumeDrag = null },
                        )
                    },
            )

            // Seeking: a touch strip over the dial, a little taller than the glass.
            if (onSeek != null) {
                val stripTop = w * (DIAL_T - 0.035f)
                Box(
                    Modifier
                        .offset(x = w * DIAL_L, y = stripTop)
                        .size(width = w * (DIAL_R - DIAL_L), height = w * (DIAL_B - DIAL_T + 0.07f))
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(latestProgress().coerceIn(0f, 1f), 0f..1f)
                            setProgress { target ->
                                latestSeek?.invoke(target.coerceIn(0f, 1f))
                                true
                            }
                        }.pointerInput(Unit) {
                            detectTapGestures { at ->
                                val f = dialFraction(at.x, size.width.toFloat())
                                latestScrub?.invoke(f)
                                latestSeek?.invoke(f)
                            }
                        }.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { at ->
                                    val f = dialFraction(at.x, size.width.toFloat())
                                    drag = f
                                    latestScrub?.invoke(f)
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val f = dialFraction(change.position.x, size.width.toFloat())
                                    drag = f
                                    latestScrub?.invoke(f)
                                },
                                onDragEnd = {
                                    drag?.let { latestSeek?.invoke(it) }
                                    drag = null
                                },
                                onDragCancel = { drag = null },
                            )
                        },
                )
            }
            }
        }
    }
}

/** Fraction of the song at [x] across a dial strip [width] wide, the empty ends excluded. */
internal fun dialFraction(
    x: Float,
    width: Float,
): Float {
    if (width <= 0f) return 0f
    return ((x / width - DIAL_PAD) / (1f - 2f * DIAL_PAD)).coerceIn(0f, 1f)
}

/* ---------------------------------------------------------------------------------------- */
/* Layer 1: the cabinet                                                                      */
/* ---------------------------------------------------------------------------------------- */

private fun CacheDrawScope.buildCabinet(
    c: RadioColors,
    measurer: TextMeasurer,
): DrawResult {
    val s = size.width
    val cabH = s * CAB_BOTTOM
    val cabCorner = CornerRadius(s * CAB_CORNER)

    // Speaker grille: punched holes in staggered rows, kept as two point lists (light rim, hole).
    val grT = s * WIN_T
    val grB = s * (WIN_T + WIN_S)
    val grL = s * GR_L
    val grR = s * GR_R
    val pitch = (grR - grL) / 10.5f
    val hole = pitch * 0.52f
    val holes = ArrayList<Offset>()
    run {
        var row = 0
        var y = grT + pitch * 0.75f
        while (y < grB - pitch * 0.45f) {
            val shift = if (row % 2 == 0) pitch * 0.5f else pitch
            var x = grL + shift
            while (x < grR - pitch * 0.45f) {
                holes += Offset(x, y)
                x += pitch
            }
            y += pitch * 0.87f
            row++
        }
    }
    val rims = holes.map { it + Offset(hole * 0.16f, hole * 0.2f) }

    // Faint grain across the case, a few long gentle waves.
    val grain = Path().apply {
        for (i in 0 until 7) {
            val y = cabH * (0.08f + i * 0.13f)
            moveTo(s * 0.03f, y)
            cubicTo(s * 0.3f, y - s * 0.012f, s * 0.6f, y + s * 0.014f, s * 0.97f, y - s * 0.004f)
        }
    }

    // Knob knurling: short ticks round each rim.
    val knurl = Path().apply {
        for (cx in listOf(KNOB_LX, KNOB_RX)) {
            val center = Offset(s * cx, s * KNOB_Y)
            val r = s * KNOB_R
            for (i in 0 until 28) {
                val a = i * (2 * Math.PI / 28)
                val dx = cos(a).toFloat()
                val dy = sin(a).toFloat()
                moveTo(center.x + dx * r * 0.86f, center.y + dy * r * 0.86f)
                lineTo(center.x + dx * r * 0.99f, center.y + dy * r * 0.99f)
            }
        }
    }

    val plateText =
        measurer.measure(
            "BLAZE · FM",
            style = TextStyle(
                color = Ink.copy(alpha = 0.85f),
                fontSize = (s * 0.03f).toSp(),
                fontWeight = FontWeight.Bold,
                letterSpacing = (s * 0.004f).toSp(),
            ),
        )

    return onDrawBehind {
        /* soft layered shadow below the case */
        for ((grow, alpha) in listOf(0.03f to 0.07f, 0.018f to 0.12f, 0.008f to 0.18f)) {
            drawRoundRect(
                color = Color.Black.copy(alpha = alpha),
                topLeft = Offset(s * (0.02f - grow), s * (0.03f)),
                size = Size(s * (0.96f + grow * 2f), cabH - s * 0.01f + s * grow),
                cornerRadius = CornerRadius(s * (CAB_CORNER + grow)),
            )
        }

        /* feet */
        for (fx in listOf(0.13f, 0.79f)) {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF241810), Color(0xFF0E0906)), startY = cabH, endY = s * FOOT_BOTTOM),
                topLeft = Offset(s * fx, cabH - s * 0.02f),
                size = Size(s * 0.08f, s * (FOOT_BOTTOM - CAB_BOTTOM) + s * 0.02f),
                cornerRadius = CornerRadius(s * 0.015f),
            )
        }

        /* the case */
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(c.top, c.body, c.bottom), startY = 0f, endY = cabH),
            size = Size(s, cabH),
            cornerRadius = cabCorner,
        )
        drawPath(grain, Color.Black.copy(alpha = 0.05f), style = Stroke(width = s * 0.006f))
        drawPath(grain, Color.White.copy(alpha = 0.03f), style = Stroke(width = s * 0.002f))
        // A warm sheen from the upper left, as from a room lamp.
        drawRoundRect(
            brush = Brush.radialGradient(
                listOf(Color(0x33FFE0B0), Color.Transparent),
                center = Offset(s * 0.22f, cabH * 0.1f),
                radius = s * 0.8f,
            ),
            size = Size(s, cabH),
            cornerRadius = cabCorner,
        )
        // Soft inner edge: light along the top, shade along the bottom, so it reads as solid.
        val inset = s * 0.009f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.32f)),
                startY = 0f,
                endY = cabH,
            ),
            topLeft = Offset(inset, inset),
            size = Size(s - inset * 2f, cabH - inset * 2f),
            cornerRadius = CornerRadius(s * (CAB_CORNER - 0.008f)),
            style = Stroke(width = s * 0.012f),
        )
        // Hairline rim, which keeps the edge visible on a pure black screen.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            size = Size(s, cabH),
            cornerRadius = cabCorner,
            style = Stroke(width = s * 0.003f),
        )

        /* window recess with a brass bezel; the cover goes on top */
        val winTL = Offset(s * WIN_L, s * WIN_T)
        val win = Size(s * WIN_S, s * WIN_S)
        val bezel = s * 0.014f
        drawRoundRect(
            brush = Brush.linearGradient(listOf(BrassLight, BrassDark, BrassLight.copy(alpha = 0.9f)), winTL, winTL + Offset(win.width, win.height)),
            topLeft = winTL - Offset(bezel, bezel),
            size = Size(win.width + bezel * 2f, win.height + bezel * 2f),
            cornerRadius = CornerRadius(s * WIN_CORNER + bezel),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = winTL - Offset(bezel, bezel),
            size = Size(win.width + bezel * 2f, win.height + bezel * 2f),
            cornerRadius = CornerRadius(s * WIN_CORNER + bezel),
            style = Stroke(width = s * 0.003f),
        )
        drawRoundRect(
            brush = Brush.linearGradient(listOf(c.placeholder, Recess), winTL, winTL + Offset(win.width, win.height)),
            topLeft = winTL,
            size = win,
            cornerRadius = CornerRadius(s * WIN_CORNER),
        )

        /* speaker grille */
        val grCorner = CornerRadius(s * 0.03f)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(grL - s * 0.006f, grT - s * 0.006f),
            size = Size(grR - grL + s * 0.012f, grB - grT + s * 0.012f),
            cornerRadius = grCorner,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(c.grille, c.bottom), startY = grT, endY = grB),
            topLeft = Offset(grL, grT),
            size = Size(grR - grL, grB - grT),
            cornerRadius = grCorner,
        )
        // Plain circles rather than round-capped points: point sprites are where GPU drivers
        // differ most, and this layer is recorded once anyway.
        val rimColor = Color.White.copy(alpha = 0.14f)
        val holeColor = Recess.copy(alpha = 0.92f)
        for (p in rims) drawCircle(rimColor, radius = hole / 2f, center = p)
        for (p in holes) drawCircle(holeColor, radius = hole / 2f, center = p)
        drawRoundRect(
            color = BrassLight.copy(alpha = 0.45f),
            topLeft = Offset(grL, grT),
            size = Size(grR - grL, grB - grT),
            cornerRadius = grCorner,
            style = Stroke(width = s * 0.005f),
        )

        /* dial recess: the glass sits in it */
        val dialPad = s * 0.012f
        drawRoundRect(
            color = Recess,
            topLeft = Offset(s * DIAL_L - dialPad, s * DIAL_T - dialPad),
            size = Size(s * (DIAL_R - DIAL_L) + dialPad * 2f, s * (DIAL_B - DIAL_T) + dialPad * 2f),
            cornerRadius = CornerRadius(s * 0.03f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = Offset(s * DIAL_L - dialPad, s * DIAL_T - dialPad),
            size = Size(s * (DIAL_R - DIAL_L) + dialPad * 2f, s * (DIAL_B - DIAL_T) + dialPad * 2f),
            cornerRadius = CornerRadius(s * 0.03f),
            style = Stroke(width = s * 0.003f),
        )

        /* knobs: shadow, ivory body, knurled rim, domed cap */
        for (kx in listOf(KNOB_LX, KNOB_RX)) {
            val center = Offset(s * kx, s * KNOB_Y)
            val r = s * KNOB_R
            drawCircle(Color.Black.copy(alpha = 0.35f), radius = r * 1.06f, center = center + Offset(0f, r * 0.14f))
            drawCircle(
                brush = Brush.radialGradient(listOf(Ivory, IvoryShade), center = center - Offset(r * 0.35f, r * 0.4f), radius = r * 1.5f),
                radius = r,
                center = center,
            )
            drawCircle(Ink.copy(alpha = 0.25f), radius = r, center = center, style = Stroke(width = s * 0.0025f))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), Ivory.copy(alpha = 0f)), center = center - Offset(r * 0.2f, r * 0.25f), radius = r * 0.7f),
                radius = r * 0.7f,
                center = center,
            )
        }
        drawPath(knurl, Ink.copy(alpha = 0.22f), style = Stroke(width = s * 0.004f, cap = StrokeCap.Round))

        /* brass name plate, with room on the left for the lamp */
        val plateTL = Offset(s * PLATE_L, s * PLATE_T)
        val plate = Size(s * (PLATE_R - PLATE_L), s * (PLATE_B - PLATE_T))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(BrassLight, BrassDark), startY = plateTL.y, endY = plateTL.y + plate.height),
            topLeft = plateTL,
            size = plate,
            cornerRadius = CornerRadius(plate.height / 2f),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = plateTL,
            size = plate,
            cornerRadius = CornerRadius(plate.height / 2f),
            style = Stroke(width = s * 0.003f),
        )
        drawCircle(Recess, radius = s * 0.016f, center = Offset(s * (PLATE_L + 0.045f), plateTL.y + plate.height / 2f))
        drawText(
            plateText,
            topLeft = Offset(
                s * (PLATE_L + 0.085f) + (plate.width - s * 0.085f - plateText.size.width) / 2f,
                plateTL.y + (plate.height - plateText.size.height) / 2f,
            ),
        )
    }
}

/* ---------------------------------------------------------------------------------------- */
/* Layer 3: the window glass                                                                 */
/* ---------------------------------------------------------------------------------------- */

private fun CacheDrawScope.buildWindowGlass(): DrawResult {
    val s = size.width
    val tl = Offset(s * WIN_L, s * WIN_T)
    val win = Size(s * WIN_S, s * WIN_S)
    val corner = CornerRadius(s * WIN_CORNER)
    val shade =
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.38f),
            0.14f to Color.Transparent,
            0.9f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.18f),
            startY = tl.y,
            endY = tl.y + win.height,
        )
    val gloss =
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.2f),
            0.4f to Color.White.copy(alpha = 0.04f),
            0.41f to Color.Transparent,
            start = tl,
            end = tl + Offset(win.width, win.height),
        )
    return onDrawBehind {
        drawRoundRect(brush = shade, topLeft = tl, size = win, cornerRadius = corner)
        drawRoundRect(brush = gloss, topLeft = tl, size = win, cornerRadius = corner)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = tl,
            size = win,
            cornerRadius = corner,
            style = Stroke(width = s * 0.004f),
        )
    }
}

/* ---------------------------------------------------------------------------------------- */
/* Layer 4: the dial                                                                         */
/* ---------------------------------------------------------------------------------------- */

/** Everything the dial needs, measured once per size; [draw] runs per frame that it changes. */
private class DialPieces(
    val s: Float,
    val rect: Rect,
    val scaleLeft: Float,
    val scaleRight: Float,
    val ticks: Path,
    val labels: List<Pair<TextLayoutResult, Offset>>,
    val bandLeft: Pair<TextLayoutResult, Offset>,
    val bandRight: Pair<TextLayoutResult, Offset>,
    val glassGloss: Brush,
) {
    fun draw(
        scope: DrawScope,
        f: Float,
        g: Float,
        volume: Float,
    ) = with(scope) {
        val corner = CornerRadius(s * 0.02f)

        // Backlight spilling round the glass while it is lit.
        if (g > 0.01f) {
            val spill = s * 0.016f
            drawRoundRect(
                color = Backlight.copy(alpha = 0.22f * g),
                topLeft = rect.topLeft - Offset(spill, spill),
                size = Size(rect.width + spill * 2f, rect.height + spill * 2f),
                cornerRadius = CornerRadius(s * 0.034f),
            )
        }

        // The glass: dim smoky cream when off, warm and bright when lit.
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(lerp(GlassOffTop, GlassOnTop, g), lerp(GlassOffBottom, GlassOnBottom, g)),
                startY = rect.top,
                endY = rect.bottom,
            ),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = corner,
        )
        if (g > 0.01f) {
            // The bulb behind the middle of the glass.
            drawRoundRect(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.45f * g), Color.Transparent),
                    center = rect.center,
                    radius = rect.width * 0.5f,
                ),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = corner,
            )
        }

        // Scale.
        drawPath(ticks, Ink.copy(alpha = 0.8f), style = Stroke(width = s * 0.0032f, cap = StrokeCap.Round))
        for ((text, at) in labels) drawText(text, topLeft = at)
        drawText(bandLeft.first, topLeft = bandLeft.second)
        drawText(bandRight.first, topLeft = bandRight.second)

        // Needle: a thin red line with its shadow on the glass, glowing a little when lit.
        val x = scaleLeft + (scaleRight - scaleLeft) * f
        val top = rect.top + rect.height * 0.08f
        val bottom = rect.bottom - rect.height * 0.08f
        drawLine(Color.Black.copy(alpha = 0.22f), Offset(x + s * 0.004f, top), Offset(x + s * 0.004f, bottom), strokeWidth = s * 0.006f)
        if (g > 0.01f) {
            drawLine(NeedleRed.copy(alpha = 0.25f * g), Offset(x, top), Offset(x, bottom), strokeWidth = s * 0.018f, cap = StrokeCap.Round)
        }
        drawLine(NeedleRed, Offset(x, top), Offset(x, bottom), strokeWidth = s * 0.0065f, cap = StrokeCap.Round)

        // Glass gloss and edge.
        drawRoundRect(brush = glassGloss, topLeft = rect.topLeft, size = rect.size, cornerRadius = corner)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = corner,
            style = Stroke(width = s * 0.004f),
        )

        // Left knob, the power switch: turned to the left when off, to the right when on, moving
        // with the backlight as the music stops and starts. Right knob, the volume: from far left
        // at silence round to far right at full.
        knobPointer(Offset(s * KNOB_LX, s * KNOB_Y), s * KNOB_R, -60f + 120f * g, s)
        knobPointer(Offset(s * KNOB_RX, s * KNOB_Y), s * KNOB_R, -135f + 270f * volume, s)

        // Pilot lamp in the name plate.
        val lamp = Offset(s * (PLATE_L + 0.045f), s * (PLATE_T + PLATE_B) / 2f)
        if (g > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(listOf(LampOn.copy(alpha = 0.55f * g), Color.Transparent), center = lamp, radius = s * 0.045f),
                radius = s * 0.045f,
                center = lamp,
            )
        }
        drawCircle(lerp(LampOff, LampOn, g), radius = s * 0.011f, center = lamp)
        drawCircle(Color.White.copy(alpha = 0.25f + 0.4f * g), radius = s * 0.004f, center = lamp - Offset(s * 0.003f, s * 0.003f))
    }
}

private fun DrawScope.knobPointer(
    center: Offset,
    r: Float,
    degrees: Float,
    s: Float,
) {
    val a = Math.toRadians(degrees.toDouble() - 90.0)
    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
    drawLine(
        Ink.copy(alpha = 0.75f),
        center + dir * (r * 0.3f),
        center + dir * (r * 0.78f),
        strokeWidth = s * 0.008f,
        cap = StrokeCap.Round,
    )
}

private fun CacheDrawScope.buildDial(measurer: TextMeasurer): DialPieces {
    val s = size.width
    val rect = Rect(s * DIAL_L, s * DIAL_T, s * DIAL_R, s * DIAL_B)
    val scaleLeft = rect.left + rect.width * DIAL_PAD
    val scaleRight = rect.right - rect.width * DIAL_PAD

    // 88 to 108 in half steps: long marks at the labels, medium on whole even numbers.
    val ticks = Path()
    val base = rect.bottom - rect.height * 0.16f
    for (i in 0..40) {
        val x = scaleLeft + (scaleRight - scaleLeft) * (i / 40f)
        val len =
            when {
                i % 8 == 0 -> rect.height * 0.34f
                i % 2 == 0 -> rect.height * 0.22f
                else -> rect.height * 0.12f
            }
        ticks.moveTo(x, base)
        ticks.lineTo(x, base - len)
    }

    val style =
        TextStyle(
            color = Ink,
            fontSize = (s * 0.034f).toSp(),
            fontWeight = FontWeight.Bold,
        )
    val small =
        TextStyle(
            color = Ink.copy(alpha = 0.7f),
            fontSize = (s * 0.024f).toSp(),
            fontWeight = FontWeight.Bold,
        )
    val labelBaseTop = rect.top + rect.height * 0.1f
    val labels =
        DialLabels.mapIndexed { i, mhz ->
            val text = measurer.measure(mhz.toString(), style)
            val x = scaleLeft + (scaleRight - scaleLeft) * (i / (DialLabels.size - 1f))
            text to Offset(x - text.size.width / 2f, labelBaseTop)
        }
    val fm = measurer.measure("FM", small)
    val mhz = measurer.measure("MHz", small)
    val bandY = rect.top + rect.height * 0.64f
    val bandLeft = fm to Offset(rect.left + (rect.width * DIAL_PAD * 0.55f) - fm.size.width / 2f, bandY - fm.size.height / 2f)
    val bandRight = mhz to Offset(rect.right - (rect.width * DIAL_PAD * 0.55f) - mhz.size.width / 2f, bandY - mhz.size.height / 2f)

    val gloss =
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.3f),
            0.45f to Color.White.copy(alpha = 0.06f),
            0.46f to Color.Transparent,
            startY = rect.top,
            endY = rect.bottom,
        )
    return DialPieces(s, rect, scaleLeft, scaleRight, ticks, labels, bandLeft, bandRight, gloss)
}

/* ---------------------------------------------------------------------------------------- */
/* Layer 0: the antennas                                                                     */
/* ---------------------------------------------------------------------------------------- */

private val Chrome = Color(0xFFD9DDE2)
private val ChromeShade = Color(0xFF8C939B)

/**
 * One telescopic antenna standing at the top right of the cabinet, leaning a little outwards
 * from a brass mount. The band is [ANT_H] of the width tall and ends where the cabinet starts.
 */
private fun CacheDrawScope.buildAntennas(): DrawResult {
    val s = size.width
    val baseY = size.height
    val mount = Offset(s * 0.8f, baseY)
    val length = size.height * 0.94f
    val a = Math.toRadians(18.0 - 90.0)
    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
    val joint = mount + dir * (length * 0.5f)
    val tip = mount + dir * length
    return onDrawBehind {
        // Shadow, then the thick lower section and the thin upper one.
        drawLine(Color.Black.copy(alpha = 0.18f), mount + Offset(s * 0.006f, 0f), tip + Offset(s * 0.006f, 0f), strokeWidth = s * 0.012f, cap = StrokeCap.Round)
        drawLine(ChromeShade, mount, joint, strokeWidth = s * 0.014f, cap = StrokeCap.Round)
        drawLine(Chrome, mount, joint, strokeWidth = s * 0.007f, cap = StrokeCap.Round)
        drawLine(ChromeShade, joint, tip, strokeWidth = s * 0.009f, cap = StrokeCap.Round)
        drawLine(Chrome, joint, tip, strokeWidth = s * 0.0045f, cap = StrokeCap.Round)
        // A small ball at the tip.
        drawCircle(ChromeShade, radius = s * 0.013f, center = tip)
        drawCircle(Chrome, radius = s * 0.008f, center = tip - Offset(s * 0.003f, s * 0.003f))
        // The brass mount it turns in, half sunk into the cabinet's top.
        drawArc(
            brush = Brush.verticalGradient(listOf(BrassLight, BrassDark), startY = baseY - s * 0.032f, endY = baseY),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(mount.x - s * 0.034f, baseY - s * 0.032f),
            size = Size(s * 0.068f, s * 0.064f),
        )
    }
}

/* ---------------------------------------------------------------------------------------- */
/* The phone's media volume, for the volume knob                                              */
/* ---------------------------------------------------------------------------------------- */

private class MediaVolume(
    private val audio: AudioManager?,
) {
    val steps: Int = (audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)

    var level by mutableFloatStateOf(read())
        private set

    private fun read(): Float = (audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / steps

    fun refresh() {
        level = read()
    }

    /** Sets the volume to the step nearest [fraction]; true when that changed the step. */
    fun set(fraction: Float): Boolean {
        val audio = audio ?: return false
        val index = (fraction.coerceIn(0f, 1f) * steps).roundToInt()
        val changed = index != audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        // A fixed-volume phone, or Do Not Disturb, can refuse.
        if (changed) runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0) }
        refresh()
        return changed
    }
}

/** The media volume, kept up to date while on screen, including changes from the side buttons. */
@Composable
private fun rememberMediaVolume(): MediaVolume {
    val context = LocalContext.current
    val volume = remember(context) { MediaVolume(context.getSystemService(AudioManager::class.java)) }
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    c: Context?,
                    intent: Intent?,
                ) = volume.refresh()
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        volume.refresh()
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return volume
}
