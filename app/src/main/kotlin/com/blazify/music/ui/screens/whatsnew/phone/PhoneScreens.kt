/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.phone

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.blazify.music.ui.screens.whatsnew.demos.SAMPLE_SONGS
import com.blazify.music.ui.screens.whatsnew.demos.SampleSong
import com.blazify.music.ui.screens.whatsnew.demos.avatar
import com.blazify.music.ui.screens.whatsnew.demos.centredText
import com.blazify.music.ui.screens.whatsnew.demos.cover
import com.blazify.music.ui.screens.whatsnew.demos.easeOut
import com.blazify.music.ui.screens.whatsnew.demos.fade
import com.blazify.music.ui.screens.whatsnew.demos.icon
import com.blazify.music.ui.screens.whatsnew.demos.pill
import com.blazify.music.ui.screens.whatsnew.demos.playingBars
import com.blazify.music.ui.screens.whatsnew.demos.seg
import com.blazify.music.ui.screens.whatsnew.demos.songLines
import com.blazify.music.ui.screens.whatsnew.demos.text
import com.blazify.music.ui.screens.whatsnew.demos.textWidth
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Blazify's screens as the mock-up draws them, laid out like the real ones: Home with its
 * header, greeting card and search bar, the mini player and navigation bar, the player, and
 * the screens and sheets the journeys open.
 */

private val VideoDark = Color(0xFF0E0B14)

// Songs the offline search finds, all by the artist that was typed.
internal val OFFLINE_RESULTS =
    listOf(
        SampleSong("Lantern Season", "Mira Vale"),
        SampleSong("Glass Harbour", "Mira Vale"),
        SampleSong("Slow Orbit", "Mira Vale"),
    )

internal val ACCOUNT_NAMES = listOf("Alex", "Maya")
internal val ACCOUNT_HANDLES = listOf("@alexplays", "@mayamusic")

/** Where the Trending shelf's arrow sits on Home, before scrolling. */
internal const val TRENDING_ARROW_Y = 276.5f

/** How far Home scrolls to bring the Trending shelf above the mini player. */
internal const val TRENDING_SCROLL = 120f

// Sample mood chips, as YouTube Music lists them under Home's search bar.
private val MOODS = listOf("Energize", "Relax", "Feel good", "Commute", "Focus")

private const val PLAYLIST = "Mira Vale Mix"

/**
 * Home as Blazify draws it: header, greeting card with the hero picture, search bar, mood chips,
 * Quick picks, and further down the Trending shelf; scrolled up by [scroll].
 */
internal fun DrawScope.home(
    p: Ph,
    scroll: Float = 0f,
    account: Int = 0,
) = with(p) {
    drawRect(c.panel)
    translate(top = -scroll * u) {
        // Header: account | logo + Blazify | settings, the two icons in the theme's colour.
        if (account == 0) {
            icon(a.person, 14.4f, 27.7f, 10f, c.accent, u)
        } else {
            avatar(m, ACCOUNT_NAMES[account].take(1), 14.4f, 27.7f, 6.5f, c.control, c.onControl, u)
        }
        image(a.logo, 49f, 21.4f, 12.6f, 12.6f, u)
        text(m, "Blazify", 64f, 20.6f, 10.5f, c.ink, u, bold = true)
        icon(a.settings, 135.3f, 27.7f, 10f, c.accent, u)

        // The greeting card in the song's colour, its words left, the hero spilling over its top.
        val cardEnd = lerp(c.accent, Color.Black, 0.3f)
        drawRoundRect(
            Brush.linearGradient(listOf(c.accent, cardEnd), Offset(5.6f * u, 45f * u), Offset(144f * u, 102f * u)),
            Offset(5.6f * u, 45f * u),
            Size(138.4f * u, 57f * u),
            CornerRadius(7.5f * u),
        )
        val onCard = if (c.accent.luminance() > 0.6f) Color.Black else Color.White
        text(m, a.greeting, 13f, 49.5f, 9f, onCard, u, bold = true, maxWidth = 62f, lines = 2)
        text(m, a.userName, 13f, 71.5f, 7.5f, onCard, u, bold = true, maxWidth = 60f)
        pill(Color.Black.fade(0.28f), 13f, 85f, 35f, 11.5f, u, radius = 4f)
        cover(c, 4, null, 14.6f, 86.6f, 8.3f, 2f, u)
        icon(a.play, 18.75f, 90.75f, 4.6f, Color.White, u)
        text(m, a.forYou, 25.6f, 87f, 5.6f, onCard, u, bold = true, maxWidth = 21f)
        cover(c, 5, null, 51f, 85f, 11.5f, 3f, u)
        clipRect(0f, 0f, SCREEN_W * u, 102f * u) { image(a.hero, 72f, 18f, 66f, 84f, u) }

        // The search bar, with the recognition button at its end.
        pill(c.raised, 5.6f, 111f, 138.4f, 20f, u)
        icon(a.search, 17f, 121f, 8f, c.ink.fade(0.85f), u)
        text(m, a.homeHint, 26f, 117.8f, 5.6f, c.inkSoft, u, maxWidth = 92f)
        icon(a.wave, 132f, 121f, 8f, c.inkSoft, u)

        // Mood chips, running off the edge.
        var chipX = 4.4f
        MOODS.forEach { mood ->
            val width = textWidth(m, mood, 5f, u) + 10f
            pill(c.raised, chipX, 141f, width, 11f, u)
            centredText(m, mood, chipX + width / 2f, 146.5f, 5f, c.ink, u, bold = false)
            chipX += width + 3.5f
        }

        // Quick picks: the title in the theme's colour, Play all, and a column of songs with the
        // next column peeking in from the right.
        text(m, a.quickPicks, 4.7f, 160.3f, 7.5f, c.accent, u, bold = true, maxWidth = 100f)
        val playAll = textWidth(m, a.playAll, 4.5f, u) + 9f
        drawRoundRect(
            c.inkSoft.fade(0.7f),
            Offset((145.3f - playAll) * u, 160.6f * u),
            Size(playAll * u, 8.4f * u),
            CornerRadius(4.2f * u),
            style = Stroke(0.6f * u),
        )
        centredText(m, a.playAll, 145.3f - playAll / 2f, 164.8f, 4.5f, c.ink, u, bold = false)
        for (i in 0 until 4) {
            val y = 176f + i * 23f
            cover(c, i, a.note, 5.3f, y, 17f, 2f, u)
            songLines(m, c, SAMPLE_SONGS[i], 27.2f, y + 2f, u, titleSize = 5.6f, maxWidth = 90f)
            icon(a.more, 123.4f, y + 8.5f, 7f, c.ink, u)
            cover(c, i + 2, a.note, 140f, y, 17f, 2f, u)
        }

        // Trending, further down.
        text(m, a.trending, 4.7f, 272f, 7.5f, c.accent, u, bold = true, maxWidth = 100f)
        icon(a.arrow, 140f, TRENDING_ARROW_Y, 7f, c.ink, u)
        for (i in 0 until 3) {
            val y = 288f + i * 23f
            centredText(m, "${i + 1}", 9f, y + 8.5f, 6.5f, c.accent, u)
            cover(c, i + 3, a.note, 15.5f, y, 17f, 2f, u)
            songLines(m, c, SAMPLE_SONGS[i + 3], 37.5f, y + 2f, u, titleSize = 5.6f, maxWidth = 90f)
        }
    }
    // The shuffle button floats over the list.
    drawRoundRect(lerp(c.accent, c.panel, 0.55f), Offset(124f * u, 235.4f * u), Size(19.7f * u, 19.4f * u), CornerRadius(6f * u))
    icon(a.shuffle, 133.85f, 245.1f, 8f, c.ink, u)
    miniPlayer(p)
    navBar(p)
}

/** The mini player: a pill in the song's colour, the round cover ringed by the song's progress. */
internal fun DrawScope.miniPlayer(p: Ph) =
    with(p) {
        val end = lerp(c.accent, Color.Black, 0.62f)
        drawRoundRect(
            Brush.horizontalGradient(listOf(c.accent, end), MINI_LEFT * u, (MINI_LEFT + MINI_W) * u),
            Offset(MINI_LEFT * u, MINI_TOP * u),
            Size(MINI_W * u, MINI_H * u),
            CornerRadius(MINI_H / 2f * u),
        )
        val onMini = if (c.accent.luminance() > 0.6f) Color.Black else Color.White
        val cy = MINI_TOP + MINI_H / 2f
        cover(c, 0, null, 7.6f, cy - 9f, 18f, 9f, u)
        drawCircle(Color.Black.fade(0.25f), 9f * u, Offset(16.6f * u, cy * u))
        drawArc(
            onMini.fade(0.9f),
            startAngle = -90f,
            sweepAngle = 120f + 40f * t,
            useCenter = false,
            topLeft = Offset(6.3f * u, (cy - 10.3f) * u),
            size = Size(20.6f * u, 20.6f * u),
            style = Stroke(1.2f * u, cap = StrokeCap.Round),
        )
        icon(a.play, 16.6f, cy, 6f, onMini, u)
        text(m, SAMPLE_SONGS[0].title, 31f, MINI_TOP + 3.4f, 5.4f, onMini, u, bold = true, maxWidth = 60f)
        text(m, SAMPLE_SONGS[0].artist, 31f, MINI_TOP + 12f, 4.6f, onMini.fade(0.75f), u, maxWidth = 60f)
        listOf(a.radio, a.add, a.heart).forEachIndexed { i, button ->
            val cx = 100f + i * 17.5f
            drawCircle(onMini.fade(0.35f), 7f * u, Offset(cx * u, cy * u), style = Stroke(0.6f * u))
            icon(button, cx, cy, 6f, onMini, u)
        }
    }

/** Home, Search, Yours and Library, Home chosen. */
internal fun DrawScope.navBar(p: Ph) =
    with(p) {
        drawRect(c.panel, Offset(0f, 287f * u), Size(SCREEN_W * u, 33f * u))
        val tabs = listOf(a.home to a.navHome, a.search to a.navSearch, a.grid to a.navYours, a.library to a.navLibrary)
        tabs.forEachIndexed { i, (painter, label) ->
            val cx = 17.8f + i * 38.13f
            val active = i == 0
            if (active) pill(c.control, cx - 10f, 291.7f, 20f, 10.8f, u)
            icon(painter, cx, 297.1f, 7.5f, if (active) c.onControl else c.inkSoft, u)
            centredText(m, label, cx, 306.8f, 4.8f, if (active) c.ink else c.inkSoft, u, bold = active)
        }
    }

/** Centres of the player's buttons, for the finger. */
internal const val PLAYER_MORE_X = 131f
internal const val PLAYER_ACTIONS_Y = 201.5f
internal const val PLAYER_ROW_Y = 293.9f
internal val PLAYER_ROW_X = floatArrayOf(23.75f, 57.8f, 91.9f, 126.25f)

/**
 * The Classic player as Blazify draws it: Now Playing over the playlist, the cover, the song
 * with like, colours and more, the seek bar, the controls, and Queue, Cast, Sleep timer and
 * Lyrics along the bottom. [video] fades the song's video in behind it, as Video Art does.
 */
internal fun DrawScope.player(
    p: Ph,
    video: Float = 0f,
) = with(p) {
    drawRect(c.panel)
    drawRect(Brush.verticalGradient(listOf(lerp(c.accent, c.panel, 0.45f), c.panel), 0f, 300f * u))
    if (video > 0f) videoBackdrop(p, video)
    val words = lerp(c.ink, Color.White, video)
    val soft = lerp(c.inkSoft, Color.White.fade(0.8f), video)

    centredText(m, a.nowPlaying, 75f, 19.4f, 5.8f, words, u)
    centredText(m, PLAYLIST, 75f, 27.7f, 5f, soft, u, bold = false)
    cover(c, 0, a.note, 11.6f, 51.4f, 126.8f, 12.5f, u, alpha = 1f - video)

    text(m, SAMPLE_SONGS[0].title, 11.6f, 192.6f, 7.5f, words, u, bold = true, maxWidth = 72f)
    text(m, SAMPLE_SONGS[0].artist, 11.6f, 202.6f, 5.6f, soft, u, maxWidth = 72f)
    icon(a.heart, 92.8f, PLAYER_ACTIONS_Y, 8f, words, u)
    drawCircle(words, 7.2f * u, Offset(112f * u, PLAYER_ACTIONS_Y * u))
    icon(a.palette, 112f, PLAYER_ACTIONS_Y, 6.4f, c.panel, u)
    drawCircle(words, 7.2f * u, Offset(PLAYER_MORE_X * u, PLAYER_ACTIONS_Y * u))
    icon(a.moreRound, PLAYER_MORE_X, PLAYER_ACTIONS_Y, 6.4f, c.panel, u)

    pill(soft.fade(0.35f), 11.6f, 224.6f, 126.8f, 3.7f, u)
    pill(if (video > 0.5f) words else lerp(c.accent, Color.White, 0.35f), 11.6f, 224.6f, 126.8f * (0.22f + 0.06f * t), 3.7f, u)
    text(m, "0:45", 12.5f, 234.6f, 4.8f, words, u)
    text(m, "3:49", 138.4f - textWidth(m, "3:49", 4.8f, u), 234.6f, 4.8f, words, u)

    icon(a.shuffle, 23.4f, 263.4f, 8f, words, u)
    icon(a.previous, 47.2f, 263.4f, 8.5f, words, u)
    drawRoundRect(lerp(c.accent, Color.White, 0.5f), Offset(61.8f * u, 250.4f * u), Size(26f * u, 26f * u), CornerRadius(8.5f * u))
    icon(a.pause, 74.8f, 263.4f, 10f, lerp(c.accent, Color.Black, 0.65f), u)
    icon(a.next, 102.8f, 263.4f, 8.5f, words, u)
    icon(a.repeat, 126.25f, 263.4f, 8f, words, u)

    listOf(a.queue to a.queueLabel, a.cast to a.castLabel, a.moon to a.sleepTimer, a.lyrics to a.lyricsLabel)
        .forEachIndexed { i, (button, label) ->
            icon(button, PLAYER_ROW_X[i], PLAYER_ROW_Y, 7f, words, u)
            centredText(m, label, PLAYER_ROW_X[i], 303.7f, 4.6f, words, u, bold = false)
        }
}

/** The song's video, in the mock: soft light drifting over a dark picture. */
private fun DrawScope.videoBackdrop(
    p: Ph,
    video: Float,
) = with(p) {
    drawRect(VideoDark.fade(video))
    for (k in 0 until 3) {
        val angle = t * 2f * PI.toFloat() + k * 2.1f
        val centre = Offset((75f + 55f * sin(angle)) * u, (100f + 50f * cos(angle * 1.3f)) * u)
        val (shade, _) = c.covers[k]
        val radius = (60f + k * 14f) * u
        drawCircle(Brush.radialGradient(listOf(shade.fade(0.8f * video), Color.Transparent), centre, radius), radius, centre)
    }
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.fade(0.6f * video)), 120f * u, SCREEN_H * u))
}

/** The player menu's last rows: sleep timer, equalizer, and 3D sound with its three modes. */
internal fun DrawScope.playerMenu(
    p: Ph,
    top: Float,
    chosen: Int,
) = with(p) {
    icon(a.moon, 18f, top + 22f, 10f, c.ink, u)
    text(m, a.sleepTimer, 30f, top + 18f, 7.5f, c.ink, u, maxWidth = 110f)
    icon(a.equalizer, 18f, top + 44f, 10f, c.ink, u)
    text(m, a.equalizerLabel, 30f, top + 40f, 7.5f, c.ink, u, maxWidth = 110f)
    icon(a.note, 18f, top + 66f, 10f, c.accent, u)
    text(m, a.spatial, 30f, top + 62f, 7.5f, c.ink, u, bold = true, maxWidth = 110f)
    a.spatialModes.forEachIndexed { i, mode ->
        val x = 14f + i * 42f
        val selected = i == chosen
        pill(if (selected) c.accent else c.control, x, top + 78f, 38f, 18f, u)
        centredText(m, mode, x + 19f, top + 87f, 6.5f, if (selected) c.onAccent else c.onControl, u)
    }
}

/** Centre of 3D sound's mode [index] in the menu, for the finger. */
internal fun spatialModeX(index: Int) = 33f + index * 42f

/** A note circling the cover, behind it and in front of it, as 8D sounds. */
internal fun DrawScope.orbitAroundCover(
    p: Ph,
    strength: Float,
) = with(p) {
    if (strength <= 0f) return@with
    val cx = 75f
    val cy = 114.8f
    drawOval(
        c.accent.fade(0.5f * strength),
        topLeft = Offset((cx - 70f) * u, (cy - 18f) * u),
        size = Size(140f * u, 36f * u),
        style = Stroke(1.2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * u, 4f * u))),
    )
    val angle = t * 6f * PI.toFloat()
    val depth = sin(angle)
    val size = 12f * (0.8f + 0.3f * (depth + 1f) / 2f)
    icon(a.note, cx + 70f * cos(angle), cy + 18f * depth, size, c.accent.fade(strength * if (depth > 0f) 1f else 0.4f), u)
}

/** Search with no connection: the field, the notice, and the songs saved on the phone. */
internal fun DrawScope.offlineSearch(
    p: Ph,
    typed: String,
    caret: Boolean,
    results: Float,
) = with(p) {
    drawRect(c.panel)
    pill(c.raised, 8f, 16f, 134f, 22f, u)
    icon(a.back, 18f, 27f, 9f, c.ink, u)
    if (typed.isEmpty()) {
        text(m, a.searchOffline, 27f, 23.5f, 6.5f, c.inkSoft, u, maxWidth = 108f)
    } else {
        text(m, typed, 27f, 22.5f, 7.5f, c.ink, u, bold = true)
    }
    if (caret) {
        val width = if (typed.isEmpty()) 0f else textWidth(m, typed, 7.5f, u, bold = true)
        drawRect(c.accent, Offset((27.5f + width) * u, 22f * u), Size(1.4f * u, 10f * u))
    }
    icon(a.cloud, 15f, 52f, 9f, c.inkSoft, u)
    drawLine(c.inkSoft, Offset(10f * u, 47f * u), Offset(20f * u, 57f * u), strokeWidth = 1.2f * u, cap = StrokeCap.Round)
    text(m, a.offlineNotice, 25f, 45f, 5.8f, c.inkSoft, u, maxWidth = 118f, lines = 3)
    OFFLINE_RESULTS.forEachIndexed { i, song ->
        val shown = easeOut(seg(results, i * 0.25f, 0.5f + i * 0.25f))
        if (shown > 0f) {
            val y = 76f + i * 26f + (1f - shown) * 6f
            cover(c, i, a.note, 10f, y, 18f, 3f, u, alpha = shown)
            songLines(m, c, song, 33f, y + 1f, u, titleSize = 7.5f, maxWidth = 90f, alpha = shown)
            icon(a.saved, 136f, y + 9f, 9f, c.accent.fade(shown), u)
        }
    }
}

/** The Trending chart the shelf's arrow opens. */
internal fun DrawScope.trendingChart(p: Ph) =
    with(p) {
        drawRect(c.panel)
        icon(a.back, 14f, 25f, 10f, c.ink, u)
        text(m, a.trending, 26f, 20f, 9f, c.ink, u, bold = true)
        drawRoundRect(
            Brush.linearGradient(listOf(c.accent, c.artEnd), Offset(8f * u, 38f * u), Offset(142f * u, 96f * u)),
            Offset(8f * u, 38f * u),
            Size(134f * u, 58f * u),
            CornerRadius(12f * u),
        )
        icon(a.rising, 28f, 67f, 20f, c.onAccent, u)
        text(m, a.trending, 44f, 60f, 11f, c.onAccent, u, bold = true, maxWidth = 66f)
        drawCircle(c.onAccent, 11f * u, Offset(126f * u, 67f * u))
        icon(a.play, 126f, 67f, 11f, c.accent, u)
        for (i in 0 until 6) {
            val y = 106f + i * 26f
            centredText(m, "${i + 1}", 13f, y + 9f, 8f, c.accent, u)
            cover(c, (i + 3) % 6, a.note, 22f, y, 18f, 3f, u)
            songLines(m, c, SAMPLE_SONGS[(i + 3) % 6], 46f, y + 1f, u, titleSize = 7.5f, maxWidth = 90f)
        }
    }

/** Song recognition: listening, then the song it found. */
internal fun DrawScope.listening(
    p: Ph,
    found: Float,
) = with(p) {
    drawRect(c.panel)
    val centre = Offset(75f * u, 128f * u)
    drawRect(Brush.radialGradient(listOf(c.accent.fade(0.28f), Color.Transparent), centre, 120f * u))
    icon(a.down, 14f, 22f, 11f, c.ink, u)
    for (k in 0 until 3) {
        val ring = (t * 3f + k / 3f) % 1f
        drawCircle(c.accent.fade((1f - ring) * 0.45f), (28f + ring * 44f) * u, centre, style = Stroke(1.5f * u))
    }
    drawCircle(c.accent, (26f + 1.5f * sin(t * 12f * PI.toFloat())) * u, centre)
    icon(a.wave, 75f, 128f, 22f, c.onAccent, u)
    for (i in 0 until 11) {
        val h = 4f + 16f * abs(sin(t * 2f * PI.toFloat() * 6f + i * 0.7f))
        pill(c.accent.fade(0.8f), 47.5f + i * 5.2f, 182f - h / 2f, 2.6f, h, u)
    }
    centredText(m, a.listening, 75f, 204f, 7f, c.inkSoft, u, bold = false)

    if (found > 0f) {
        val y = 232f + (1f - found) * 70f
        drawRoundRect(c.raised, Offset(10f * u, y * u), Size(130f * u, 64f * u), CornerRadius(12f * u))
        cover(c, 2, a.note, 18f, y + 10f, 44f, 8f, u)
        songLines(m, c, SAMPLE_SONGS[2], 68f, y + 14f, u, titleSize = 8f, maxWidth = 66f)
        drawCircle(c.accent, 9f * u, Offset(124f * u, (y + 46f) * u))
        icon(a.play, 124f, y + 46f, 9f, c.onAccent, u)
    }
}

/** One row of the queue. */
internal fun DrawScope.queueRow(
    p: Ph,
    song: Int,
    y: Float,
    current: Boolean,
    alpha: Float = 1f,
    dx: Float = 0f,
) = with(p) {
    if (alpha <= 0f) return@with
    drawRect(c.raised.fade(alpha), Offset((6f + dx) * u, (y - 2f) * u), Size(138f * u, 22f * u))
    cover(c, song, a.note, 10f + dx, y, 18f, 3f, u, alpha = alpha)
    songLines(m, c, SAMPLE_SONGS[song], 33f + dx, y + 0.5f, u, titleSize = 7.5f, maxWidth = 88f, alpha = alpha, titleColor = if (current) c.accent else c.ink)
    if (current) playingBars(c, 126f + dx, y + 3f, t, u, alpha) else pill(c.inkSoft.fade(0.5f * alpha), 128f + dx, y + 8f, 10f, 1.6f, u)
}

/** The account sheet: who is signed in, and the accounts kept on this phone. */
internal fun DrawScope.accountSheet(
    p: Ph,
    top: Float,
    current: Int,
) = with(p) {
    avatar(m, ACCOUNT_NAMES[current].take(1), 75f, top + 30f, 16f, if (current == 0) c.accent else c.control, if (current == 0) c.onAccent else c.onControl, u)
    centredText(m, ACCOUNT_NAMES[current], 75f, top + 55f, 8.5f, c.ink, u)
    centredText(m, ACCOUNT_HANDLES[current], 75f, top + 66f, 6.5f, c.inkSoft, u, bold = false)
    text(m, a.accounts, 12f, top + 78f, 6f, c.inkSoft, u, bold = true, maxWidth = 126f)
    for (who in 0 until 2) {
        val y = top + 98f + who * 22f
        avatar(m, ACCOUNT_NAMES[who].take(1), 20f, y, 7f, if (who == 0) c.accent else c.control, if (who == 0) c.onAccent else c.onControl, u)
        text(m, ACCOUNT_NAMES[who], 32f, y - 5f, 7.5f, c.ink, u, bold = true)
        if (who == current) icon(a.check, 136f, y, 9f, c.accent, u)
    }
}
