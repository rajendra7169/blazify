/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * A few seconds of celebration over the whole screen when the festival logo is tapped: each
 * festival's own scene around the big moving logo, its greeting, and a way to turn festive
 * mode off. Everything is drawn here as a function of time, so it adds no images, animation
 * files or sound files to the app, and it costs nothing once it is closed.
 */

package com.blazify.music.festival

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.FestivalTouchesKey
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** How long the celebration stays before it fades by itself, in seconds. */
private const val TOTAL = 6.5f

/** Where the big logo's centre sits, as a share of the screen's height. */
private const val LOGO_Y = 0.36f

private val Gold = Color(0xFFFFC107)
private val PaleGold = Color(0xFFFFE082)
private val Cream = Color(0xFFFFF59D)
private val Amber = Color(0xFFFFA726)
private val Coral = Color(0xFFFF7043)
private val Clay = Color(0xFFB5532A)
private val ClayLight = Color(0xFFE07A3F)
private val ClayDark = Color(0xFF5A2614)

/**
 * The celebration for [festival]. Any tap ends it early; the pill at the top right turns
 * festive mode off for good (it can be turned back on in Look & Feel). With animations turned
 * off on the phone, it shows the finished picture and waits for a tap.
 */
@Composable
fun FestivalCelebration(
    festival: Festival,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    var festivalTouches by rememberPreference(FestivalTouchesKey, true)
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var t by remember { mutableFloatStateOf(if (still) 3.2f else 0f) }
    var leaving by remember { mutableStateOf(false) }
    var fade by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        // A soft chime, only when nothing is playing and the phone is not on silent or vibrate.
        val musicPlaying = playerConnection?.isPlaying?.value == true
        if (!still && !musicPlaying) withContext(Dispatchers.Default) { playChime(context) }
    }
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.12f)
            last = now
            t += dt
            if (t >= TOTAL - 0.6f) leaving = true
            if (leaving) {
                fade -= dt / 0.5f
                if (fade <= 0f) {
                    onDone()
                    break
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fade.coerceIn(0f, 1f) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (still) onDone() else leaving = true
                    },
        ) {
            val logoSize = min(maxWidth.value * 0.46f, 190f).dp
            val logoCentre = maxHeight * LOGO_Y
            Canvas(Modifier.fillMaxSize()) { drawScene(festival, t) }

            // The logo pops in, a little past full size and back.
            val pop = backOut(((t - 0.15f) / 0.6f).coerceIn(0f, 1f))
            FestivalLogoArt(
                festival = festival,
                isDark = true,
                size = logoSize,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = logoCentre - logoSize / 2)
                        .graphicsLayer {
                            scaleX = pop
                            scaleY = pop
                            alpha = pop.coerceIn(0f, 1f)
                        },
            )

            // Below the logo; for Tihar and Diwali, below the rangoli round it.
            val lights = festival == Festival.TIHAR || festival == Festival.DIWALI
            val textGap = if (lights) (min(maxWidth.value, maxHeight.value) * 0.42f).dp + 12.dp else logoSize / 2 + 24.dp
            val textIn = ((t - 1.2f) / 0.6f).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .offset(y = logoCentre + textGap)
                        .graphicsLayer {
                            alpha = textIn
                            val grow = 0.9f + 0.1f * textIn
                            scaleX = grow
                            scaleY = grow
                        },
            ) {
                Text(
                    text = stringResource(festival.greetingRes),
                    textAlign = TextAlign.Center,
                    style =
                        TextStyle(
                            brush = Brush.linearGradient(greetingColors(festival)),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 3f), 10f),
                        ),
                )
                Text(
                    text =
                        stringResource(
                            if (festival == Festival.TIHAR || festival == Festival.DIWALI) {
                                R.string.festival_wish_lights
                            } else {
                                R.string.festival_wish_joy
                            },
                        ),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 16.sp,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 2f), 8f)),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Surface(
                onClick = {
                    festivalTouches = false
                    Toast.makeText(context, R.string.festival_turned_off, Toast.LENGTH_LONG).show()
                    onDone()
                },
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 12.dp, end = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.festival_turn_off),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

/** The greeting's own festive colours. */
private fun greetingColors(festival: Festival): List<Color> =
    when (festival) {
        Festival.TIHAR, Festival.DIWALI -> listOf(Cream, Amber, Coral)
        Festival.CHRISTMAS -> listOf(Color.White, Color(0xFFFFCDD2), Color(0xFFEF5350))
        Festival.NEW_YEAR -> listOf(Gold, Color(0xFFF48FB1), Color(0xFF80DEEA))
        Festival.MUSIC_DAY -> listOf(Color(0xFFFF80AB), Amber, Color(0xFFB388FF))
        Festival.HOLI -> listOf(Color(0xFFFF4081), Color(0xFFFFEB3B), Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFE040FB))
        Festival.DASHAIN -> listOf(Color(0xFFFFF59D), Color(0xFFFFCA28), Color(0xFFFF8A65))
        Festival.LUNAR_NEW_YEAR -> listOf(Cream, Gold, Color(0xFFFFB300))
        Festival.EID_AL_FITR -> listOf(Cream, Gold, Color(0xFFFFB300))
        Festival.EID_AL_ADHA -> listOf(Color(0xFFC8E6C9), Color(0xFF81C784), Gold)
    }

// ── Scenes ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawScene(festival: Festival, t: Float) {
    // About one unit per dp on a phone, whatever its density.
    val u = size.minDimension / 360f
    val centre = Offset(size.width / 2, size.height * LOGO_Y)
    val dusk = (t / 0.5f).coerceIn(0f, 1f)
    when (festival) {
        Festival.TIHAR, Festival.DIWALI -> lightsScene(t, u, centre, dusk)
        Festival.CHRISTMAS -> christmasScene(t, u, dusk)
        Festival.NEW_YEAR -> newYearScene(t, u, centre, dusk)
        Festival.MUSIC_DAY -> musicScene(t, u, dusk)
        Festival.HOLI -> holiScene(t, u, centre, dusk)
        Festival.DASHAIN -> dashainScene(t, u, dusk)
        Festival.LUNAR_NEW_YEAR -> lunarScene(t, u, centre, dusk)
        Festival.EID_AL_FITR -> eidScene(t, u, dusk, green = false)
        Festival.EID_AL_ADHA -> eidScene(t, u, dusk, green = true)
    }
}

/** Tihar and Diwali: a rangoli blooms round the logo, sparks burst, diyas light along the bottom. */
private fun DrawScope.lightsScene(t: Float, u: Float, centre: Offset, dusk: Float) {
    sky(dusk, Color(0xFF1A0B2E), Color(0xFF2A0E14), Color(0xFF0B0A09))
    stars(t, u, dusk, 40, Cream, 0.62f)
    val maxR = min(size.width, size.height) * 0.5f
    val colors = listOf(Amber, Color(0xFFE91E63), Color(0xFF26A69A), Color(0xFFFFD54F))
    listOf(12, 16, 24).forEachIndexed { k, petals ->
        val grow = easeOut(((t - 0.4f - k * 0.22f) / 0.7f).coerceIn(0f, 1f))
        if (grow <= 0f) return@forEachIndexed
        val radius = maxR * (0.52f + 0.14f * k) * grow
        val length = maxR * (0.12f + 0.02f * k) * grow
        val turn = t * (if (k % 2 == 0) 6f else -6f) + k * 9f
        val color = colors[k % colors.size]
        for (i in 0 until petals) {
            rotate(turn + i * 360f / petals, centre) {
                petal(Offset(centre.x, centre.y - radius + length), length, length * 0.42f, color)
            }
        }
        drawCircle(color.copy(alpha = 0.35f * grow), radius * 0.98f, centre, style = Stroke(2f * u))
    }
    // Sparks burst in the sky round the rangoli, never behind the logo.
    listOf(Offset(0.18f, 0.13f), Offset(0.82f, 0.17f), Offset(0.22f, 0.8f), Offset(0.78f, 0.77f)).forEachIndexed { b, at ->
        burst(
            t,
            0.9f + b * 0.6f,
            Offset(size.width * at.x, size.height * at.y),
            listOf(Cream, Amber, Gold, Color(0xFFE91E63)),
            48,
            120f * u,
            2.2f,
            70f * u,
            u,
            seed = b,
        )
    }
    val count = 7
    val width = size.width / (count + 1.5f)
    for (i in 0 until count) {
        val lit = ((t - 0.7f - i * 0.16f) / 0.3f).coerceIn(0f, 1f)
        bottomDiya(Offset(size.width * (i + 1) / (count + 1), size.height - width * 0.9f), width * 0.82f, t * 1.3f + i * 0.37f, lit)
    }
}

/** Christmas: a winter night, strings of lights that twinkle, snow falling onto a snowy ground. */
private fun DrawScope.christmasScene(t: Float, u: Float, dusk: Float) {
    sky(dusk, Color(0xFF0B1A33), Color(0xFF16294D), Color(0xFF0A0F1F))
    stars(t, u, dusk, 30, Color.White, 0.5f)
    val bulbColors = listOf(Color(0xFFE53935), Gold, Color(0xFF43A047), Color(0xFF1E88E5))
    listOf(0.06f to 46f, 0.13f to 30f).forEachIndexed { row, (top, sag) ->
        val wire = Path()
        val n = 13
        for (i in 0..40) {
            val x = size.width * i / 40f
            val y = size.height * top + sag * u * (1f - (2f * i / 40f - 1f).let { it * it })
            if (i == 0) wire.moveTo(x, y) else wire.lineTo(x, y)
        }
        drawPath(wire, Color(0xFF263238).copy(alpha = dusk), style = Stroke(2f * u))
        for (i in 0 until n) {
            val f = (i + 0.5f + row * 0.5f) / n
            val x = size.width * f
            val y = size.height * top + sag * u * (1f - (2f * f - 1f).let { it * it }) + 7f * u
            val on = 0.45f + 0.55f * (0.5f + 0.5f * sin(t * 3.2f + i * 1.7f + row))
            val c = bulbColors[(i + row) % bulbColors.size]
            drawCircle(c.copy(alpha = 0.35f * on * dusk), 14f * u, Offset(x, y))
            drawOval(c.copy(alpha = (0.5f + 0.5f * on) * dusk), Offset(x - 4.5f * u, y - 6f * u), Size(9f * u, 12f * u))
            drawRect(Color(0xFF455A64).copy(alpha = dusk), Offset(x - 3f * u, y - 9f * u), Size(6f * u, 4f * u))
        }
    }
    val ground = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height * 0.9f)
        cubicTo(size.width * 0.3f, size.height * 0.85f, size.width * 0.6f, size.height * 0.93f, size.width, size.height * 0.87f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(ground, Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFB0BEC5)), size.height * 0.85f, size.height), alpha = dusk)
    for (i in 0 until 110) {
        val speed = (30f + 50f * hash(i, 1)) * u
        val y = (hash(i, 2) * size.height * 1.1f + t * speed) % (size.height * 1.1f) - size.height * 0.05f
        val x = hash(i, 3) * size.width + sin(t * 0.9f + i) * 14f * u
        drawCircle(Color.White.copy(alpha = (0.5f + 0.5f * hash(i, 4)) * dusk), (1.5f + 3f * hash(i, 5)) * u, Offset(x, y))
    }
}

/** New Year: fireworks bursting all over a midnight sky, and confetti coming down. */
private fun DrawScope.newYearScene(t: Float, u: Float, centre: Offset, dusk: Float) {
    sky(dusk, Color(0xFF05061A), Color(0xFF140A33), Color(0xFF000000))
    stars(t, u, dusk, 40, Color.White, 0.7f)
    val palettes =
        listOf(
            listOf(Gold, Cream, Coral),
            listOf(Color(0xFFEC407A), Color(0xFFF8BBD0), Gold),
            listOf(Color(0xFF26C6DA), Color(0xFFB388FF), Color.White),
            listOf(Color(0xFF66BB6A), Color(0xFFFFEE58), Color.White),
        )
    for (k in 0 until 11) {
        val start = 0.3f + k * 0.48f
        val at = Offset(size.width * (0.15f + 0.7f * hash(k, 7)), size.height * (0.1f + 0.45f * hash(k, 8)))
        // The rocket climbs for half a second before it bursts.
        val climb = ((t - start + 0.5f) / 0.5f).coerceIn(0f, 1f)
        if (climb in 0.01f..0.99f) {
            val y = size.height - (size.height - at.y) * easeOut(climb)
            drawLine(Cream.copy(alpha = 0.8f), Offset(at.x, y), Offset(at.x, y + 26f * u), 2.5f * u, StrokeCap.Round)
        }
        burst(t, start, at, palettes[k % palettes.size], 56, 150f * u, 1.9f, 60f * u, u, seed = k + 10)
    }
    confetti(t, u, dusk, 70)
}

/** World Music Day: an equaliser dancing along the bottom, and notes floating up. */
private fun DrawScope.musicScene(t: Float, u: Float, dusk: Float) {
    sky(dusk, Color(0xFF1A0B3D), Color(0xFF3B0F5C), Color(0xFF0B0618))
    stars(t, u, dusk, 30, Color(0xFFE1BEE7), 0.6f)
    val bars = 24
    val gap = 4f * u
    val barWidth = (size.width - gap * (bars + 1)) / bars
    for (i in 0 until bars) {
        val level = 0.15f + 0.85f * abs(sin(t * (2.2f + hash(i, 1) * 2.5f) + i * 0.7f))
        val h = size.height * 0.2f * level * dusk
        val x = gap + i * (barWidth + gap)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFFF4081), Amber), size.height - h, size.height),
            Offset(x, size.height - h),
            Size(barWidth, h),
            CornerRadius(barWidth / 2),
        )
    }
    val colors = listOf(Color(0xFFEC407A), Color(0xFF26C6DA), Gold, Color(0xFFB388FF), Color(0xFF69F0AE))
    for (i in 0 until 18) {
        val speed = (50f + 40f * hash(i, 2)) * u
        val travel = size.height * 0.9f
        val p = (hash(i, 3) * travel + t * speed) % travel
        val y = size.height * 0.85f - p
        val x = hash(i, 4) * size.width + sin(t * 1.4f + i) * 18f * u
        val fadeInOut = (p / (travel * 0.15f)).coerceAtMost(1f) * ((travel - p) / (travel * 0.2f)).coerceAtMost(1f)
        musicNote(Offset(x, y), (0.8f + 0.6f * hash(i, 5)) * u, colors[i % colors.size].copy(alpha = fadeInOut * dusk), i % 3 == 0)
    }
}

/** Holi: clouds of colour powder thrown into the air from all sides. */
private fun DrawScope.holiScene(t: Float, u: Float, centre: Offset, dusk: Float) {
    sky(dusk, Color(0xFF1B0B2E), Color(0xFF2D0B3A), Color(0xFF120818))
    val colors =
        listOf(Color(0xFFEC407A), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFF7043))
    for (k in 0 until 12) {
        val start = 0.2f + k * 0.38f
        val at = Offset(size.width * (0.1f + 0.8f * hash(k, 11)), size.height * (0.12f + 0.75f * hash(k, 12)))
        powder(t, start, at, colors[k % colors.size], u, seed = k)
    }
    for (i in 0 until 60) {
        val c = colors[i % colors.size]
        val x = hash(i, 13) * size.width + sin(t * 0.7f + i) * 20f * u
        val y = (hash(i, 14) * size.height - t * (10f + 20f * hash(i, 15)) * u + size.height) % size.height
        drawCircle(c.copy(alpha = 0.55f * dusk), (1.5f + 2.5f * hash(i, 16)) * u, Offset(x, y))
    }
}

/** Dashain: an evening sky full of kites, and clouds drifting by. */
private fun DrawScope.dashainScene(t: Float, u: Float, dusk: Float) {
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFF0B3D91),
            0.62f to Color(0xFF1565C0),
            1f to Color(0xFFF57C00),
        ),
        alpha = dusk,
    )
    for (i in 0 until 5) {
        val x = ((hash(i, 21) * 1.4f - 0.2f) * size.width + t * (8f + 10f * hash(i, 22)) * u) % (size.width * 1.4f) - size.width * 0.2f
        val y = size.height * (0.08f + 0.6f * hash(i, 23))
        cloud(Offset(x, y), (26f + 22f * hash(i, 24)) * u, dusk)
    }
    val kiteColors =
        listOf(
            listOf(Color(0xFFFDD835), Color(0xFFE53935), Color(0xFF1E88E5)),
            listOf(Color(0xFFE91E63), Color(0xFFFFEB3B), Color(0xFF00897B)),
            listOf(Color(0xFF7E57C2), Color(0xFFFF7043), Color(0xFFFFF176)),
            listOf(Color(0xFF43A047), Color(0xFFFFFFFF), Color(0xFFE53935)),
        )
    val kites = 7
    for (i in 0 until kites) {
        val rise = easeOut(((t - 0.1f - i * 0.15f) / 1.2f).coerceIn(0f, 1f))
        val homeX = size.width * (0.08f + 0.84f * (i + 0.5f) / kites)
        val homeY = size.height * (0.08f + 0.22f * hash(i, 25)) + if (i == kites / 2) size.height * 0.5f else 0f
        val x = homeX + sin(t * (0.9f + 0.4f * hash(i, 26)) + i) * 16f * u
        val y = size.height + (homeY - size.height) * rise + cos(t * 1.3f + i) * 10f * u
        val anchor = Offset(size.width * (if (i % 2 == 0) 0.2f else 0.8f), size.height)
        drawLine(Color.White.copy(alpha = 0.35f * dusk), Offset(x, y + 22f * u), anchor, 1f * u)
        kite(Offset(x, y), (18f + 8f * hash(i, 27)) * u, sin(t * 1.1f + i) * 10f, kiteColors[i % kiteColors.size], t, i)
    }
}

/** Lunar New Year: rows of red lanterns swinging, gold sparkles, and fireworks in red and gold. */
private fun DrawScope.lunarScene(t: Float, u: Float, centre: Offset, dusk: Float) {
    sky(dusk, Color(0xFF3B0000), Color(0xFF7F0000), Color(0xFF1A0000))
    for (k in 0 until 5) {
        val at = Offset(size.width * (0.15f + 0.7f * hash(k, 31)), size.height * (0.55f + 0.25f * hash(k, 32)))
        burst(t, 0.8f + k * 0.9f, at, listOf(Gold, Cream, Color(0xFFFF5252)), 48, 120f * u, 1.8f, 50f * u, u, seed = k + 30)
    }
    val n = 6
    for (i in 0 until n) {
        val x = size.width * (i + 0.5f) / n
        val cord = size.height * (0.06f + 0.1f * hash(i, 33)) * easeOut(((t - i * 0.1f) / 0.8f).coerceIn(0f, 1f))
        val swing = sin(t * 1.6f + i * 0.8f) * 7f
        rotate(swing, Offset(x, 0f)) {
            drawLine(Color(0xFFFFD54F).copy(alpha = dusk), Offset(x, 0f), Offset(x, cord), 1.6f * u)
            chineseLantern(Offset(x, cord), (16f + 6f * hash(i, 34)) * u, dusk)
        }
    }
    stars(t, u, dusk, 36, Gold, 1f)
}

/** Eid: a crescent moon over a starry night, and lanterns (fanous) glowing as they swing. */
private fun DrawScope.eidScene(t: Float, u: Float, dusk: Float, green: Boolean) {
    sky(dusk, Color(0xFF04121F), Color(0xFF0B2A3C), Color(0xFF020A12))
    stars(t, u, dusk, 60, PaleGold, 0.75f)
    val moonAt = Offset(size.width * 0.2f, size.height * 0.12f)
    val r = 38f * u
    val rise = easeOut((t / 1.2f).coerceIn(0f, 1f))
    drawCircle(
        Brush.radialGradient(listOf(PaleGold.copy(alpha = 0.35f * rise), Color.Transparent), moonAt, r * 2.4f),
        r * 2.4f,
        moonAt,
    )
    val moon =
        Path.combine(
            PathOperation.Difference,
            Path().apply { addOval(Rect(moonAt, r)) },
            Path().apply { addOval(Rect(Offset(moonAt.x + r * 0.45f, moonAt.y - r * 0.25f), r * 0.82f)) },
        )
    drawPath(
        moon,
        if (green) {
            Brush.linearGradient(listOf(Color(0xFFA5D6A7), Color(0xFF2E7D32)), moonAt - Offset(r, r), moonAt + Offset(r, r))
        } else {
            Brush.linearGradient(listOf(Cream, Gold, Color(0xFFF57F17)), moonAt - Offset(r, r), moonAt + Offset(r, r))
        },
        alpha = rise,
    )
    val n = 4
    for (i in 0 until n) {
        val x = size.width * (0.45f + 0.5f * i / (n - 1))
        val cord = size.height * (0.05f + 0.1f * hash(i, 41)) * easeOut(((t - 0.2f - i * 0.12f) / 0.8f).coerceIn(0f, 1f))
        rotate(sin(t * 1.4f + i) * 6f, Offset(x, 0f)) {
            drawLine(Color(0xFFB8860B).copy(alpha = dusk), Offset(x, 0f), Offset(x, cord), 1.5f * u)
            fanous(Offset(x, cord), (14f + 5f * hash(i, 42)) * u, t, green, dusk)
        }
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

private fun DrawScope.sky(alpha: Float, top: Color, middle: Color, bottom: Color) =
    drawRect(Brush.verticalGradient(listOf(top, middle, bottom)), alpha = 0.985f * alpha)

private fun DrawScope.stars(t: Float, u: Float, alpha: Float, count: Int, color: Color, height: Float) {
    for (i in 0 until count) {
        val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 2.2f + i * 0.9f))
        drawCircle(
            color.copy(alpha = 0.55f * twinkle * alpha),
            (1f + 1.6f * hash(i, 91)) * u,
            Offset(hash(i, 92) * size.width, hash(i, 93) * size.height * height),
        )
    }
}

/**
 * A burst of [count] sparks thrown out from [at] at [start] seconds: they slow down, fall and
 * fade over [life] seconds. Worked out from the time alone, so nothing has to be kept.
 */
private fun DrawScope.burst(
    t: Float,
    start: Float,
    at: Offset,
    colors: List<Color>,
    count: Int,
    speed: Float,
    life: Float,
    gravity: Float,
    u: Float,
    seed: Int,
) {
    val age = t - start
    if (age < 0f || age > life) return
    val drag = 1.8f
    val travel = (1f - exp(-drag * age)) / drag
    val fade = 1f - age / life
    if (age < 0.25f) drawCircle(Color.White.copy(alpha = 0.6f * (1f - age / 0.25f)), 26f * u, at)
    for (i in 0 until count) {
        val a = i * 2f * PI.toFloat() / count + hash(i, seed) * 0.3f
        val s = speed * (0.55f + 0.45f * hash(i, seed + 50))
        val p = Offset(at.x + cos(a) * s * travel, at.y + sin(a) * s * travel + 0.5f * gravity * age * age)
        val c = colors[i % colors.size]
        drawCircle(c.copy(alpha = 0.25f * fade), 7f * u, p)
        drawCircle(c.copy(alpha = fade), 2.3f * u, p)
    }
}

/** A puff of colour powder that bursts out soft and wide and slowly settles. */
private fun DrawScope.powder(t: Float, start: Float, at: Offset, color: Color, u: Float, seed: Int) {
    val age = t - start
    val life = 3f
    if (age < 0f || age > life) return
    val spread = 1f - exp(-2.6f * age)
    val fade = (1f - age / life).let { it * it }
    for (i in 0 until 16) {
        val a = hash(i, seed + 60) * 2f * PI.toFloat()
        val d = (30f + 70f * hash(i, seed + 61)) * u * spread
        val p = Offset(at.x + cos(a) * d, at.y + sin(a) * d * 0.8f + 12f * u * age * age)
        val r = (22f + 26f * hash(i, seed + 62)) * u * (0.5f + 0.5f * spread)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f * fade), Color.Transparent), p, r), r, p)
    }
}

private fun DrawScope.confetti(t: Float, u: Float, alpha: Float, count: Int) {
    val colors = listOf(Color(0xFFEC407A), Gold, Color(0xFF26C6DA), Color(0xFF7E57C2), Coral, Color(0xFF66BB6A))
    for (i in 0 until count) {
        val speed = (60f + 60f * hash(i, 71)) * u
        val y = (hash(i, 72) * size.height + t * speed) % (size.height * 1.05f) - size.height * 0.03f
        val x = hash(i, 73) * size.width + sin(t * 2f + i) * 12f * u
        rotate(t * (90f + 200f * hash(i, 74)) + i * 40f, Offset(x, y)) {
            drawRect(colors[i % colors.size].copy(alpha = alpha), Offset(x - 4f * u, y - 2f * u), Size(8f * u, 4f * u))
        }
    }
}

private fun DrawScope.musicNote(at: Offset, s: Float, color: Color, double: Boolean) {
    fun head(c: Offset) {
        rotate(-22f, c) { drawOval(color, Offset(c.x - 7f * s, c.y - 5f * s), Size(14f * s, 10f * s)) }
        drawRect(color, Offset(c.x + 5f * s, c.y - 30f * s), Size(2.6f * s, 30f * s))
    }
    head(at)
    if (double) {
        val second = Offset(at.x + 22f * s, at.y - 5f * s)
        head(second)
        val beam = Path().apply {
            moveTo(at.x + 5f * s, at.y - 30f * s)
            lineTo(second.x + 7.6f * s, second.y - 30f * s)
            lineTo(second.x + 7.6f * s, second.y - 23f * s)
            lineTo(at.x + 5f * s, at.y - 23f * s)
            close()
        }
        drawPath(beam, color)
    } else {
        val flag = Path().apply {
            moveTo(at.x + 7.6f * s, at.y - 30f * s)
            relativeQuadraticTo(10f * s, 4f * s, 8f * s, 16f * s)
            relativeQuadraticTo(-2f * s, -7f * s, -8f * s, -9f * s)
            close()
        }
        drawPath(flag, color)
    }
}

private fun DrawScope.cloud(at: Offset, r: Float, alpha: Float) {
    val c = Color.White.copy(alpha = 0.16f * alpha)
    drawCircle(c, r, at)
    drawCircle(c, r * 0.75f, Offset(at.x - r, at.y + r * 0.25f))
    drawCircle(c, r * 0.8f, Offset(at.x + r, at.y + r * 0.2f))
}

/** A diamond kite with its colour panels and a tail of bows. */
private fun DrawScope.kite(at: Offset, s: Float, tilt: Float, colors: List<Color>, t: Float, i: Int) {
    rotate(tilt, at) {
        val top = Offset(at.x, at.y - s)
        val right = Offset(at.x + s * 0.8f, at.y)
        val bottom = Offset(at.x, at.y + s * 1.2f)
        val left = Offset(at.x - s * 0.8f, at.y)
        fun tri(a: Offset, b: Offset, c: Color) =
            drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(at.x, at.y); close() }, c)
        tri(top, right, colors[0])
        tri(right, bottom, colors[1])
        tri(bottom, left, colors[0])
        tri(left, top, colors[2])
        drawLine(Color(0xFF3E2723), top, bottom, 1.2f)
        drawLine(Color(0xFF3E2723), left, right, 1.2f)
        val tail = Path().apply {
            moveTo(bottom.x, bottom.y)
            for (k in 1..6) lineTo(bottom.x + sin(t * 4f + k * 0.9f + i) * s * 0.25f, bottom.y + k * s * 0.32f)
        }
        drawPath(tail, colors[1], style = Stroke(1.5f))
    }
}

private fun DrawScope.chineseLantern(top: Offset, r: Float, alpha: Float) {
    val body = Rect(top.x - r, top.y + r * 0.3f, top.x + r, top.y + r * 2.1f)
    drawCircle(Color(0xFFFF8A65).copy(alpha = 0.35f * alpha), r * 1.8f, body.center)
    drawRect(Gold.copy(alpha = alpha), Offset(top.x - r * 0.5f, top.y), Size(r, r * 0.35f))
    drawOval(Brush.verticalGradient(listOf(Color(0xFFEF5350), Color(0xFFB71C1C)), body.top, body.bottom), body.topLeft, body.size, alpha = alpha)
    val ribs = Path().apply {
        moveTo(top.x, body.top)
        quadraticTo(top.x - r * 0.9f, body.center.y, top.x, body.bottom)
        moveTo(top.x, body.top)
        quadraticTo(top.x + r * 0.9f, body.center.y, top.x, body.bottom)
    }
    drawPath(ribs, Gold.copy(alpha = 0.8f * alpha), style = Stroke(r * 0.07f))
    drawRect(Gold.copy(alpha = alpha), Offset(top.x - r * 0.5f, body.bottom - r * 0.05f), Size(r, r * 0.3f))
    for (k in -1..1) {
        drawLine(Gold.copy(alpha = alpha), Offset(top.x + k * r * 0.25f, body.bottom + r * 0.25f), Offset(top.x + k * r * 0.3f, body.bottom + r * 1.1f), r * 0.08f)
    }
}

private fun DrawScope.fanous(top: Offset, r: Float, t: Float, green: Boolean, alpha: Float) {
    val brass = Color(0xFFB8860B)
    val glow = 0.75f + 0.25f * sin(t * 2.4f + top.x)
    val cap = Path().apply {
        moveTo(top.x - r * 0.4f, top.y)
        lineTo(top.x + r * 0.4f, top.y)
        lineTo(top.x + r * 0.75f, top.y + r * 0.5f)
        lineTo(top.x - r * 0.75f, top.y + r * 0.5f)
        close()
    }
    drawPath(cap, Brush.verticalGradient(listOf(PaleGold, Gold), top.y, top.y + r * 0.5f), alpha = alpha)
    val glass = Path().apply {
        moveTo(top.x - r * 0.75f, top.y + r * 0.5f)
        lineTo(top.x + r * 0.75f, top.y + r * 0.5f)
        lineTo(top.x + r * 0.95f, top.y + r * 1.9f)
        lineTo(top.x, top.y + r * 2.7f)
        lineTo(top.x - r * 0.95f, top.y + r * 1.9f)
        close()
    }
    val lightAt = Offset(top.x, top.y + r * 1.5f)
    drawCircle((if (green) Color(0xFF81C784) else Color(0xFFFFCA28)).copy(alpha = 0.3f * glow * alpha), r * 2.2f, lightAt)
    drawPath(
        glass,
        Brush.radialGradient(
            listOf(
                (if (green) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)).copy(alpha = glow),
                if (green) Color(0xFF43A047) else Color(0xFFFFB300),
            ),
            lightAt,
            r * 1.4f,
        ),
        alpha = alpha,
    )
    drawPath(glass, brass.copy(alpha = alpha), style = Stroke(r * 0.1f))
    drawLine(brass.copy(alpha = alpha), Offset(top.x, top.y + r * 0.5f), Offset(top.x, top.y + r * 2.7f), r * 0.07f)
    drawLine(brass.copy(alpha = alpha), Offset(top.x - r * 0.85f, top.y + r * 1.2f), Offset(top.x + r * 0.85f, top.y + r * 1.2f), r * 0.07f)
}

/** A clay lamp along the bottom of the Tihar and Diwali scene, centred at [base] (its rim). */
private fun DrawScope.bottomDiya(base: Offset, width: Float, flicker: Float, lit: Float) {
    val bowl = Path().apply {
        moveTo(base.x - width / 2, base.y)
        cubicTo(base.x - width / 2.4f, base.y + width * 0.42f, base.x + width / 2.4f, base.y + width * 0.42f, base.x + width / 2, base.y)
        quadraticTo(base.x + width * 0.62f, base.y - width * 0.08f, base.x + width * 0.42f, base.y - width * 0.02f)
        close()
    }
    drawPath(bowl, Brush.verticalGradient(listOf(ClayLight, Clay, ClayDark), base.y - width * 0.1f, base.y + width * 0.4f))
    drawOval(ClayDark, Offset(base.x - width / 2, base.y - width * 0.06f), Size(width, width * 0.12f))
    if (lit <= 0f) return
    val wobble = sin(flicker * 2 * PI).toFloat()
    val height = width * (0.55f + 0.06f * wobble) * lit
    val tip = Offset(base.x + width * 0.04f * wobble, base.y - width * 0.05f - height)
    drawCircle(
        Brush.radialGradient(listOf(Amber.copy(alpha = 0.55f * lit), Color.Transparent), Offset(base.x, base.y - height * 0.5f), width * 0.9f),
        radius = width * 0.9f,
        center = Offset(base.x, base.y - height * 0.5f),
    )
    val flame = Path().apply {
        moveTo(base.x, base.y - width * 0.04f)
        cubicTo(base.x - width * 0.2f, base.y - height * 0.35f, tip.x - width * 0.05f, tip.y + height * 0.3f, tip.x, tip.y)
        cubicTo(tip.x + width * 0.05f, tip.y + height * 0.3f, base.x + width * 0.2f, base.y - height * 0.35f, base.x, base.y - width * 0.04f)
        close()
    }
    drawPath(flame, Brush.verticalGradient(listOf(Cream, Color(0xFFFFB300), Coral), tip.y, base.y), alpha = lit)
}

/** One rangoli petal, a pointed leaf from [base] pointing away from the centre. */
private fun DrawScope.petal(base: Offset, length: Float, width: Float, color: Color) {
    val leaf = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(base.x - width, base.y - length * 0.35f, base.x - width * 0.4f, base.y - length * 0.85f, base.x, base.y - length)
        cubicTo(base.x + width * 0.4f, base.y - length * 0.85f, base.x + width, base.y - length * 0.35f, base.x, base.y)
        close()
    }
    drawPath(leaf, color)
    drawPath(leaf, Color.White.copy(alpha = 0.25f), style = Stroke(1.5f))
}

/** A fixed pseudo-random number in 0..1 for item [i] and stream [k]: the same every frame. */
private fun hash(i: Int, k: Int): Float {
    val x = sin(i * 12.9898f + k * 78.233f) * 43758.547f
    return x - floor(x)
}

private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)

/** Eases out past 1 and settles back, for things that pop in. */
private fun backOut(x: Float): Float {
    val c = 1.7f
    val y = x - 1f
    return 1f + (c + 1f) * y * y * y + c * y * y
}

/** Three soft bell strikes, made here rather than shipped as a sound file. */
private fun playChime(context: Context) {
    val audio = context.getSystemService(AudioManager::class.java) ?: return
    if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
    val rate = 44_100
    val notes = listOf(659.25 to 0.0, 830.61 to 0.22, 987.77 to 0.44)
    val seconds = 2.0
    val samples = ShortArray((rate * seconds).toInt())
    for (i in samples.indices) {
        val time = i.toDouble() / rate
        var v = 0.0
        notes.forEach { (freq, start) ->
            val local = time - start
            if (local >= 0) {
                val envelope = exp(-local * 3.2) * kotlin.math.min(1.0, local * 200)
                v += envelope * (sin(2 * PI * freq * local) + 0.35 * sin(2 * PI * freq * 2.76 * local))
            }
        }
        samples[i] = (v * 0.16 * Short.MAX_VALUE).coerceIn(-32767.0, 32767.0).toInt().toShort()
    }
    runCatching {
        val track =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
        track.write(samples, 0, samples.size)
        track.play()
        Thread.sleep((seconds * 1000).toLong())
        track.release()
    }
}
