/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource

private val BAR_WIDTHS = floatArrayOf(120f, 90f, 60f)

/**
 * The finger taps the feature's own icon, which lights up, and a few lines appear under it.
 * A stand-in for features that do not have a demo of their own yet.
 */
@Composable
internal fun TapIconDemo(
    @DrawableRes iconRes: Int,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(iconRes)
    DemoCanvas(loopMs = 3600, stillAt = 0.60f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val on = seg(t, 0.30f, 0.36f) * (1f - seg(t, 0.90f, 0.95f))
            val bounce = 1f + 0.08f * press(seg(t, 0.30f, 0.40f))
            drawCircle(lerp(c.control, c.accent, on), 30f * u * bounce, Offset(140f * u, 54f * u))
            icon(painter, 140f, 54f, 28f, lerp(c.onControl, c.onAccent, on), u)
            for (i in 0 until 3) {
                val shown = easeOut(seg(t, 0.40f + i * 0.06f, 0.52f + i * 0.06f)) * (1f - seg(t, 0.90f, 0.95f))
                val width = BAR_WIDTHS[i]
                pill(c.line.fade(shown), 140f - width / 2f, 98f + i * 11f + (1f - shown) * 6f, width, 6f, u)
            }
            val k = ease(seg(t, 0.06f, 0.20f))
            finger(
                c,
                cx = mix(222f, 140f, k),
                cy = mix(124f, 54f, k),
                visible = seg(t, 0.04f, 0.08f) * (1f - seg(t, 0.62f, 0.68f)),
                tap = seg(t, 0.26f, 0.33f),
                u = u,
            )
        }
    }
}
