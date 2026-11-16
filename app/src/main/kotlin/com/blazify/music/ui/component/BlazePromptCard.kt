/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * The card the app asks things with: a new version is ready, or would you star Blazify. One
 * compact card, an object drawn in 3D breaking out of its top edge, a title, two lines and two
 * buttons. The objects are drawn here rather than shipped as pictures, so they stay sharp at
 * any size and follow light and dark themes.
 */

package com.blazify.music.ui.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Amber = Color(0xFFFFA726)
private val Coral = Color(0xFFFF7043)
private val ButtonInk = Color(0xFF1A0E00)

/**
 * The card's measurements. On a short screen, a phone on its side or a small phone with large
 * text, the picture, its sky and the gaps shrink so the card still fits; if it still does not,
 * it scrolls.
 */
private class PromptSizes(val overflow: Dp, val art: Dp, val sky: Dp, val gap: Dp)

private val RoomySizes = PromptSizes(overflow = 46.dp, art = 132.dp, sky = 84.dp, gap = 18.dp)
private val CompactSizes = PromptSizes(overflow = 32.dp, art = 88.dp, sky = 58.dp, gap = 12.dp)

/** The two buttons of a [BlazePromptCard]; a null label leaves that button out. */
data class PromptButtons(
    val primary: String?,
    val onPrimary: () -> Unit,
    val secondary: String?,
    val onSecondary: () -> Unit,
)

@Composable
fun BlazePromptCard(
    title: String,
    onDismissRequest: () -> Unit,
    buttons: PromptButtons,
    art: DrawScope.(time: Float) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val enter = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 380f)) }
    // One slow clock for everything that moves in the picture: a full turn every six seconds.
    val clock = rememberInfiniteTransition(label = "promptArt")
    val tick by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "promptTime",
    )
    val time = if (still) 0.15f else tick

    Dialog(onDismissRequest = onDismissRequest) {
        PromptCardLayout(
            title = title,
            buttons = buttons,
            time = time,
            art = art,
            content = content,
            modifier =
                Modifier.graphicsLayer {
                    alpha = enter.value.coerceIn(0f, 1f)
                    val grow = 0.94f + 0.06f * enter.value
                    scaleX = grow
                    scaleY = grow
                    translationY = (1f - enter.value) * 28.dp.toPx()
                },
        )
    }
}

/** The card itself, outside any window, so it can also be drawn on its own (the preview test does). */
@Composable
internal fun PromptCardLayout(
    title: String,
    buttons: PromptButtons,
    time: Float,
    art: DrawScope.(time: Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val glow = if (dark) Amber.copy(alpha = 0.20f) else Amber.copy(alpha = 0.16f)
    val config = LocalConfiguration.current
    // Short screens: landscape phones, and small phones once large text takes the room.
    val sizes = if (config.screenHeightDp / config.fontScale.coerceAtLeast(1f) < 560) CompactSizes else RoomySizes
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier =
            modifier
                .widthIn(max = 300.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 16.dp,
            modifier = Modifier.padding(top = sizes.overflow),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // The sky the object flies in: a warm glow with notes drifting in it.
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(sizes.sky),
                ) {
                    drawRect(Brush.verticalGradient(listOf(glow, Color.Transparent)))
                    drawSkyNotes(time, if (dark) Color.White else Amber)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = sizes.gap),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    content()
                    Spacer(Modifier.height(sizes.gap))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        buttons.secondary?.let { label ->
                            PromptPill(label = label, primary = false, onClick = buttons.onSecondary, modifier = Modifier.weight(1f))
                        }
                        buttons.primary?.let { label ->
                            PromptPill(label = label, primary = true, onClick = buttons.onPrimary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Canvas(Modifier.size(sizes.art)) { art(time) }
    }
}

/** The two lines under the title. */
@Composable
fun PromptBody(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PromptPill(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A pill while the label fits on one line; with large text a long label wraps and the
    // corners stay round instead of the label being cut off.
    val shape = RoundedCornerShape(24.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .heightIn(min = 46.dp)
                .clip(shape)
                .then(
                    if (primary) {
                        Modifier.background(Brush.horizontalGradient(listOf(Amber, Coral)))
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = if (primary) ButtonInk else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

/* ------------------------------------------------------------------------------------------ */
/* The pictures. Each draws in a 100 × 100 space scaled to the canvas.                        */
/* ------------------------------------------------------------------------------------------ */

private inline fun DrawScope.inUnits(block: DrawScope.(u: Float) -> Unit) = block(size.minDimension / 100f)

/** Faint notes in the card's sky, each floating a little up and down on its own beat. */
private fun DrawScope.drawSkyNotes(time: Float, ink: Color) {
    val spots = listOf(Triple(0.12f, 0.62f, 0.0f), Triple(0.88f, 0.42f, 0.3f), Triple(0.22f, 0.3f, 0.55f), Triple(0.78f, 0.78f, 0.8f))
    spots.forEach { (x, y, phase) ->
        val wave = sin((time + phase) * 2 * PI * 2).toFloat()
        val at = Offset(size.width * x, size.height * y - wave * 3.dp.toPx())
        drawNote(at, 11.dp.toPx(), ink.copy(alpha = 0.13f + 0.05f * wave))
    }
}

/**
 * The update picture: a glossy rocket climbing to the right, the Blazify logo in its porthole,
 * and smoke and notes tumbling out of its exhaust.
 */
fun DrawScope.drawRocket(time: Float, logo: ImageBitmap) = inUnits { u ->
    val bob = sin(time * 2 * PI * 2).toFloat() * 1.1f * u
    rotate(38f, Offset(50f * u, 52f * u)) {
        translate(0f, bob - 5f * u) {
            // Smoke: a cloud of soft round puffs centred on the rocket's line, just under the nozzle,
            // widening as it falls behind; lit from the top left.
            val puffs = listOf(Triple(50f, 84f, 6.5f), Triple(44.5f, 89.5f, 6f), Triple(55.5f, 89.5f, 6f), Triple(50f, 95f, 6.5f))
            puffs.forEachIndexed { i, (x, y, r) ->
                val breathe = 1f + 0.08f * sin((time * 2 * PI * 2) + i).toFloat()
                val c = Offset(x * u, y * u)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFEDE6DF), Color(0xFFC9BEB4)),
                        center = c + Offset(-r * 0.35f * u, -r * 0.4f * u),
                        radius = r * u * 1.4f,
                    ),
                    radius = r * u * breathe,
                    center = c,
                )
            }
            // Small puffs leaving the nozzle straight down its line, spreading evenly to both sides
            // as they grow and fade.
            for (i in 0 until 6) {
                val p = (time * 3 + i / 6f) % 1f
                val side = if (i % 2 == 0) -1f else 1f
                val c = Offset((50f + side * 7f * p) * u, (80f + 24f * p) * u)
                val r = (2.4f + 4.2f * p) * u
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White, Color(0xFFE3DAD1)),
                        center = c - Offset(r * 0.3f, r * 0.35f),
                        radius = r * 1.3f,
                    ),
                    radius = r,
                    center = c,
                    alpha = (1f - p) * 0.9f,
                )
            }
            // Two notes tumbling out with the smoke, one to each side of its line.
            for (i in 0..1) {
                val p = (time * 2f + i / 2f) % 1f
                val side = if (i == 0) -1f else 1f
                val c = Offset((50f + side * (6f + 6f * p)) * u, (86f + 14f * p) * u)
                drawNote(c, 8.5f * u, Amber.copy(alpha = (1f - p) * 0.95f))
            }
            // Fins behind the body.
            val finBrush = Brush.linearGradient(listOf(Color(0xFFFF8A65), Coral, Color(0xFFBF360C)), Offset(30f * u, 55f * u), Offset(40f * u, 82f * u))
            drawPath(
                Path().apply {
                    moveTo(37f * u, 50f * u)
                    cubicTo(26f * u, 56f * u, 24f * u, 70f * u, 27f * u, 80f * u)
                    cubicTo(31f * u, 75f * u, 35f * u, 72f * u, 39f * u, 71f * u)
                    close()
                },
                finBrush,
            )
            drawPath(
                Path().apply {
                    moveTo(63f * u, 50f * u)
                    cubicTo(74f * u, 56f * u, 76f * u, 70f * u, 73f * u, 80f * u)
                    cubicTo(69f * u, 75f * u, 65f * u, 72f * u, 61f * u, 71f * u)
                    close()
                },
                Brush.linearGradient(listOf(Color(0xFFFF8A65), Coral, Color(0xFF8D2A0A)), Offset(62f * u, 55f * u), Offset(74f * u, 82f * u)),
            )
            // Nozzle.
            drawPath(
                Path().apply {
                    moveTo(43f * u, 72f * u); lineTo(57f * u, 72f * u); lineTo(55f * u, 79f * u); lineTo(45f * u, 79f * u); close()
                },
                Brush.horizontalGradient(listOf(Color(0xFF6D6259), Color(0xFFB7ACA2), Color(0xFF4E453E)), 43f * u, 57f * u),
            )
            // Body: a rounded shell, shaded like a cylinder lit from the left.
            val body =
                Path().apply {
                    moveTo(50f * u, 8f * u)
                    cubicTo(61f * u, 16f * u, 66f * u, 32f * u, 64f * u, 52f * u)
                    cubicTo(63f * u, 62f * u, 61f * u, 70f * u, 58f * u, 74f * u)
                    lineTo(42f * u, 74f * u)
                    cubicTo(39f * u, 70f * u, 37f * u, 62f * u, 36f * u, 52f * u)
                    cubicTo(34f * u, 32f * u, 39f * u, 16f * u, 50f * u, 8f * u)
                    close()
                }
            drawPath(
                body,
                Brush.horizontalGradient(
                    0f to Color(0xFFB3A79C),
                    0.3f to Color(0xFFFFFFFF),
                    0.62f to Color(0xFFF1EBE5),
                    1f to Color(0xFF94877B),
                    startX = 36f * u,
                    endX = 64f * u,
                ),
            )
            clipPath(body) {
                // Amber nose cone, with the same cylinder shading.
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color(0xFFD84315),
                        0.32f to Color(0xFFFFCC80),
                        0.6f to Amber,
                        1f to Color(0xFFBF360C),
                        startX = 36f * u,
                        endX = 64f * u,
                    ),
                    topLeft = Offset(30f * u, 0f),
                    size = Size(40f * u, 25f * u),
                )
                // A band at the waist.
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color(0xFFBF360C),
                        0.35f to Color(0xFFFFB74D),
                        1f to Color(0xFF8D2A0A),
                        startX = 36f * u,
                        endX = 64f * u,
                    ),
                    topLeft = Offset(30f * u, 63f * u),
                    size = Size(40f * u, 4f * u),
                )
                // Shine down the lit side.
                drawRect(
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0f)), 40f * u, 46f * u),
                    topLeft = Offset(40f * u, 14f * u),
                    size = Size(6f * u, 54f * u),
                )
            }
            // Porthole: a gold rim around dark glass, with a note inside.
            val port = Offset(50f * u, 41f * u)
            drawCircle(
                Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFA000), Color(0xFF8D5A00)), port - Offset(10.5f * u, 10.5f * u), port + Offset(10.5f * u, 10.5f * u)),
                radius = 11f * u,
                center = port,
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF4E342E), Color(0xFF1A0E00)), center = port - Offset(2f * u, 2f * u), radius = 10.5f * u),
                radius = 8.6f * u,
                center = port,
            )
            // The Blazify logo behind the glass, in amber.
            val logoSide = (14f * u).toInt()
            drawImage(
                logo,
                dstOffset = IntOffset((port.x - logoSide / 2f).toInt(), (port.y - logoSide / 2f).toInt()),
                dstSize = IntSize(logoSide, logoSide),
                colorFilter = ColorFilter.tint(Amber),
                filterQuality = FilterQuality.High,
            )
            drawArc(
                Color.White.copy(alpha = 0.55f),
                startAngle = 195f,
                sweepAngle = 55f,
                useCenter = false,
                topLeft = port - Offset(7.4f * u, 7.4f * u),
                size = Size(14.8f * u, 14.8f * u),
                style = Stroke(width = 1.1f * u),
            )
        }
    }
}

/**
 * The star picture: a faceted gold star, each face lit by how it turns to a light at the top
 * left, a note engraved in the middle, a glint at a tip and notes floating round it.
 */
fun DrawScope.drawStar(time: Float) = inUnits { u ->
    // A soft breath: the star grows and settles by a few percent, three seconds a beat.
    val beat = sin(time * 2 * PI * 2).toFloat()
    val grow = 1f + 0.035f * beat
    val centre = Offset(50f * u, 52f * u)
    // Small GitHub marks floating low on each side, where the star is asked for.
    listOf(Triple(10f, 66f, 0.25f), Triple(90f, 70f, 0.75f)).forEach { (x, y, phase) ->
        val wave = sin((time + phase) * 2 * PI * 2).toFloat()
        drawGitHubMark(Offset(x * u, (y - 3f * wave) * u), 10f * u, Amber.copy(alpha = 0.75f + 0.15f * wave))
    }
    // Two notes beside it, each floating a little on its own beat.
    listOf(Triple(14f, 22f, 0f), Triple(86f, 30f, 0.5f)).forEach { (x, y, phase) ->
        val wave = sin((time + phase) * 2 * PI * 2).toFloat()
        drawNote(Offset(x * u, (y - 3f * wave) * u), 10f * u, Amber.copy(alpha = 0.8f + 0.15f * wave))
    }
    scale(grow, centre) {
        val outer = 36f * u
        val inner = outer * 0.48f
        val points =
            (0 until 10).map { i ->
                val r = if (i % 2 == 0) outer else inner
                val a = -PI / 2 + i * PI / 5
                centre + Offset((r * cos(a)).toFloat(), (r * sin(a)).toFloat())
            }
        val light = -PI * 0.75 // from the top left
        for (i in 0 until 10) {
            val p1 = points[i]
            val p2 = points[(i + 1) % 10]
            val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f) - centre
            // Each face is a triangle from the centre; its brightness is how squarely it faces the light,
            // nudged by which side of its ridge it is on, which is what makes the bevel read as 3D.
            val facing = cos(kotlin.math.atan2(mid.y.toDouble(), mid.x.toDouble()) - light).toFloat()
            val side = if (i % 2 == 0) 0.18f else -0.18f
            val lit = ((facing + 1f) / 2f + side).coerceIn(0f, 1f)
            val face = lerp(Color(0xFFE65100), Color(0xFFFFF59D), lit)
            drawPath(
                Path().apply { moveTo(centre.x, centre.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() },
                face,
            )
        }
        // A thin darker rim holds the shape.
        drawPath(
            Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                close()
            },
            Color(0xFFB45309).copy(alpha = 0.55f),
            style = Stroke(width = 1.2f * u),
        )
        // The engraved note: one cut in a deeper gold, sitting in the middle of the facets.
        drawNote(centre + Offset(0.6f * u, 2f * u), 13f * u, Color(0xFF8D4A00).copy(alpha = 0.7f))
        // Glint at the top-left tip.
        val glint = 0.5f + 0.5f * sin(time * 2 * PI * 3).toFloat()
        drawSparkle(points[8] + Offset(2f * u, 2f * u), (4f + 3f * glint) * u, Color.White.copy(alpha = 0.5f + 0.5f * glint))
    }
}

/** A quaver: an oval head tilted back, a stem and a flag. [h] is its full height. */
private fun DrawScope.drawNote(at: Offset, h: Float, color: Color) {
    if (color.alpha <= 0f) return
    val headW = h * 0.46f
    val headH = h * 0.32f
    val head = Offset(at.x - h * 0.12f, at.y + h * 0.32f)
    rotate(-22f, head) {
        drawOval(color, topLeft = head - Offset(headW / 2f, headH / 2f), size = Size(headW, headH))
    }
    val stemX = head.x + headW * 0.42f
    val top = at.y - h * 0.5f
    drawRect(color, topLeft = Offset(stemX - h * 0.06f, top), size = Size(h * 0.1f, head.y - top))
    drawPath(
        Path().apply {
            moveTo(stemX, top)
            cubicTo(stemX + h * 0.12f, top + h * 0.12f, stemX + h * 0.36f, top + h * 0.2f, stemX + h * 0.26f, top + h * 0.48f)
            cubicTo(stemX + h * 0.28f, top + h * 0.3f, stemX + h * 0.14f, top + h * 0.24f, stemX, top + h * 0.22f)
            close()
        },
        color,
    )
}

/** GitHub's mark, the same outline as the app's own github icon, in a 24 × 24 box. */
private val GitHubMark: Path by lazy {
    PathParser()
        .parsePathString(
            "M12,2A10,10 0,0 0,2 12c0,4.42 2.87,8.17 6.84,9.5c0.5,0.08 0.66,-0.23 0.66,-0.5c0,-0.23 0,-0.86 0,-1.69" +
                "c-2.77,0.6 -3.36,-1.34 -3.36,-1.34c-0.46,-1.16 -1.11,-1.47 -1.11,-1.47c-0.91,-0.62 0.07,-0.6 0.07,-0.6" +
                "c1,0.07 1.53,1.03 1.53,1.03c0.87,1.52 2.34,1.07 2.91,0.83c0.09,-0.65 0.35,-1.09 0.63,-1.34" +
                "c-2.22,-0.25 -4.55,-1.11 -4.55,-4.92c0,-1.11 0.38,-2 1.03,-2.71c-0.1,-0.25 -0.45,-1.29 0.1,-2.64" +
                "c0,0 0.84,-0.27 2.75,1.02c0.79,-0.22 1.65,-0.33 2.5,-0.33c0.85,0 1.71,0.11 2.5,0.33" +
                "c1.91,-1.29 2.75,-1.02 2.75,-1.02c0.55,1.35 0.2,2.39 0.1,2.64c0.65,0.71 1.03,1.6 1.03,2.71" +
                "c0,3.82 -2.34,4.66 -4.57,4.91c0.36,0.31 0.69,0.92 0.69,1.85c0,1.34 0,2.42 0,2.74" +
                "c0,0.27 0.16,0.59 0.67,0.5C19.14,20.16 22,16.42 22,12A10,10 0,0 0,12 2Z",
        ).toPath()
}

/** The GitHub mark centred on [at], [side] across. */
private fun DrawScope.drawGitHubMark(at: Offset, side: Float, color: Color) {
    translate(at.x - side / 2f, at.y - side / 2f) {
        scale(side / 24f, side / 24f, pivot = Offset.Zero) { drawPath(GitHubMark, color) }
    }
}

/** A four-pointed sparkle. */
private fun DrawScope.drawSparkle(at: Offset, r: Float, color: Color) {
    val w = r * 0.22f
    drawPath(
        Path().apply {
            moveTo(at.x, at.y - r)
            quadraticTo(at.x + w, at.y - w, at.x + r, at.y)
            quadraticTo(at.x + w, at.y + w, at.x, at.y + r)
            quadraticTo(at.x - w, at.y + w, at.x - r, at.y)
            quadraticTo(at.x - w, at.y - w, at.x, at.y - r)
            close()
        },
        color,
    )
}
