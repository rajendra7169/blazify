/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.phone

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.blazify.music.R
import com.blazify.music.ui.screens.whatsnew.demos.DemoColors
import com.blazify.music.ui.screens.whatsnew.demos.fade
import com.blazify.music.ui.screens.whatsnew.demos.finger
import com.blazify.music.ui.screens.whatsnew.demos.mix
import com.blazify.music.ui.screens.whatsnew.demos.text
import java.util.Calendar
import kotlin.math.min

/*
 * A phone with Blazify on it, drawn in code: the frame, and the pieces the screens and the
 * journeys share. Screens are laid out on a 150 × 320 screen, the frame adds 5 all round.
 */

internal const val SCREEN_W = 150f
internal const val SCREEN_H = 320f
internal const val BEZEL = 5f
internal const val PHONE_W = SCREEN_W + 2 * BEZEL
internal const val PHONE_H = SCREEN_H + 2 * BEZEL
internal val BezelColour = Color(0xFF1B1B1F)

// Where the mini player sits, as on the real Home: a pill just above the navigation bar.
internal const val MINI_LEFT = 4.4f
internal const val MINI_TOP = 261f
internal const val MINI_W = 141.2f
internal const val MINI_H = 22.7f

/** Everything one frame of a phone screen is drawn with. */
internal class Ph(
    val c: DemoColors,
    val a: PhoneAssets,
    val m: TextMeasurer,
    val u: Float,
    val t: Float,
    /** For clipping the player as it grows out of the mini player. */
    val clip: Path,
    /** For fading a whole screen in or out. */
    val layer: Paint,
)

/** The app's own icons, pictures and words that the mock-up shows. Nothing here is new to the APK. */
internal class PhoneAssets(
    val person: Painter,
    val settings: Painter,
    val search: Painter,
    val wave: Painter,
    val pause: Painter,
    val play: Painter,
    val next: Painter,
    val previous: Painter,
    val more: Painter,
    val moreRound: Painter,
    val palette: Painter,
    val add: Painter,
    val radio: Painter,
    val library: Painter,
    val cast: Painter,
    val down: Painter,
    val queue: Painter,
    val lyrics: Painter,
    val moon: Painter,
    val equalizer: Painter,
    val shuffle: Painter,
    val repeat: Painter,
    val heart: Painter,
    val home: Painter,
    val grid: Painter,
    val back: Painter,
    val check: Painter,
    val saved: Painter,
    val cloud: Painter,
    val rising: Painter,
    val arrow: Painter,
    val playNext: Painter,
    val note: Painter,
    val logo: Painter,
    val hero: Painter,
    val greeting: String,
    val userName: String,
    val homeHint: String,
    val quickPicks: String,
    val trending: String,
    val navHome: String,
    val navSearch: String,
    val navYours: String,
    val navLibrary: String,
    val forYou: String,
    val playAll: String,
    val castLabel: String,
    val lyricsLabel: String,
    val nowPlaying: String,
    val spatial: String,
    val spatialModes: List<String>,
    val sleepTimer: String,
    val equalizerLabel: String,
    val queueLabel: String,
    val playNextLabel: String,
    val similar: String,
    val accounts: String,
    val listening: String,
    val searchOffline: String,
    val offlineNotice: String,
    val videoArt: String,
)

@Composable
internal fun rememberPhoneAssets(): PhoneAssets {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Home's two pictures, loaded by Coil off the main thread at the small size they are drawn.
    val hero =
        rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(if (dark) R.drawable.blaze_home_dark else R.drawable.blaze_home_light)
                .size(240)
                .build(),
        )
    val logo = rememberAsyncImagePainter(ImageRequest.Builder(context).data(R.drawable.blaze_logo).size(64).build())
    return PhoneAssets(
        person = painterResource(R.drawable.person),
        settings = painterResource(R.drawable.settings),
        search = painterResource(R.drawable.search),
        wave = painterResource(R.drawable.sound_wave),
        pause = painterResource(R.drawable.pause),
        play = painterResource(R.drawable.play),
        next = painterResource(R.drawable.skip_next),
        previous = painterResource(R.drawable.skip_previous),
        more = painterResource(R.drawable.more_vert),
        moreRound = painterResource(R.drawable.more_horiz),
        palette = painterResource(R.drawable.palette),
        add = painterResource(R.drawable.add),
        radio = painterResource(R.drawable.radio),
        library = painterResource(R.drawable.library_music_outlined),
        cast = painterResource(R.drawable.cast),
        down = painterResource(R.drawable.expand_more),
        queue = painterResource(R.drawable.queue_music),
        lyrics = painterResource(R.drawable.lyrics),
        moon = painterResource(R.drawable.bedtime),
        equalizer = painterResource(R.drawable.equalizer),
        shuffle = painterResource(R.drawable.shuffle),
        repeat = painterResource(R.drawable.repeat),
        heart = painterResource(R.drawable.favorite_border),
        home = painterResource(R.drawable.home_filled),
        grid = painterResource(R.drawable.grid_view),
        back = painterResource(R.drawable.arrow_back),
        check = painterResource(R.drawable.check),
        saved = painterResource(R.drawable.offline),
        cloud = painterResource(R.drawable.cloud),
        rising = painterResource(R.drawable.trending_up),
        arrow = painterResource(R.drawable.arrow_forward),
        playNext = painterResource(R.drawable.playlist_play),
        note = painterResource(R.drawable.music_note),
        logo = logo,
        hero = hero,
        greeting = stringResource(greetingRes()),
        userName = stringResource(R.string.blaze_greeting_default_name),
        homeHint = stringResource(R.string.home_search_hint),
        quickPicks = stringResource(R.string.quick_picks),
        trending = stringResource(R.string.trending),
        navHome = stringResource(R.string.home),
        navSearch = stringResource(R.string.search),
        navYours = stringResource(R.string.yours),
        navLibrary = stringResource(R.string.filter_library),
        forYou = stringResource(R.string.home_for_you),
        playAll = stringResource(R.string.play_all),
        castLabel = stringResource(R.string.cast),
        lyricsLabel = stringResource(R.string.lyrics),
        nowPlaying = stringResource(R.string.now_playing),
        spatial = stringResource(R.string.spatial_sound),
        spatialModes =
            listOf(
                stringResource(R.string.spatial_sound_off),
                stringResource(R.string.spatial_sound_surround),
                stringResource(R.string.spatial_sound_orbit),
            ),
        sleepTimer = stringResource(R.string.sleep_timer),
        equalizerLabel = stringResource(R.string.equalizer),
        queueLabel = stringResource(R.string.queue),
        playNextLabel = stringResource(R.string.play_next),
        similar = stringResource(R.string.similar_content),
        accounts = stringResource(R.string.accounts_and_channels),
        listening = stringResource(R.string.recognition_notification_listening),
        searchOffline = stringResource(R.string.search_offline),
        offlineNotice = stringResource(R.string.search_offline_notice),
        videoArt = stringResource(R.string.player_design_video),
    )
}

/** The same greeting Home's card shows for the time of day. */
private fun greetingRes(): Int {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> R.string.home_greeting_morning
        hour in 12..16 -> R.string.home_greeting_afternoon
        hour in 17..20 -> R.string.home_greeting_evening
        else -> R.string.home_greeting_night
    }
}

/**
 * Draws the phone, centred and scaled to fit, and [block] on its screen, clipped to the
 * screen's rounded corners; `u` is pixels per screen unit.
 */
internal inline fun DrawScope.inPhone(
    c: DemoColors,
    m: TextMeasurer,
    frame: Path,
    block: DrawScope.(u: Float) -> Unit,
) {
    val u = min(size.width / PHONE_W, size.height / PHONE_H)
    translate((size.width - PHONE_W * u) / 2f, (size.height - PHONE_H * u) / 2f) {
        drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(2f * u, 7f * u), Size(PHONE_W * u, PHONE_H * u), CornerRadius(25f * u))
        drawRoundRect(BezelColour, Offset.Zero, Size(PHONE_W * u, PHONE_H * u), CornerRadius(25f * u))
        drawRoundRect(Color.White.copy(alpha = 0.14f), Offset.Zero, Size(PHONE_W * u, PHONE_H * u), CornerRadius(25f * u), style = Stroke(1f * u))
        translate(BEZEL * u, BEZEL * u) {
            frame.reset()
            frame.addRoundRect(RoundRect(0f, 0f, SCREEN_W * u, SCREEN_H * u, CornerRadius(20f * u)))
            clipPath(frame) {
                block(u)
                // The status bar and the gesture bar, on top of whatever the screen shows.
                text(m, "9:41", 6f, 2f, 5.5f, c.ink, u, bold = true)
                drawRoundRect(c.ink, Offset(134f * u, 3.6f * u), Size(4f * u, 5.6f * u), CornerRadius(1f * u))
                drawCircle(c.ink, 2.6f * u, Offset(128f * u, 7f * u))
                drawRoundRect(c.ink.fade(0.55f), Offset(59f * u, 316.6f * u), Size(32f * u, 1.6f * u), CornerRadius(0.8f * u))
            }
            drawCircle(BezelColour, 3.2f * u, Offset(SCREEN_W / 2f * u, 7f * u))
        }
    }
}

/** Draws [block] at [alpha], as one layer, so a whole screen fades in or out. */
internal inline fun DrawScope.faded(
    p: Ph,
    alpha: Float,
    block: DrawScope.() -> Unit,
) {
    if (alpha <= 0f) return
    if (alpha >= 1f) {
        block()
        return
    }
    p.layer.alpha = alpha
    drawContext.canvas.saveLayer(Rect(-size.width, -size.height, size.width * 2f, size.height * 2f), p.layer)
    block()
    drawContext.canvas.restore()
}

/** A picture as it is, untinted. */
internal fun DrawScope.image(
    painter: Painter,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    u: Float,
) {
    translate(x * u, y * u) { with(painter) { draw(Size(w * u, h * u)) } }
}

/** A bottom sheet [height] tall, [progress] of the way up, over a dimmed screen. */
internal inline fun DrawScope.sheet(
    p: Ph,
    progress: Float,
    height: Float,
    content: DrawScope.(top: Float) -> Unit,
) {
    if (progress <= 0f) return
    val u = p.u
    drawRect(Color.Black.fade(0.32f * progress))
    val top = SCREEN_H - height * progress
    drawRoundRect(p.c.raised, Offset(0f, top * u), Size(SCREEN_W * u, (height + 24f) * u), CornerRadius(16f * u))
    drawRoundRect(p.c.inkSoft.fade(0.4f), Offset(63f * u, (top + 5f) * u), Size(24f * u, 3f * u), CornerRadius(1.5f * u))
    content(top)
}

/** The player growing out of the mini player, as it does when the mini player is tapped. */
internal inline fun DrawScope.fromMiniPlayer(
    p: Ph,
    progress: Float,
    content: DrawScope.() -> Unit,
) {
    if (progress <= 0f) return
    val u = p.u
    p.clip.reset()
    p.clip.addRoundRect(
        RoundRect(
            mix(MINI_LEFT, 0f, progress) * u,
            mix(MINI_TOP, 0f, progress) * u,
            mix(MINI_LEFT + MINI_W, SCREEN_W, progress) * u,
            mix(MINI_TOP + MINI_H, SCREEN_H, progress) * u,
            CornerRadius(mix(MINI_H / 2f, 20f, progress) * u),
        ),
    )
    clipPath(p.clip) { faded(p, (progress * 2.5f).coerceAtMost(1f)) { content() } }
}

/** The finger, sized for the phone. */
internal fun DrawScope.tapAt(
    p: Ph,
    x: Float,
    y: Float,
    visible: Float,
    tap: Float,
) = finger(p.c, x, y, visible, tap, p.u, size = 8f)
