/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.R

// Sample accounts, like the sample listeners in onboarding.
private val NAMES = listOf("Alex", "Maya")
private val HANDLES = listOf("@alexplays", "@mayamusic")

/** The account card opens the kept accounts; the finger picks the other one and it switches. */
@Composable
internal fun AccountsDemo(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val expand = painterResource(R.drawable.expand_more)
    val check = painterResource(R.drawable.check)
    val measurer = rememberTextMeasurer()
    val listTitle = stringResource(R.string.accounts_and_channels)
    DemoCanvas(loopMs = 4400, stillAt = 0.55f, playing = playing, modifier = modifier) { t, c ->
        inDesignBox { u ->
            val current = if (t > 0.50f && t < 0.97f) 1 else 0
            val pop = if (current == 1) overshoot(seg(t, 0.50f, 0.58f)) else 1f

            fun avatarColours(who: Int) = if (who == 0) c.accent to c.onAccent else c.control to c.onControl

            // The current account.
            pill(c.panel, 20f, 8f, 240f, 44f, u, radius = 12f)
            val (bg, fg) = avatarColours(current)
            avatar(measurer, NAMES[current].take(1), 42f, 30f, 14f * pop, bg, fg, u)
            text(measurer, NAMES[current], 64f, 15f, 11f, c.ink, u, bold = true)
            text(measurer, HANDLES[current], 64f, 31f, 9f, c.inkSoft, u)
            icon(expand, 244f, 30f, 18f, c.inkSoft, u)

            // The accounts kept on this phone.
            val sheet = easeOut(seg(t, 0.22f, 0.32f)) * (1f - seg(t, 0.62f, 0.70f))
            if (sheet > 0f) {
                val dy = (1f - sheet) * 12f
                pill(c.panel.fade(sheet), 20f, 58f + dy, 240f, 72f, u, radius = 12f)
                text(measurer, listTitle, 32f, 62f + dy, 8.5f, c.inkSoft.fade(sheet), u, bold = true, maxWidth = 210f)
                for (who in 0 until 2) {
                    val cy = 86f + who * 26f + dy
                    val (rowBg, rowFg) = avatarColours(who)
                    avatar(measurer, NAMES[who].take(1), 42f, cy, 10f, rowBg.fade(sheet), rowFg.fade(sheet), u)
                    text(measurer, NAMES[who], 60f, cy - 7f, 10.5f, c.ink.fade(sheet), u, bold = true)
                    if (who == current) icon(check, 244f, cy, 16f, c.accent.fade(sheet), u)
                }
            }

            val toCard = ease(seg(t, 0.06f, 0.14f))
            val toMaya = ease(seg(t, 0.34f, 0.44f))
            finger(
                c,
                cx = mix(mix(220f, 150f, toCard), 150f, toMaya),
                cy = mix(mix(120f, 30f, toCard), 112f, toMaya),
                visible = seg(t, 0.04f, 0.08f) * (1f - seg(t, 0.58f, 0.64f)),
                tap = if (t < 0.34f) seg(t, 0.16f, 0.22f) else seg(t, 0.46f, 0.52f),
                u = u,
            )
        }
    }
}
