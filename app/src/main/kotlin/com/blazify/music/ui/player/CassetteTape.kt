/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * CASSETTE design centrepiece: a 3D retro compact cassette (dark shell, cream
 * label, accent stripe, "A" side badge, "60" length mark) with a window showing
 * the two tape reels. The album art is printed faintly across the label (under
 * the printed texture) with a small full-opacity art preview beside the "A"
 * badge. The reels spin while a song plays and the tape visibly transfers from
 * the left spool to the right as playback progresses; on pause everything
 * freezes. Compose Canvas + coil — shared by the player and the design-preview.
 */

package com.blazify.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

/* ---- retro palette (matches the reference; deliberately static) ---- */
private val ShellTop = Color(0xFF5A4B3D)
private val ShellMid = Color(0xFF3B3128)
private val ShellBottom = Color(0xFF262019)
private val LabelCream = Color(0xFFF2E7D0)
private val LabelCreamDark = Color(0xFFE4D6BC)
private val LabelBorder = Color(0xFFC9B999)
private val RetroOrange = Color(0xFFE0812A)
private val WindowDark = Color(0xFF241D15)
private val SpoolDark = Color(0xFF14100B)
private val HubCream = Color(0xFFEFE6D2)
private val InkBrown = Color(0xFF3A2F24)

@Composable
fun CassetteTape(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    // Dynamic accent (album colour) for the label stripe; retro neutrals stay fixed.
    accent: Color = RetroOrange,
    // Album art: printed faintly across the label + a small preview beside "A".
    thumbnailUrl: String? = null,
) {
    // Reels spin while playing; freeze in place on pause.
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
            )
        }
    }
    // Smooth tape transfer between spools.
    val tape by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "tapeTransfer",
    )

    BoxWithConstraints(modifier = modifier.aspectRatio(1.55f)) {
        val bw = maxWidth
        val bh = maxHeight

        // 1) Base: shadows, shell, cream label.
        Canvas(modifier = Modifier.fillMaxSize()) { drawCassetteBase() }

        // 2) Album art printed across the label — low opacity, under the texture.
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = bw * 0.075f, y = bh * 0.075f)
                    .size(width = bw * 0.83f, height = bh * 0.545f)
                    .clip(RoundedCornerShape(bw * 0.02f))
                    .alpha(0.22f),
            )
        }

        // 3) Printed texture + window + reels on top of the art.
        Canvas(modifier = Modifier.fillMaxSize()) { drawCassetteDetails(tape, rotation.value, accent) }

        // 4) Small full-opacity art preview, left of the "A" badge.
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = bw * 0.10f, y = bh * 0.10f)
                    .size(width = bw * 0.055f, height = bh * 0.085f)
                    .clip(RoundedCornerShape(bw * 0.008f))
                    .border(1.dp, InkBrown.copy(alpha = 0.6f), RoundedCornerShape(bw * 0.008f)),
            )
        }

        // Side badge "A" (inside the drawn badge box, right of the art preview).
        Text(
            text = "A",
            color = InkBrown,
            fontWeight = FontWeight.Black,
            fontSize = (bw.value * 0.045f).sp,
            modifier = Modifier.offset(x = bw * 0.182f, y = bh * 0.095f),
        )
        // Length mark "60" on the accent stripe.
        Text(
            text = "60",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (bw.value * 0.052f).sp,
            modifier = Modifier.offset(x = bw * 0.82f, y = bh * 0.465f),
        )
    }
}

/** Pass 1 — shadows, shell body and the cream label backing. */
private fun DrawScope.drawCassetteBase() {
    val s = size.width
    val t = size.height

    /* --- layered drop shadow (soft, wide → tight, dark = 3D lift) --- */
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(s * 0.05f, t * 0.10f),
        size = Size(s * 0.97f, t * 0.93f),
        cornerRadius = CornerRadius(s * 0.06f, s * 0.06f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(s * 0.035f, t * 0.075f),
        size = Size(s * 0.965f, t * 0.925f),
        cornerRadius = CornerRadius(s * 0.055f, s * 0.055f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.24f),
        topLeft = Offset(s * 0.022f, t * 0.048f),
        size = Size(s * 0.96f, t * 0.93f),
        cornerRadius = CornerRadius(s * 0.05f, s * 0.05f),
    )

    /* --- shell --- */
    val shellTL = Offset(s * 0.005f, 0f)
    val shellSize = Size(s * 0.97f, t * 0.955f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(ShellTop, ShellMid, ShellBottom)),
        topLeft = shellTL,
        size = shellSize,
        cornerRadius = CornerRadius(s * 0.045f, s * 0.045f),
    )
    // Double-wall bevel: inner dark line + outer hairline light.
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(shellTL.x + s * 0.012f, shellTL.y + t * 0.02f),
        size = Size(shellSize.width - s * 0.024f, shellSize.height - t * 0.04f),
        cornerRadius = CornerRadius(s * 0.035f, s * 0.035f),
        style = Stroke(width = s * 0.003f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = shellTL,
        size = shellSize,
        cornerRadius = CornerRadius(s * 0.045f, s * 0.045f),
        style = Stroke(width = s * 0.0025f),
    )
    // Top catch-light + bottom inner shade.
    drawLine(
        Color.White.copy(alpha = 0.18f),
        Offset(s * 0.06f, t * 0.018f),
        Offset(s * 0.92f, t * 0.018f),
        strokeWidth = t * 0.012f,
        cap = StrokeCap.Round,
    )
    drawLine(
        Color.Black.copy(alpha = 0.30f),
        Offset(s * 0.06f, t * 0.935f),
        Offset(s * 0.92f, t * 0.935f),
        strokeWidth = t * 0.014f,
        cap = StrokeCap.Round,
    )

    // Corner screws.
    for ((sx, sy) in listOf(0.045f to 0.07f, 0.935f to 0.07f, 0.045f to 0.87f, 0.935f to 0.87f)) {
        val c = Offset(s * sx, t * sy)
        drawCircle(Color(0xFF1C1712), radius = s * 0.016f, center = c)
        drawCircle(Color.White.copy(alpha = 0.20f), radius = s * 0.016f, center = c, style = Stroke(width = s * 0.003f))
        drawLine(Color(0xFF54463A), c - Offset(s * 0.008f, 0f), c + Offset(s * 0.008f, 0f), strokeWidth = s * 0.004f)
    }

    /* --- cream label backing --- */
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(LabelCream, LabelCreamDark)),
        topLeft = Offset(s * 0.075f, t * 0.075f),
        size = Size(s * 0.83f, t * 0.545f),
        cornerRadius = CornerRadius(s * 0.02f, s * 0.02f),
    )
}

/** Pass 2 — printed texture, window, reels and gloss, drawn over the label art. */
private fun DrawScope.drawCassetteDetails(progress: Float, rotationDeg: Float, accent: Color) {
    val s = size.width
    val t = size.height

    // Label frame (over the art edge).
    drawRoundRect(
        color = LabelBorder,
        topLeft = Offset(s * 0.075f, t * 0.075f),
        size = Size(s * 0.83f, t * 0.545f),
        cornerRadius = CornerRadius(s * 0.02f, s * 0.02f),
        style = Stroke(width = s * 0.004f),
    )

    // "A" badge box (letter drawn as a Text overlay; sits right of the art preview).
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(s * 0.17f, t * 0.10f),
        size = Size(s * 0.055f, t * 0.085f),
        cornerRadius = CornerRadius(s * 0.008f, s * 0.008f),
        style = Stroke(width = s * 0.0045f),
    )

    // Accent stripe (dynamic colour) with print texture + a thinner echo line.
    drawRect(
        color = accent,
        topLeft = Offset(s * 0.075f, t * 0.455f),
        size = Size(s * 0.83f, t * 0.105f),
    )
    for (i in 1..3) {
        val ly = t * (0.455f + 0.105f * i / 4f)
        drawLine(
            Color.Black.copy(alpha = 0.10f),
            Offset(s * 0.075f, ly),
            Offset(s * 0.905f, ly),
            strokeWidth = t * 0.006f,
        )
    }
    drawRect(
        color = accent.copy(alpha = 0.55f),
        topLeft = Offset(s * 0.075f, t * 0.575f),
        size = Size(s * 0.83f, t * 0.022f),
    )

    /* --- reel window --- */
    val winTL = Offset(s * 0.285f, t * 0.155f)
    val winSize = Size(s * 0.43f, t * 0.27f)
    val winRadius = s * 0.02f
    drawRoundRect(
        color = WindowDark,
        topLeft = winTL,
        size = winSize,
        cornerRadius = CornerRadius(winRadius, winRadius),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = winTL,
        size = winSize,
        cornerRadius = CornerRadius(winRadius, winRadius),
        style = Stroke(width = s * 0.008f),
    )

    // Reels + tape, clipped inside the window.
    val windowPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = winTL.x,
                top = winTL.y,
                right = winTL.x + winSize.width,
                bottom = winTL.y + winSize.height,
                cornerRadius = CornerRadius(winRadius, winRadius),
            ),
        )
    }
    val reelY = t * 0.29f
    val leftC = Offset(s * 0.385f, reelY)
    val rightC = Offset(s * 0.615f, reelY)
    val minSpool = s * 0.055f
    val maxSpool = s * 0.115f
    val leftSpool = minSpool + (maxSpool - minSpool) * (1f - progress)
    val rightSpool = minSpool + (maxSpool - minSpool) * progress

    clipPath(windowPath) {
        // Tape bridge along the window bottom (connects the two spools).
        drawLine(
            SpoolDark,
            Offset(leftC.x, winTL.y + winSize.height - t * 0.02f),
            Offset(rightC.x, winTL.y + winSize.height - t * 0.02f),
            strokeWidth = t * 0.028f,
        )
        // Tape sheen (thin light line along the moving tape).
        drawLine(
            Color.White.copy(alpha = 0.10f),
            Offset(leftC.x, winTL.y + winSize.height - t * 0.026f),
            Offset(rightC.x, winTL.y + winSize.height - t * 0.026f),
            strokeWidth = t * 0.006f,
        )

        for ((center, spool) in listOf(leftC to leftSpool, rightC to rightSpool)) {
            // Wound tape spool (radius follows playback progress).
            drawCircle(SpoolDark, radius = spool, center = center)
            drawCircle(Color.White.copy(alpha = 0.05f), radius = spool * 0.85f, center = center, style = Stroke(width = 1.2f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = spool * 0.65f, center = center, style = Stroke(width = 1.2f))

            // Rotating cream hub with teeth.
            rotate(degrees = rotationDeg, pivot = center) {
                drawCircle(HubCream, radius = s * 0.042f, center = center, style = Stroke(width = s * 0.013f))
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                    drawLine(
                        HubCream,
                        center + Offset(dir.x * s * 0.016f, dir.y * s * 0.016f),
                        center + Offset(dir.x * s * 0.040f, dir.y * s * 0.040f),
                        strokeWidth = s * 0.011f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            drawCircle(WindowDark, radius = s * 0.012f, center = center)
        }

        // Glass gloss across the window (diagonal light band).
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.09f), Color.Transparent, Color.Transparent),
                start = Offset(winTL.x, winTL.y),
                end = Offset(winTL.x + winSize.width * 0.8f, winTL.y + winSize.height),
            ),
            topLeft = winTL,
            size = winSize,
            cornerRadius = CornerRadius(winRadius, winRadius),
        )
    }

    /* --- bottom trapezoid with capstan holes --- */
    val trap = Path().apply {
        moveTo(s * 0.30f, t * 0.945f)
        lineTo(s * 0.35f, t * 0.755f)
        lineTo(s * 0.65f, t * 0.755f)
        lineTo(s * 0.70f, t * 0.945f)
    }
    drawPath(trap, Color(0xFF6B5B49).copy(alpha = 0.65f), style = Stroke(width = s * 0.006f))
    for (hx in listOf(0.415f, 0.585f)) {
        val c = Offset(s * hx, t * 0.85f)
        drawCircle(Color(0xFF17120D), radius = s * 0.016f, center = c)
        drawCircle(Color.White.copy(alpha = 0.18f), radius = s * 0.016f, center = c, style = Stroke(width = s * 0.003f))
    }
    for (hx in listOf(0.375f, 0.625f)) {
        drawCircle(Color(0xFF17120D), radius = s * 0.009f, center = Offset(s * hx, t * 0.79f))
    }

    /* --- soft diagonal sheen over the whole shell (plastic gloss) --- */
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(s * 0.9f, t),
        ),
        topLeft = Offset(s * 0.005f, 0f),
        size = Size(s * 0.97f, t * 0.955f),
        cornerRadius = CornerRadius(s * 0.045f, s * 0.045f),
    )
}
