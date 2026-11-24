/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/*
 * Shared pieces for the What's new demos. Each demo is drawn on a 280 × 132 design box,
 * scaled to fit its card, from one clock that runs 0 to 1 over the loop. The clock is read
 * only while drawing, so a looping demo redraws without recomposing anything.
 */

internal const val DESIGN_W = 280f
internal const val DESIGN_H = 132f

/** How far [t] is from [from] to [to], held between 0 and 1. */
internal fun seg(
    t: Float,
    from: Float,
    to: Float,
): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

internal fun ease(x: Float): Float = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f

internal fun easeOut(x: Float): Float = 1f - (1f - x).pow(3)

/** Goes a little past 1 and settles back, for things that pop into place. */
internal fun overshoot(x: Float): Float {
    val c = 1.70158f
    val y = x - 1f
    return 1f + (c + 1f) * y * y * y + c * y * y
}

internal fun mix(
    from: Float,
    to: Float,
    x: Float,
): Float = from + (to - from) * x

/** 0 at both ends and 1 in the middle: a press that goes down and comes back up. */
internal fun press(x: Float): Float = sin(PI.toFloat() * x)

internal fun Color.fade(amount: Float): Color = copy(alpha = alpha * amount.coerceIn(0f, 1f))

/** The demo's colours, all from the theme, so light, dark, pure black and album colours work. */
@Immutable
internal class DemoColors(
    val accent: Color,
    val onAccent: Color,
    val panel: Color,
    val control: Color,
    val onControl: Color,
    val ink: Color,
    val inkSoft: Color,
    /** Raised surfaces: search bars, the mini player, sheets. */
    val raised: Color,
    val line: Color,
    val artStart: Color,
    val artEnd: Color,
    /** Cover shades for the sample songs, so each song looks like its own album. */
    val covers: List<Pair<Color, Color>>,
)

@Composable
internal fun rememberDemoColors(): DemoColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        DemoColors(
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            panel = scheme.surface,
            control = scheme.secondaryContainer,
            onControl = scheme.onSecondaryContainer,
            ink = scheme.onSurface,
            inkSoft = scheme.onSurfaceVariant,
            raised = scheme.surfaceContainerHigh,
            line = scheme.onSurfaceVariant.copy(alpha = 0.35f),
            artStart = scheme.primary,
            artEnd = scheme.tertiary,
            covers =
                listOf(
                    scheme.primary to scheme.tertiary,
                    scheme.tertiary to scheme.secondary,
                    scheme.secondary to scheme.primary,
                    scheme.primaryContainer to scheme.tertiary,
                    scheme.tertiaryContainer to scheme.primary,
                    scheme.secondaryContainer to scheme.secondary,
                ),
        )
    }
}

/** How many times a demo plays its whole story before it settles on its key moment. */
private const val DEMO_PLAYS = 2

/** True when animations are off (Remove animations, or an animator scale of 0). */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * A demo that loops every [loopMs] while [playing], and otherwise holds its still frame at
 * [stillAt]: the moment that explains the feature, with the finger on the control.
 */
@Composable
internal fun DemoCanvas(
    loopMs: Int,
    stillAt: Float,
    playing: Boolean,
    modifier: Modifier = Modifier,
    draw: DrawScope.(t: Float, colors: DemoColors) -> Unit,
) {
    val still = rememberReduceMotion()
    val colors = rememberDemoColors()
    val clock = remember { Animatable(stillAt) }
    LaunchedEffect(playing, still) {
        if (!playing || still) {
            clock.snapTo(stillAt)
            return@LaunchedEffect
        }
        // Each time a demo comes into view it starts from the beginning of its story. It tells it
        // DEMO_PLAYS times, then once more up to its key moment and holds there, so a card left on
        // screen stops drawing frames (idle means zero frames). Swiping or scrolling back restarts it.
        repeat(DEMO_PLAYS) {
            clock.snapTo(0f)
            clock.animateTo(1f, tween(durationMillis = loopMs, easing = LinearEasing))
        }
        clock.snapTo(0f)
        clock.animateTo(stillAt, tween(durationMillis = (loopMs * stillAt).toInt(), easing = LinearEasing))
    }
    Spacer(modifier.drawBehind { draw(clock.value, colors) })
}

/** Draws [block] in the 280 × 132 design box, centred and scaled; `u` is pixels per unit. */
internal inline fun DrawScope.inDesignBox(block: DrawScope.(u: Float) -> Unit) {
    val u = min(size.width / DESIGN_W, size.height / DESIGN_H)
    translate(left = (size.width - DESIGN_W * u) / 2f, top = (size.height - DESIGN_H * u) / 2f) {
        block(u)
    }
}

/** A rounded bar or box in design units; round-ended unless [radius] says otherwise. */
internal fun DrawScope.pill(
    color: Color,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    u: Float,
    radius: Float = h / 2f,
) {
    if (color.alpha <= 0f || w <= 0f || h <= 0f) return
    drawRoundRect(color, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(radius * u))
}

/** A stand-in album cover, shaded from the theme's colours. */
internal fun DrawScope.art(
    colors: DemoColors,
    x: Float,
    y: Float,
    side: Float,
    radius: Float,
    u: Float,
    alpha: Float = 1f,
) {
    if (alpha <= 0f) return
    drawRoundRect(
        brush =
            Brush.linearGradient(
                listOf(colors.artStart, colors.artEnd),
                start = Offset(x * u, y * u),
                end = Offset((x + side) * u, (y + side) * u),
            ),
        topLeft = Offset(x * u, y * u),
        size = Size(side * u, side * u),
        cornerRadius = CornerRadius(radius * u),
        alpha = alpha.coerceIn(0f, 1f),
    )
}

/** One of the app's own icons, centred on ([cx], [cy]). */
internal fun DrawScope.icon(
    painter: Painter,
    cx: Float,
    cy: Float,
    side: Float,
    tint: Color,
    u: Float,
) {
    if (tint.alpha <= 0f || side <= 0f) return
    val px = side * u
    translate(cx * u - px / 2f, cy * u - px / 2f) {
        with(painter) { draw(Size(px, px), colorFilter = ColorFilter.tint(tint)) }
    }
}

/** Short text centred on ([cx], [cy]), [sizeUnits] tall in design units. */
internal fun DrawScope.centredText(
    measurer: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    sizeUnits: Float,
    color: Color,
    u: Float,
    bold: Boolean = true,
) {
    if (color.alpha <= 0f) return
    val layout =
        measurer.measure(
            text,
            TextStyle(fontSize = (sizeUnits * u).toSp(), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
        )
    drawText(
        layout,
        color = color,
        topLeft = Offset(cx * u - layout.size.width / 2f, cy * u - layout.size.height / 2f),
    )
}

/**
 * The finger: a soft dot that shrinks a little when it presses, and a ring that spreads from
 * it on a tap. [tap] runs 0 to 1 over one press.
 */
internal fun DrawScope.finger(
    colors: DemoColors,
    cx: Float,
    cy: Float,
    visible: Float,
    tap: Float,
    u: Float,
    size: Float = 13f,
) {
    if (visible <= 0f) return
    val center = Offset(cx * u, cy * u)
    if (tap > 0f && tap < 1f) {
        drawCircle(
            colors.accent.fade(0.8f * (1f - tap) * visible),
            radius = size * (1f + 1.7f * tap) * u,
            center = center,
            style = Stroke(2f * u),
        )
    }
    val radius = size * u * (1f - 0.18f * press(tap))
    drawCircle(colors.ink.fade(0.2f * visible), radius, center)
    drawCircle(colors.ink.fade(0.45f * visible), radius, center, style = Stroke(1.5f * u))
}
