/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * How a festival looks: a warm glow and a lit diya on the Home logo, and, when the logo is
 * tapped, a few seconds of celebration over the whole screen. All of it drawn here, so it
 * adds no images, no animation files and no sound files to the app.
 */

package com.blazify.music.festival

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
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
import com.blazify.music.BuildConfig
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.ContentCountryKey
import com.blazify.music.constants.FestivalTouchesKey
import com.blazify.music.constants.SYSTEM_DEFAULT
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val Marigold = Color(0xFFFFA726)
private val Flame = Color(0xFFFFB300)
private val FlameCore = Color(0xFFFFF59D)
private val Clay = Color(0xFFB5532A)
private val ClayLight = Color(0xFFE07A3F)
private val ClayDark = Color(0xFF5A2614)
private val RangoliColors = listOf(Color(0xFFFFA726), Color(0xFFE91E63), Color(0xFF26A69A), Color(0xFFFFD54F))

/** Test copies built with -PforceFestival=all step through every festival with [FestivalTestButton]. */
private val testIndex = androidx.compose.runtime.mutableIntStateOf(0)

private val testingAll get() = BuildConfig.FORCE_FESTIVAL == "all"

/** Today's festival here, or null: none today, none in this country, or switched off. */
@Composable
fun rememberActiveFestival(): Festival? {
    val context = LocalContext.current
    val enabled by rememberPreference(FestivalTouchesKey, true)
    val chosenCountry by rememberPreference(ContentCountryKey, SYSTEM_DEFAULT)
    if (testingAll) return Festival.entries[testIndex.intValue % Festival.entries.size]
    if (!enabled) return null
    return remember(chosenCountry) {
        // Test copies can be built to pretend it is a festival day (-PforceFestival=tihar);
        // the copies people install are built without it.
        Festival.entries.firstOrNull { it.id == BuildConfig.FORCE_FESTIVAL }
            ?: Festival.on(LocalDate.now(), festivalCountry(context, chosenCountry.takeUnless { it == SYSTEM_DEFAULT }))
    }
}

/**
 * The Home logo in festival dress, moving, at exactly the logo's own size: the decoration lives
 * inside the logo's square, so nothing in the header moves on a festival day. Tapping it plays
 * the celebration.
 */
@Composable
fun FestivalLogo(
    festival: Festival,
    isDark: Boolean,
    logoSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    var celebrating by remember { mutableStateOf(false) }
    FestivalLogoArt(
        festival = festival,
        isDark = isDark,
        size = logoSize,
        modifier =
            modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { celebrating = true },
    )
    if (celebrating) FestivalCelebration(festival) { celebrating = false }
}

/** Only in test copies built with -PforceFestival=all: a button over Home to step to the next festival. */
@Composable
fun FestivalTestButton(modifier: Modifier = Modifier) {
    if (!testingAll) return
    val festival = Festival.entries[testIndex.intValue % Festival.entries.size]
    androidx.compose.material3.Surface(
        onClick = { testIndex.intValue = (testIndex.intValue + 1) % Festival.entries.size },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = Color(0xE6202020),
        contentColor = Color.White,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Text(
            text = "${festival.id}  ${festival.ordinal + 1}/${Festival.entries.size}  ›",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/**
 * A few seconds over the whole screen: diyas light one by one along the bottom, a rangoli
 * blooms in the middle, sparks rise, and the greeting comes in. Any tap ends it early. With
 * animations turned off on the phone, it shows the finished picture and waits for a tap.
 */
@Composable
fun FestivalCelebration(
    festival: Festival,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val total = 5.2f
    var t by remember { mutableFloatStateOf(if (still) 3.6f else 0f) }
    var leaving by remember { mutableStateOf(false) }
    var fade by remember { mutableFloatStateOf(1f) }
    val sparks = remember { Sparks() }

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
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            t += dt
            sparks.step(dt, t)
            if (t >= total - 0.6f) leaving = true
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fade }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (still) onDone() else leaving = true
                    },
        ) {
            Canvas(Modifier.fillMaxSize()) { drawCelebration(t, sparks) }
            val textIn = ((t - 1.5f) / 0.6f).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .offset(y = 150.dp)
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
                            brush = Brush.linearGradient(listOf(FlameCore, Marigold, Color(0xFFFF7043))),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                )
                Text(
                    text = stringResource(R.string.festival_wish_lights),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun DrawScope.drawCelebration(t: Float, sparks: Sparks) {
    val w = size.width
    val h = size.height
    // A warm night falls over the screen.
    val dusk = (t / 0.5f).coerceIn(0f, 1f)
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF1A0B2E), Color(0xFF2A0E14), Color(0xFF0B0A09))),
        alpha = 0.985f * dusk,
    )
    // A sky of small lights twinkling above the rangoli.
    for (i in 0 until 40) {
        val sx = ((i * 73) % 100) / 100f * w
        val sy = ((i * 37) % 100) / 100f * h * 0.62f
        val twinkle = 0.35f + 0.65f * ((sin((t * 2.2f + i * 0.9f).toDouble()).toFloat() + 1f) / 2f)
        drawCircle(FlameCore.copy(alpha = 0.55f * twinkle * dusk), radius = 2.2f + (i % 3), center = Offset(sx, sy))
    }

    // The rangoli blooms ring by ring, turning slowly.
    val center = Offset(w / 2, h * 0.36f)
    val maxR = min(w, h) * 0.34f
    val rings = listOf(8, 12, 16, 24)
    rings.forEachIndexed { k, petals ->
        val grow = easeOut(((t - 0.4f - k * 0.22f) / 0.7f).coerceIn(0f, 1f))
        if (grow <= 0f) return@forEachIndexed
        val radius = maxR * (0.28f + 0.24f * k) * grow
        val length = maxR * (0.22f + 0.05f * k) * grow
        val turn = (t * (if (k % 2 == 0) 6f else -6f)) + k * 9f
        val color = RangoliColors[k % RangoliColors.size]
        for (i in 0 until petals) {
            val angle = turn + i * 360f / petals
            rotate(angle, center) {
                drawPetal(Offset(center.x, center.y - radius + length), length, length * 0.42f, color)
            }
        }
        drawCircle(color.copy(alpha = 0.35f * grow), radius = radius * 0.98f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
    }
    val heart = easeOut(((t - 0.3f) / 0.5f).coerceIn(0f, 1f))
    drawCircle(Brush.radialGradient(listOf(FlameCore, Marigold, Color.Transparent), center, maxR * 0.25f), radius = maxR * 0.25f * heart, center = center)

    // Diyas along the bottom, lit one after another.
    val count = 7
    val diyaWidth = w / (count + 1.5f)
    for (i in 0 until count) {
        val x = w * (i + 1) / (count + 1)
        val y = h - diyaWidth * 0.9f
        val lit = ((t - 0.7f - i * 0.16f) / 0.3f).coerceIn(0f, 1f)
        drawDiya(Offset(x, y), diyaWidth * 0.82f, flicker = (t * 1.3f + i * 0.37f) % 1f, lit = lit)
    }

    sparks.draw(this)
}

/** A clay lamp with its flame, centred at [base] (the rim of the bowl). */
private fun DrawScope.drawDiya(base: Offset, width: Float, flicker: Float, lit: Float) {
    val bowl =
        Path().apply {
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
        Brush.radialGradient(listOf(Marigold.copy(alpha = 0.55f * lit), Color.Transparent), Offset(base.x, base.y - height * 0.5f), width * 0.9f),
        radius = width * 0.9f,
        center = Offset(base.x, base.y - height * 0.5f),
    )
    val flame =
        Path().apply {
            moveTo(base.x, base.y - width * 0.04f)
            cubicTo(base.x - width * 0.2f, base.y - height * 0.35f, tip.x - width * 0.05f, tip.y + height * 0.3f, tip.x, tip.y)
            cubicTo(tip.x + width * 0.05f, tip.y + height * 0.3f, base.x + width * 0.2f, base.y - height * 0.35f, base.x, base.y - width * 0.04f)
            close()
        }
    drawPath(flame, Brush.verticalGradient(listOf(FlameCore, Flame, Color(0xFFFF7043)), tip.y, base.y), alpha = lit)
}

/** One rangoli petal, a pointed leaf from [base] pointing away from the centre. */
private fun DrawScope.drawPetal(base: Offset, length: Float, width: Float, color: Color) {
    val petal =
        Path().apply {
            moveTo(base.x, base.y)
            cubicTo(base.x - width, base.y - length * 0.35f, base.x - width * 0.4f, base.y - length * 0.85f, base.x, base.y - length)
            cubicTo(base.x + width * 0.4f, base.y - length * 0.85f, base.x + width, base.y - length * 0.35f, base.x, base.y)
            close()
        }
    drawPath(petal, color)
    drawPath(petal, Color.White.copy(alpha = 0.25f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
}

private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)

/** Golden sparks that burst out of the rangoli and drift down. */
private class Sparks {
    private class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val hue: Color)

    private val list = ArrayList<Spark>(160)
    private val random = Random(7)
    private var bursts = 0

    fun step(dt: Float, t: Float) {
        // Three bursts as the rangoli opens, then a gentle drift.
        val due = listOf(0.9f, 1.6f, 2.4f)
        while (bursts < due.size && t >= due[bursts]) {
            repeat(70) {
                val a = random.nextFloat() * 2 * PI.toFloat()
                val speed = 0.18f + random.nextFloat() * 0.32f
                list += Spark(0.5f, 0.36f, cos(a) * speed, sin(a) * speed, 1f, if (random.nextBoolean()) FlameCore else Marigold)
            }
            bursts++
        }
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            s.vy += 0.25f * dt
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.vx *= exp(-1.6f * dt)
            s.life -= dt / 2.4f
            if (s.life <= 0f) iterator.remove()
        }
    }

    fun draw(scope: DrawScope) {
        with(scope) {
            val w = size.width
            val h = size.height
            list.forEach { s ->
                translate(s.x * w, s.y * h) {
                    drawCircle(s.hue.copy(alpha = 0.3f * s.life), radius = 14f)
                    drawCircle(s.hue.copy(alpha = s.life), radius = 4.5f)
                }
            }
        }
    }
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
                val envelope = exp(-local * 3.2) * min(1.0, local * 200)
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
