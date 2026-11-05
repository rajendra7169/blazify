/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * The Blazify logo dressed for each festival, drawn live. Everything is laid out on the logo's
 * own 256-unit square, the same one its image uses, so a decoration never makes the logo
 * bigger: it only fills the empty space the logo already has around it. The festive colours
 * are the same in light and dark mode; only the logo itself follows the theme.
 */

package com.blazify.music.festival

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.blazify.music.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One loop of every festival animation, in seconds; each movement's period divides it. */
private const val LOOP = 12f

/** The logo for [festival], moving; [size] is the logo's usual size and is never exceeded by layout. */
@Composable
fun FestivalLogoArt(
    festival: Festival,
    isDark: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // With animations turned off on the phone, the logo holds still.
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val clock = rememberInfiniteTransition(label = "festivalLogo")
    val running by clock.animateFloat(
        initialValue = 0f,
        targetValue = LOOP,
        animationSpec = infiniteRepeatable(tween((LOOP * 1000).toInt(), easing = LinearEasing)),
        label = "festivalLogoTime",
    )
    val t = if (still) 1.3f else running

    val logo = ImageBitmap.imageResource(if (isDark) R.drawable.blaze_logo_white else R.drawable.blaze_logo)
    val headphones =
        ImageBitmap.imageResource(if (isDark) R.drawable.blaze_logo_headphones_white else R.drawable.blaze_logo_headphones)
    val flame = ImageBitmap.imageResource(R.drawable.blaze_logo_flame_play)

    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 256f
        scale(k, k, pivot = Offset.Zero) {
            when (festival) {
                Festival.TIHAR -> {
                    image(headphones)
                    garland(t)
                    diya(t, flame, rangoli = false)
                }
                Festival.DIWALI -> {
                    image(headphones)
                    diya(t, flame, rangoli = true)
                    sparkle(44f, 48f, 9f, Gold, t, 0f)
                    sparkle(212f, 44f, 8f, PaleGold, t, 0.9f)
                    sparkle(30f, 92f, 5f, PaleGold, t, 1.7f)
                    sparkle(228f, 96f, 5f, Gold, t, 0.4f)
                }
                Festival.CHRISTMAS -> {
                    image(logo)
                    santaHat(t)
                }
                Festival.NEW_YEAR -> {
                    image(logo)
                    fireworks(t)
                }
                Festival.MUSIC_DAY -> {
                    image(logo)
                    musicNotes(t)
                }
                Festival.HOLI -> {
                    image(logo)
                    holi(t, logo)
                }
                Festival.DASHAIN -> {
                    image(logo)
                    kite(t)
                }
                Festival.LUNAR_NEW_YEAR -> {
                    image(logo)
                    lantern(t)
                }
                Festival.EID_AL_FITR -> {
                    image(logo)
                    eid(t, green = false)
                }
                Festival.EID_AL_ADHA -> {
                    image(logo)
                    eid(t, green = true)
                }
            }
        }
    }
}

// ── Colours ──────────────────────────────────────────────────────────────────

private val Gold = Color(0xFFFFC107)
private val PaleGold = Color(0xFFFFE082)
private val Marigold = Color(0xFFFB8C00)
private val Saffron = Color(0xFFFFA000)
private val Makhamali = Color(0xFFC2185B)

private fun wave(t: Float, period: Float, phase: Float = 0f) = sin(2f * PI.toFloat() * (t + phase) / period)

/** 0..1 over [period], starting [delay] seconds in. */
private fun cycle(t: Float, period: Float, delay: Float = 0f): Float {
    val x = ((t - delay) / period) % 1f
    return if (x < 0f) x + 1f else x
}

private fun DrawScope.image(bitmap: ImageBitmap) =
    drawImage(bitmap, dstSize = IntSize(256, 256), filterQuality = FilterQuality.High)

// ── Tihar and Diwali ─────────────────────────────────────────────────────────

/** Marigold (sayapatri) and makhamali garland over the headband, swaying a little. */
private fun DrawScope.garland(t: Float) {
    rotate(1.2f * wave(t, 6f), pivot = Offset(128f, 50f)) {
        for (i in 0 until 11) {
            val a = (18f + i * 14.4f) * PI.toFloat() / 180f
            val x = 128f - 71f * cos(a)
            val y = 121f - 71f * sin(a)
            if (i % 3 == 1) makhamali(x, y, 6.2f) else marigold(x, y, 9f)
        }
        marigold(52f, 132f, 7f)
        makhamali(50f, 146f, 5f)
        marigold(204f, 132f, 7f)
        makhamali(206f, 146f, 5f)
    }
}

private fun DrawScope.marigold(x: Float, y: Float, r: Float) {
    for (i in 0 until 12) {
        val a = i * PI.toFloat() / 6f
        drawCircle(Marigold, r * 0.38f, Offset(x + cos(a) * r * 0.72f, y + sin(a) * r * 0.72f))
    }
    drawCircle(Saffron, r * 0.78f, Offset(x, y))
    drawCircle(Gold, r * 0.5f, Offset(x, y))
    drawCircle(PaleGold, r * 0.22f, Offset(x - r * 0.15f, y - r * 0.15f))
}

private fun DrawScope.makhamali(x: Float, y: Float, r: Float) {
    drawCircle(Makhamali, r, Offset(x, y))
    drawCircle(Color(0xFFF48FB1).copy(alpha = 0.8f), r * 0.35f, Offset(x - r * 0.3f, y - r * 0.3f))
}

/** A clay diyo holding the Blazify flame, play button and all, flickering. */
private fun DrawScope.diya(t: Float, flame: ImageBitmap, rangoli: Boolean) {
    val glow = 0.55f + 0.45f * (0.5f + 0.5f * wave(t, 3f))
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFFFFB74D).copy(alpha = 0.55f * glow), Color.Transparent),
            center = Offset(128f, 120f),
            radius = 62f,
        ),
        topLeft = Offset(70f, 58f),
        size = Size(116f, 124f),
    )
    if (rangoli) rangoliDots()

    val stretch = 1f + 0.045f * wave(t, 1.2f) + 0.02f * wave(t, 0.4f)
    val narrow = 1f - 0.03f * wave(t, 1.2f, 0.35f)
    withTransform({
        rotate(1.6f * wave(t, 1.5f), pivot = Offset(128f, 175f))
        scale(narrow, stretch, pivot = Offset(128f, 175f))
        translate(38.4f, 39.9f)
        scale(0.7f, 0.7f, pivot = Offset.Zero)
    }) {
        image(flame)
    }

    val bowl =
        Path().apply {
            moveTo(87f, 164f)
            cubicTo(90f, 195f, 166f, 195f, 169f, 164f)
            cubicTo(157f, 159.5f, 99f, 159.5f, 87f, 164f)
            close()
        }
    drawPath(
        bowl,
        Brush.verticalGradient(
            0f to Color(0xFFE07B45),
            0.45f to Color(0xFFB9532A),
            1f to Color(0xFF6E2A12),
            startY = 159f,
            endY = 190f,
        ),
    )
    val handle =
        Path().apply {
            moveTo(169f, 164f)
            quadraticTo(178f, 158f, 175f, 151f)
            quadraticTo(171f, 160f, 160f, 162f)
            close()
        }
    drawPath(handle, Color(0xFFB9532A))
    drawOval(Color(0xFF5A2310), Offset(87f, 158f), Size(82f, 12f))
    drawOval(Color(0xFF8A3A18), Offset(90.5f, 159.5f), Size(75f, 7f))
    listOf(100f, 110f, 119f, 128f, 137f, 146f, 156f).forEachIndexed { i, x ->
        drawCircle(Color(0xFFFFD54F), 2.2f, Offset(x, if (i == 0 || i == 6) 176f else 179f))
    }
    val band = Path().apply {
        moveTo(101f, 172f)
        quadraticTo(128f, 181f, 155f, 172f)
    }
    drawPath(band, Color(0xFFE8894A).copy(alpha = 0.7f), style = Stroke(1.6f))
}

private fun DrawScope.rangoliDots() {
    val colors = listOf(Color(0xFFE91E63), Color(0xFF26A69A), Gold, Color(0xFF7E57C2))
    for (k in 0 until 3) {
        val r = 22f + k * 9f
        for (i in 0..8) {
            val a = PI.toFloat() + i * PI.toFloat() / 8f
            drawCircle(colors[(i + k) % 4], 3.4f - k * 0.6f, Offset(128f + cos(a) * r * 1.7f, 206f + sin(a) * r * 0.5f))
        }
    }
}

/** A four-pointed sparkle that grows and fades in turn. */
private fun DrawScope.sparkle(x: Float, y: Float, r: Float, color: Color, t: Float, phase: Float) {
    val on = 0.5f + 0.5f * wave(t, 3f, phase)
    val s = r * (0.6f + 0.5f * on)
    val star =
        Path().apply {
            moveTo(x, y - s)
            quadraticTo(x + s * 0.18f, y - s * 0.18f, x + s, y)
            quadraticTo(x + s * 0.18f, y + s * 0.18f, x, y + s)
            quadraticTo(x - s * 0.18f, y + s * 0.18f, x - s, y)
            quadraticTo(x - s * 0.18f, y - s * 0.18f, x, y - s)
            close()
        }
    drawPath(star, color.copy(alpha = 0.25f + 0.75f * on))
}

// ── Christmas ────────────────────────────────────────────────────────────────

/** A Santa hat worn on the headband: the fur brim follows the band's curve. */
private fun DrawScope.santaHat(t: Float) {
    val hat =
        Path().apply {
            moveTo(94f, 52f)
            cubicTo(96f, 20f, 128f, 2f, 160f, 6f)
            cubicTo(184f, 9f, 202f, 24f, 210f, 48f)
            cubicTo(214f, 58f, 214f, 66f, 212f, 72f)
            cubicTo(204f, 60f, 194f, 50f, 182f, 44f)
            cubicTo(176f, 46f, 170f, 48f, 162f, 50f)
            close()
        }
    drawPath(hat, Brush.linearGradient(listOf(Color(0xFFEF5350), Color(0xFFB71C1C)), Offset(94f, 2f), Offset(214f, 72f)))
    drawPath(
        Path().apply {
            moveTo(112f, 38f)
            cubicTo(124f, 18f, 150f, 12f, 170f, 16f)
        },
        Color(0xFFFF8A80).copy(alpha = 0.45f),
        style = Stroke(4f, cap = StrokeCap.Round),
    )
    drawPath(
        Path().apply {
            moveTo(182f, 44f)
            cubicTo(194f, 50f, 204f, 60f, 212f, 72f)
        },
        Color(0xFF8E1414).copy(alpha = 0.5f),
        style = Stroke(2.5f),
    )
    // The brim is an arc of the headband's own circle (centre 128,121), so it sits on it.
    val band = Rect(Offset(50f, 43f), Size(156f, 156f))
    drawArc(Color(0xFFF5F5F5), -120f, 60f, false, band.topLeft, band.size, style = Stroke(17f, cap = StrokeCap.Round))
    drawArc(Color.White, -116f, 52f, false, band.topLeft, band.size, style = Stroke(9f, cap = StrokeCap.Round))

    rotate(0.5f + 4.5f * wave(t, 3f), pivot = Offset(201.5f, 66.5f)) {
        drawCircle(Color(0xFFFAFAFA), 11.5f, Offset(213f, 78f))
        drawCircle(Color.White, 4.5f, Offset(209f, 74f))
        drawCircle(Color(0xFFE0E0E0), 3f, Offset(216f, 82f))
    }
    falling(t, 0f) { snowflake(36f, 40f, 9f) }
    falling(t, 1f) { snowflake(232f, 150f, 7f) }
    falling(t, 2f) { snowflake(24f, 160f, 6f) }
}

private fun DrawScope.snowflake(x: Float, y: Float, r: Float) {
    for (i in 0 until 3) {
        val a = i * PI.toFloat() / 3f
        drawLine(
            Color(0xFF90CAF9),
            Offset(x - cos(a) * r, y - sin(a) * r),
            Offset(x + cos(a) * r, y + sin(a) * r),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round,
        )
    }
}

/** Something drifting down and fading at both ends, every three seconds. */
private fun DrawScope.falling(t: Float, delay: Float, content: DrawScope.() -> Unit) {
    val p = cycle(t, 3f, delay)
    val alpha = when {
        p < 0.15f -> p / 0.15f
        p > 0.85f -> (1f - p) / 0.15f
        else -> 1f
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(-40f, -60f, 300f, 320f), Paint().apply { this.alpha = alpha })
        translate(0f, -30f + 70f * p) { content() }
        canvas.restore()
    }
}

// ── New Year ─────────────────────────────────────────────────────────────────

private fun DrawScope.fireworks(t: Float) {
    burst(t, 70f, 34f, 24f, Gold, Color(0xFFFF7043), 0f)
    burst(t, 140f, 18f, 22f, Color(0xFFEC407A), Color(0xFFFFD54F), 0.8f)
    burst(t, 204f, 40f, 22f, Color(0xFF26C6DA), Color(0xFFB388FF), 1.6f)
    val confetti =
        listOf(
            Triple(Offset(30f, 92f), Color(0xFFEC407A), 20f),
            Triple(Offset(226f, 96f), Gold, -30f),
            Triple(Offset(22f, 140f), Color(0xFF26C6DA), 45f),
            Triple(Offset(232f, 150f), Color(0xFF7E57C2), -15f),
            Triple(Offset(40f, 190f), Color(0xFFFF7043), 60f),
            Triple(Offset(218f, 196f), Color(0xFF66BB6A), -40f),
        )
    confetti.forEachIndexed { i, (at, color, angle) ->
        falling(t, if (i % 2 == 0) 0f else 1.5f) {
            rotate(angle, pivot = at) {
                drawRoundRect(color, at, Size(9f, 4.5f), androidx.compose.ui.geometry.CornerRadius(1.5f))
            }
        }
    }
}

/** A firework that opens from its centre, holds, and fades, every 2.4 s. */
private fun DrawScope.burst(t: Float, x: Float, y: Float, r: Float, c1: Color, c2: Color, delay: Float) {
    val p = cycle(t, 2.4f, delay)
    val grow = if (p < 0.6f) 0.15f + 0.85f * (p / 0.6f) else 1f + 0.15f * ((p - 0.6f) / 0.4f)
    val alpha = when {
        p < 0.15f -> p / 0.15f
        p < 0.6f -> 1f
        else -> 1f - (p - 0.6f) / 0.4f
    }
    scale(grow, grow, pivot = Offset(x, y)) {
        for (i in 0 until 12) {
            val a = i * PI.toFloat() / 6f
            val color = (if (i % 2 == 1) c2 else c1).copy(alpha = alpha)
            drawLine(
                color,
                Offset(x + cos(a) * r * 0.35f, y + sin(a) * r * 0.35f),
                Offset(x + cos(a) * r, y + sin(a) * r),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
            drawCircle(color, 2.2f, Offset(x + cos(a) * (r + 4f), y + sin(a) * (r + 4f)))
        }
        drawCircle(Color(0xFFFFF8E1).copy(alpha = alpha), 3f, Offset(x, y))
    }
}

// ── World Music Day ──────────────────────────────────────────────────────────

private fun DrawScope.musicNotes(t: Float) {
    fun lift(phase: Float) = 3f - 8f * (0.5f + 0.5f * wave(t, 4f, phase))
    translate(0f, lift(0f)) { note(214f, 64f, 1f, Color(0xFFEC407A), double = false) }
    translate(0f, lift(1.2f)) { note(30f, 82f, 0.85f, Color(0xFF26C6DA), double = true) }
    translate(0f, lift(2.4f)) { note(222f, 118f, 0.7f, Color(0xFF7E57C2), double = false) }
    sparkle(196f, 30f, 7f, Gold, t, 0f)
    sparkle(48f, 40f, 5f, Gold, t, 1.4f)
}

private fun DrawScope.note(x: Float, y: Float, s: Float, color: Color, double: Boolean) {
    fun head(cx: Float, cy: Float) {
        rotate(-22f, pivot = Offset(cx, cy)) {
            drawOval(color, Offset(cx - 7f * s, cy - 5.2f * s), Size(14f * s, 10.4f * s))
        }
        drawRect(color, Offset(cx + 5.2f * s, cy - 30f * s), Size(2.6f * s, 30f * s))
    }
    head(x, y)
    if (!double) {
        val flag =
            Path().apply {
                moveTo(x + 7.8f * s, y - 30f * s)
                relativeQuadraticTo(10f * s, 4f * s, 8f * s, 16f * s)
                relativeQuadraticTo(-2f * s, -7f * s, -8f * s, -9f * s)
                close()
            }
        drawPath(flag, color)
    } else {
        val x2 = x + 22f * s
        val y2 = y - 5f * s
        head(x2, y2)
        val beam =
            Path().apply {
                moveTo(x + 5.2f * s, y - 30f * s)
                lineTo(x2 + 7.8f * s, y2 - 30f * s)
                lineTo(x2 + 7.8f * s, y2 - 23f * s)
                lineTo(x + 5.2f * s, y - 23f * s)
                close()
            }
        drawPath(beam, color)
    }
}

// ── Holi ─────────────────────────────────────────────────────────────────────

/** Colour powder splashed on the headphones (only where the logo is), and dust drifting round it. */
private fun DrawScope.holi(t: Float, logo: ImageBitmap) {
    val colors = listOf(Color(0xFFEC407A), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF8E24AA))
    val blobs =
        listOf(
            floatArrayOf(56f, 150f, 24f, 0f),
            floatArrayOf(128f, 52f, 26f, 1f),
            floatArrayOf(200f, 150f, 24f, 2f),
            floatArrayOf(176f, 70f, 18f, 3f),
            floatArrayOf(80f, 72f, 16f, 4f),
            floatArrayOf(128f, 170f, 22f, 3f),
        )
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(0f, 0f, 256f, 256f), Paint())
        blobs.forEach { (x, y, r, c) ->
            val blob =
                Path().apply {
                    moveTo(x - r, y)
                    cubicTo(x - r, y - r * 1.2f, x + r * 0.6f, y - r * 1.1f, x + r, y - r * 0.2f)
                    cubicTo(x + r * 1.3f, y + r * 0.6f, x + r * 0.2f, y + r * 1.1f, x - r * 0.4f, y + r * 0.9f)
                    cubicTo(x - r * 1.2f, y + r * 0.7f, x - r * 1.1f, y + r * 0.3f, x - r, y)
                    close()
                }
            drawPath(blob, colors[c.toInt()].copy(alpha = 0.88f))
        }
        drawImage(logo, dstSize = IntSize(256, 256), blendMode = BlendMode.DstIn, filterQuality = FilterQuality.High)
        canvas.restore()
    }
    val drift = 0.5f + 0.5f * wave(t, 6f)
    val dust =
        listOf(
            Triple(30f, 120f, 0), Triple(24f, 96f, 0), Triple(226f, 120f, 2), Triple(232f, 96f, 2), Triple(100f, 26f, 1),
            Triple(150f, 24f, 1), Triple(196f, 40f, 3), Triple(60f, 40f, 4), Triple(226f, 180f, 2), Triple(30f, 184f, 0),
        )
    translate(-2f + 5f * drift, 2f - 6f * drift) {
        dust.forEach { (x, y, c) ->
            for (j in 0 until 5) {
                drawCircle(
                    colors[c].copy(alpha = (0.35f + j * 0.1f) * (0.6f + 0.4f * drift)),
                    1.6f + j % 3,
                    Offset(x + ((j * 7) % 11 - 5) * 1.6f, y + ((j * 5) % 9 - 4) * 1.6f),
                )
            }
        }
    }
}

// ── Dashain ──────────────────────────────────────────────────────────────────

/** A kite (changa) flying from the right ear cup, and jamara grass behind the left. */
private fun DrawScope.kite(t: Float) {
    drawPath(
        Path().apply {
            moveTo(214f, 78f)
            quadraticTo(228f, 120f, 214f, 150f)
        },
        Color(0xFF8D6E63),
        style = Stroke(1.6f),
    )
    withTransform({
        translate(0f, -0.5f - 2.5f * wave(t, 4f))
        rotate(7f * wave(t, 4f), pivot = Offset(214f, 78f))
    }) {
        rotate(12f, pivot = Offset(214f, 46f)) {
            drawPath(diamond(214f, 16f, 240f, 46f, 214f, 76f, 188f, 46f), Color(0xFFFDD835))
            drawPath(triangle(214f, 16f, 240f, 46f, 214f, 46f), Color(0xFFE53935))
            drawPath(triangle(214f, 46f, 214f, 76f, 188f, 46f), Color(0xFFE53935))
            drawPath(triangle(214f, 16f, 214f, 46f, 188f, 46f), Color(0xFF1E88E5))
            drawLine(Color(0xFF5D4037), Offset(214f, 16f), Offset(214f, 76f), 1.4f)
            drawLine(Color(0xFF5D4037), Offset(188f, 46f), Offset(240f, 46f), 1.4f)
        }
        drawPath(
            Path().apply {
                moveTo(206f, 80f)
                relativeQuadraticTo(-6f, 10f, 2f, 18f)
                relativeQuadraticTo(8f, 8f, 0f, 18f)
            },
            Color(0xFFE53935),
            style = Stroke(1.6f),
        )
        drawPath(triangle(200f, 92f, 207f, 89f, 208f, 96f), Color(0xFFFDD835))
        drawPath(triangle(201f, 108f, 209f, 106f, 208f, 113f), Color(0xFFFDD835))
    }
    val sway = 1.5f * wave(t, 6f)
    listOf(-30f to Color(0xFFFBC02D), -15f to Color(0xFFFDD835), 0f to Color(0xFFF9A825), 15f to Color(0xFFFDD835), 28f to Color(0xFFFFEE58))
        .forEach { (a, color) ->
            drawPath(
                Path().apply {
                    moveTo(52f, 128f)
                    relativeQuadraticTo(-4f + a * 0.2f, -30f, a * 0.9f + sway, -52f)
                },
                color,
                style = Stroke(3.2f, cap = StrokeCap.Round),
            )
        }
}

private fun diamond(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float) =
    Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x3, y3)
        lineTo(x4, y4)
        close()
    }

private fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
    Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x3, y3)
        close()
    }

// ── Lunar New Year ───────────────────────────────────────────────────────────

/** A red lantern swinging from the headband, and a plum blossom sprig across the top. */
private fun DrawScope.lantern(t: Float) {
    rotate(5f * wave(t, 4f), pivot = Offset(68f, 74f)) {
        drawLine(Color(0xFFC62828), Offset(68f, 74f), Offset(44f, 96f), 1.8f)
        rotate(-8f, pivot = Offset(45f, 95f)) {
            drawRoundRect(goldBrush(34f, 92f, 56f, 98f), Offset(34f, 92f), Size(22f, 6f), androidx.compose.ui.geometry.CornerRadius(2f))
        }
        drawOval(redBrush(22f, 100f, 62f, 136f), Offset(22f, 100f), Size(40f, 36f))
        drawPath(
            Path().apply {
                moveTo(42f, 100f)
                quadraticTo(32f, 118f, 42f, 136f)
                moveTo(42f, 100f)
                quadraticTo(52f, 118f, 42f, 136f)
                moveTo(24f, 118f)
                lineTo(60f, 118f)
            },
            Gold.copy(alpha = 0.9f),
            style = Stroke(1.4f),
        )
        drawRoundRect(goldBrush(32f, 134f, 52f, 140f), Offset(32f, 134f), Size(20f, 6f), androidx.compose.ui.geometry.CornerRadius(2f))
        drawLine(Gold, Offset(38f, 140f), Offset(36f, 160f), 2f, cap = StrokeCap.Round)
        drawLine(Gold, Offset(42f, 140f), Offset(42f, 162f), 2f, cap = StrokeCap.Round)
        drawLine(Gold, Offset(46f, 140f), Offset(48f, 160f), 2f, cap = StrokeCap.Round)
    }
    drawPath(
        Path().apply {
            moveTo(150f, 34f)
            quadraticTo(178f, 18f, 214f, 24f)
        },
        Color(0xFF6D4C41),
        style = Stroke(3f, cap = StrokeCap.Round),
    )
    listOf(Offset(166f, 26f), Offset(192f, 18f), Offset(212f, 26f)).forEach { c ->
        for (i in 0 until 5) {
            val a = i * 2f * PI.toFloat() / 5f - PI.toFloat() / 2f
            drawCircle(Color(0xFFF48FB1), 4f, Offset(c.x + cos(a) * 5f, c.y + sin(a) * 5f))
        }
        drawCircle(Color(0xFFFFD54F), 2.6f, c)
    }
}

private fun goldBrush(x0: Float, y0: Float, x1: Float, y1: Float) =
    Brush.linearGradient(listOf(PaleGold, Gold, Color(0xFFF57F17)), Offset(x0, y0), Offset(x1, y1))

private fun redBrush(x0: Float, y0: Float, x1: Float, y1: Float) =
    Brush.linearGradient(listOf(Color(0xFFEF5350), Color(0xFFB71C1C)), Offset(x0, y0), Offset(x1, y1))

// ── Eid ──────────────────────────────────────────────────────────────────────

/** A crescent and stars, and a fanous lantern swinging from the headband: gold, or green and gold. */
private fun DrawScope.eid(t: Float, green: Boolean) {
    val moon =
        Path.combine(
            PathOperation.Difference,
            Path().apply { addOval(Rect(Offset(56f, 50f), 26f)) },
            Path().apply { addOval(Rect(Offset(68f, 44f), 21f)) },
        )
    drawPath(
        moon,
        if (green) {
            Brush.linearGradient(listOf(Color(0xFF81C784), Color(0xFF1B5E20)), Offset(30f, 24f), Offset(82f, 76f))
        } else {
            goldBrush(30f, 24f, 82f, 76f)
        },
    )
    sparkle(80f, 36f, 8f, Gold, t, 0f)
    sparkle(30f, 24f, 5f, PaleGold, t, 1.1f)
    sparkle(206f, 40f, 6f, PaleGold, t, 2f)

    rotate(5f * wave(t, 4f), pivot = Offset(190f, 66f)) {
        drawLine(Color(0xFFB8860B), Offset(190f, 66f), Offset(222f, 84f), 1.6f)
        drawPath(diamond(216f, 84f, 228f, 84f, 232f, 92f, 212f, 92f), goldBrush(212f, 84f, 232f, 92f))
        val glass =
            Path().apply {
                moveTo(212f, 92f)
                lineTo(232f, 92f)
                lineTo(236f, 118f)
                lineTo(222f, 132f)
                lineTo(208f, 118f)
                close()
            }
        val glow = 0.75f + 0.25f * wave(t, 3f)
        drawPath(
            glass,
            Brush.radialGradient(
                listOf(
                    (if (green) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)).copy(alpha = glow),
                    if (green) Color(0xFF43A047) else Color(0xFFFFB300),
                ),
                center = Offset(222f, 110f),
                radius = 20f,
            ),
        )
        drawPath(glass, Color(0xFFB8860B), style = Stroke(2f))
        drawLine(Color(0xFFB8860B), Offset(222f, 92f), Offset(222f, 132f), 1.4f)
        drawLine(Color(0xFFB8860B), Offset(210f, 106f), Offset(234f, 106f), 1.4f)
        drawPath(triangle(216f, 132f, 228f, 132f, 222f, 142f), goldBrush(216f, 132f, 228f, 142f))
    }
}
