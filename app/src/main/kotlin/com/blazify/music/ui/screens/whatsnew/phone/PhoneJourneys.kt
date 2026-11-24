/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.blazify.music.ui.screens.whatsnew.demos.DemoCanvas
import com.blazify.music.ui.screens.whatsnew.demos.centredText
import com.blazify.music.ui.screens.whatsnew.demos.ease
import com.blazify.music.ui.screens.whatsnew.demos.easeOut
import com.blazify.music.ui.screens.whatsnew.demos.fade
import com.blazify.music.ui.screens.whatsnew.demos.icon
import com.blazify.music.ui.screens.whatsnew.demos.mix
import com.blazify.music.ui.screens.whatsnew.demos.pill
import com.blazify.music.ui.screens.whatsnew.demos.seg
import com.blazify.music.ui.screens.whatsnew.demos.text
import com.blazify.music.ui.screens.whatsnew.demos.textWidth
import com.blazify.music.whatsnew.DemoId

/*
 * Each journey starts on Home, as the person will, and walks to the feature: the taps, the
 * screens they open, and the feature doing its thing. One clock runs 0 to 1 over the loop,
 * and the last moments fade back to Home so the loop starts where it ends.
 */

/** A journey: how long one loop lasts, the moment shown when animations are off, and the drawing. */
internal class Journey(
    val loopMs: Int,
    val stillAt: Float,
    val draw: DrawScope.(Ph) -> Unit,
)

private val JOURNEYS: Map<DemoId, Journey> =
    mapOf(
        DemoId.SPATIAL_SOUND to Journey(8000, 0.60f) { spatialSound(it) },
        DemoId.SEARCH_OFFLINE to Journey(7600, 0.64f) { offlineSearchJourney(it) },
        DemoId.TRENDING to Journey(7600, 0.62f) { trending(it) },
        DemoId.KEEPS_PLAYING to Journey(8400, 0.76f) { keepsPlaying(it) },
        DemoId.VIDEO_ART to Journey(7600, 0.66f) { videoArt(it) },
        DemoId.ACCOUNTS to Journey(7200, 0.55f) { accounts(it) },
        DemoId.QUEUE_SWIPE to Journey(8000, 0.60f) { queueSwipe(it) },
        DemoId.RECOGNITION to Journey(7600, 0.45f) { recognition(it) },
    )

/** True when [demo] has a journey through the phone mock-up. */
fun hasPhoneJourney(demo: DemoId): Boolean = demo in JOURNEYS

/**
 * The phone mock-up playing [demo]'s journey from Home. [playing] is false for cards that should
 * hold their still frame. Hidden from TalkBack: the card's words say what it shows.
 */
@Composable
fun PhoneDemo(
    demo: DemoId,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val journey = JOURNEYS[demo] ?: return
    val assets = rememberPhoneAssets()
    val measurer = rememberTextMeasurer(cacheSize = 48)
    val frame = remember { Path() }
    val clip = remember { Path() }
    val layer = remember { Paint() }
    DemoCanvas(journey.loopMs, journey.stillAt, playing, modifier.clearAndSetSemantics {}) { t, c ->
        inPhone(c, measurer, frame) { u ->
            journey.draw(this, Ph(c, assets, measurer, u, t, clip, layer))
        }
    }
}

/** The last moments of every loop: Home fades back in over everything. */
private fun DrawScope.backToHome(p: Ph) = faded(p, seg(p.t, 0.94f, 1f)) { home(p) }

/** Home, then the player growing out of the mini player after the finger taps it. */
private fun DrawScope.homeThenPlayer(
    p: Ph,
    open: Float,
    video: Float = 0f,
) {
    home(p)
    fromMiniPlayer(p, open) { player(p, video) }
}

/** 3D sound: player › menu › 8D, and the note circles the cover. */
private fun DrawScope.spatialSound(p: Ph) {
    val t = p.t
    homeThenPlayer(p, ease(seg(t, 0.18f, 0.30f)))
    orbitAroundCover(p, seg(t, 0.70f, 0.78f) * (1f - seg(t, 0.92f, 0.96f)))
    val menu = easeOut(seg(t, 0.42f, 0.50f)) * (1f - ease(seg(t, 0.64f, 0.72f)))
    sheet(p, menu, 128f) { top -> playerMenu(p, top, if (t > 0.60f) 2 else 0) }

    val toMini = ease(seg(t, 0.06f, 0.14f))
    val toMore = ease(seg(t, 0.32f, 0.38f))
    val toMode = ease(seg(t, 0.52f, 0.58f))
    tapAt(
        p,
        x = mix(mix(mix(120f, 60f, toMini), PLAYER_MORE_X, toMore), spatialModeX(2), toMode),
        y = mix(mix(mix(230f, 272.4f, toMini), PLAYER_ACTIONS_Y, toMore), 192f + 87f, toMode),
        visible = seg(t, 0.04f, 0.07f) * (1f - seg(t, 0.66f, 0.70f)),
        tap =
            when {
                t < 0.30f -> seg(t, 0.14f, 0.18f)
                t < 0.50f -> seg(t, 0.38f, 0.42f)
                else -> seg(t, 0.58f, 0.62f)
            },
    )
    backToHome(p)
}

/** Search with no connection: Home's search bar › type › the saved songs come up. */
private fun DrawScope.offlineSearchJourney(p: Ph) {
    val t = p.t
    val open = ease(seg(t, 0.20f, 0.30f))
    translate(left = -open * 40f * p.u) { home(p) }
    if (open > 0f) {
        translate(left = (1f - open) * SCREEN_W * p.u) {
            val typed = "mira".take((seg(t, 0.34f, 0.48f) * 4f).toInt())
            val caret = t < 0.50f || (t * 7600f / 450f).toInt() % 2 == 0
            offlineSearch(p, typed, caret, seg(t, 0.50f, 0.68f))
        }
    }
    val k = ease(seg(t, 0.06f, 0.15f))
    tapAt(p, mix(110f, 60f, k), mix(230f, 121f, k), seg(t, 0.04f, 0.07f) * (1f - seg(t, 0.24f, 0.28f)), seg(t, 0.16f, 0.20f))
    backToHome(p)
}

/** Trending: scroll Home down to the shelf › its arrow › the country's chart. */
private fun DrawScope.trending(p: Ph) {
    val t = p.t
    val scroll = ease(seg(t, 0.10f, 0.28f)) * TRENDING_SCROLL
    val open = ease(seg(t, 0.44f, 0.56f))
    translate(left = -open * 40f * p.u) { home(p, scroll) }
    if (open > 0f) translate(left = (1f - open) * SCREEN_W * p.u) { trendingChart(p) }

    val drag = ease(seg(t, 0.10f, 0.28f))
    val toArrow = ease(seg(t, 0.32f, 0.38f))
    tapAt(
        p,
        x = mix(75f, 140f, toArrow),
        y = mix(mix(232f, 232f - TRENDING_SCROLL, drag), TRENDING_ARROW_Y - TRENDING_SCROLL, toArrow),
        visible = seg(t, 0.05f, 0.08f) * (1f - seg(t, 0.48f, 0.52f)),
        tap = if (t < 0.30f) 0.35f * (if (t in 0.09f..0.29f) 1f else 0f) else seg(t, 0.40f, 0.44f),
    )
    backToHome(p)
}

/** Music keeps playing: player › queue › the album ends and similar songs carry on. */
private fun DrawScope.keepsPlaying(p: Ph) {
    val t = p.t
    homeThenPlayer(p, ease(seg(t, 0.18f, 0.30f)))
    val queue = easeOut(seg(t, 0.42f, 0.50f))
    sheet(p, queue, 170f) { top ->
        text(p.m, p.a.queueLabel, 12f, top + 12f, 8.5f, p.c.ink, p.u, bold = true)
        val carriedOn = t > 0.72f
        queueRow(p, 0, top + 30f, current = false, alpha = 0.6f)
        queueRow(p, 1, top + 54f, current = !carriedOn)
        val progress = if (carriedOn) 1f else mix(0.6f, 1f, seg(t, 0.50f, 0.70f))
        pill(p.c.line, 33f, top + 74f, 104f, 1.6f, p.u)
        pill(p.c.accent, 33f, top + 74f, 104f * progress, 1.6f, p.u)
        text(p.m, p.a.similar, 12f, top + 82f, 6.5f, p.c.accent.fade(seg(t, 0.60f, 0.64f)), p.u, bold = true)
        for (i in 0 until 2) {
            val shown = easeOut(seg(t, 0.62f + i * 0.05f, 0.72f + i * 0.05f))
            queueRow(p, if (i == 0) 2 else 5, top + 98f + i * 24f + (1f - shown) * 8f, current = i == 0 && carriedOn, alpha = shown)
        }
    }
    val toMini = ease(seg(t, 0.06f, 0.14f))
    val toQueue = ease(seg(t, 0.32f, 0.38f))
    tapAt(
        p,
        x = mix(mix(120f, 60f, toMini), PLAYER_ROW_X[0], toQueue),
        y = mix(mix(230f, 272.4f, toMini), PLAYER_ROW_Y, toQueue),
        visible = seg(t, 0.04f, 0.07f) * (1f - seg(t, 0.46f, 0.50f)),
        tap = if (t < 0.30f) seg(t, 0.14f, 0.18f) else seg(t, 0.38f, 0.42f),
    )
    backToHome(p)
}

/** Video Art finds the real video: player › the song's own video fades in behind it. */
private fun DrawScope.videoArt(p: Ph) {
    val t = p.t
    val video = ease(seg(t, 0.40f, 0.54f))
    homeThenPlayer(p, ease(seg(t, 0.18f, 0.30f)), video)
    if (video > 0f && t < 0.94f) {
        val width = textWidth(p.m, p.a.videoArt, 6.5f, p.u, bold = true) + 14f
        pill(p.c.accent.fade(video), 75f - width / 2f, 36f, width, 11f, p.u)
        centredText(p.m, p.a.videoArt, 75f, 41.5f, 6f, p.c.onAccent.fade(video), p.u)
    }
    val k = ease(seg(t, 0.06f, 0.14f))
    tapAt(p, mix(120f, 60f, k), mix(230f, 272.4f, k), seg(t, 0.04f, 0.07f) * (1f - seg(t, 0.22f, 0.26f)), seg(t, 0.14f, 0.18f))
    backToHome(p)
}

/** Accounts: the account button › the kept accounts › switch to the other one. */
private fun DrawScope.accounts(p: Ph) {
    val t = p.t
    val current = if (t > 0.56f && t < 0.96f) 1 else 0
    home(p, account = if (t > 0.70f) current else 0)
    val open = easeOut(seg(t, 0.22f, 0.32f)) * (1f - ease(seg(t, 0.66f, 0.74f)))
    sheet(p, open, 150f) { top -> accountSheet(p, top, current) }

    val toButton = ease(seg(t, 0.06f, 0.14f))
    val toMaya = ease(seg(t, 0.40f, 0.48f))
    tapAt(
        p,
        x = mix(mix(110f, 14.4f, toButton), 75f, toMaya),
        y = mix(mix(230f, 27.7f, toButton), SCREEN_H - 150f + 120f, toMaya),
        visible = seg(t, 0.04f, 0.07f) * (1f - seg(t, 0.60f, 0.64f)),
        tap = if (t < 0.40f) seg(t, 0.16f, 0.20f) else seg(t, 0.50f, 0.56f),
    )
    backToHome(p)
}

/** The queue: player › queue › swipe a song right and it moves up to play next. */
private fun DrawScope.queueSwipe(p: Ph) {
    val t = p.t
    homeThenPlayer(p, ease(seg(t, 0.18f, 0.30f)))
    val queue = easeOut(seg(t, 0.38f, 0.46f))
    val swipe = ease(seg(t, 0.50f, 0.62f)) * (1f - ease(seg(t, 0.66f, 0.72f)))
    val moved = ease(seg(t, 0.72f, 0.82f))
    sheet(p, queue, 170f) { top ->
        text(p.m, p.a.queueLabel, 12f, top + 12f, 8.5f, p.c.ink, p.u, bold = true)
        queueRow(p, 0, top + 30f, current = true)
        queueRow(p, 3, top + mix(54f, 78f, moved), current = false)
        queueRow(p, 5, top + 102f, current = false)
        val swipedY = top + mix(78f, 54f, moved)
        if (swipe > 0.01f) {
            pill(p.c.accent, 6f, swipedY - 2f, 138f, 22f, p.u, radius = 0f)
            icon(p.a.playNext, 16f, swipedY + 9f, 10f, p.c.onAccent, p.u)
            text(p.m, p.a.playNextLabel, 25f, swipedY + 5f, 6.5f, p.c.onAccent, p.u, bold = true, maxWidth = 70f)
        }
        queueRow(p, 4, swipedY, current = false, dx = swipe * 80f)
        if (t > 0.72f && t < 0.94f) pill(p.c.accent, 6f, swipedY + 2f, 2f, 14f, p.u)
    }
    val toMini = ease(seg(t, 0.06f, 0.14f))
    val toQueue = ease(seg(t, 0.30f, 0.36f))
    val toRow = ease(seg(t, 0.44f, 0.50f))
    val rowY = SCREEN_H - 170f + 78f + 8f
    tapAt(
        p,
        x = mix(mix(mix(120f, 60f, toMini), PLAYER_ROW_X[0], toQueue), 40f, toRow) + swipe * 80f,
        y = mix(mix(mix(230f, 272.4f, toMini), PLAYER_ROW_Y, toQueue), rowY, toRow),
        visible = seg(t, 0.04f, 0.07f) * (1f - seg(t, 0.64f, 0.68f)),
        tap =
            when {
                t < 0.30f -> seg(t, 0.14f, 0.18f)
                t < 0.44f -> seg(t, 0.36f, 0.40f)
                else -> seg(t, 0.50f, 0.54f) * 0.4f
            },
    )
    backToHome(p)
}

/** Song recognition: the sound wave in Home's search bar › listening › the song it found. */
private fun DrawScope.recognition(p: Ph) {
    val t = p.t
    home(p)
    faded(p, seg(t, 0.22f, 0.30f)) { listening(p, easeOut(seg(t, 0.58f, 0.70f))) }
    val k = ease(seg(t, 0.06f, 0.14f))
    tapAt(p, mix(100f, 132f, k), mix(230f, 121f, k), seg(t, 0.04f, 0.07f) * (1f - seg(t, 0.24f, 0.28f)), seg(t, 0.16f, 0.20f))
    backToHome(p)
}
