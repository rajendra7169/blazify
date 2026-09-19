package com.blazify.music.ui.player

import android.content.Context
import android.net.ConnectivityManager
import android.util.LruCache
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.blazify.music.constants.AnimatedCoversKey
import com.blazify.music.constants.SaveDataOnMobileKey
import com.blazify.music.models.MediaMetadata
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.get
import com.blazify.paxsenix.Paxsenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max

/**
 * Where a song's moving cover lives, remembered for the session so each song is only asked about
 * once. A song Apple has no moving cover for is remembered too, as an empty answer.
 */
private object MotionCovers {
    private val known = LruCache<String, String>(300)
    private const val NONE = ""

    suspend fun tallFor(context: Context, song: MediaMetadata): String? {
        known.get(song.id)?.let { return it.ifEmpty { null } }
        val artist = song.artists.joinToString { it.name }
        if (song.title.isBlank() || artist.isBlank()) return null

        val url =
            withContext(Dispatchers.IO) {
                Paxsenix.init(context)
                Paxsenix.motionCover(song.title, artist, song.duration)?.let { it.tall ?: it.square }
            }
        known.put(song.id, url ?: NONE)
        if (url != null) Timber.tag("MotionCover").i("moving cover for ${song.title}")
        return url
    }
}

/**
 * The song's moving cover, when there is one and the listener wants it.
 *
 * Nothing is fetched on mobile data while "save data on mobile" is on: a cover is a small video,
 * but a video all the same, looping for as long as the song plays.
 */
@Composable
fun rememberMotionCover(song: MediaMetadata?): String? {
    val context = LocalContext.current
    return produceState<String?>(initialValue = null, song?.id) {
        value = null
        song ?: return@produceState
        if (!context.dataStore.get(AnimatedCoversKey, true)) return@produceState
        val metered =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
        if (metered && context.dataStore.get(SaveDataOnMobileKey, false)) return@produceState
        value = runCatching { MotionCovers.tallFor(context, song) }.getOrNull()
    }.value
}

/**
 * A cover that moves: the looping video, muted, cut to fill whatever space it is given, faded in
 * over the still artwork once its first frame is on screen — so there is never a black gap while
 * it loads, and a cover that fails simply never appears.
 *
 * It only plays while [active]: a player out of sight has no business decoding video.
 */
@OptIn(UnstableApi::class)
@Composable
fun MotionCoverVideo(
    url: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var shown by remember(url) { mutableStateOf(false) }
    var videoSize by remember(url) { mutableStateOf<VideoSize?>(null) }

    val player =
        remember(url) {
            ExoPlayer.Builder(context).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
                setMediaItem(
                    MediaItem
                        .Builder()
                        .setUri(url)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build(),
                )
                prepare()
            }
        }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    shown = true
                }

                override fun onVideoSizeChanged(size: VideoSize) {
                    if (size.width > 0 && size.height > 0) videoSize = size
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, active) {
        player.playWhenReady = active
    }

    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "motionCover",
    )

    BoxWithConstraints(
        modifier = modifier.clipToBounds().alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        // Cut to fill, like the still artwork beneath it: scaled until both sides are covered,
        // the overflow trimmed off evenly.
        val size = videoSize
        val drawModifier =
            if (size == null) {
                Modifier.requiredSize(maxWidth, maxHeight)
            } else {
                val aspect = size.width * size.pixelWidthHeightRatio / size.height
                val scale = max(maxWidth.value / aspect, maxHeight.value)
                Modifier.requiredSize((scale * aspect).dp, scale.dp)
            }
        AndroidView(
            factory = { TextureView(it).also { view -> player.setVideoTextureView(view) } },
            modifier = drawModifier,
        )
    }
}
