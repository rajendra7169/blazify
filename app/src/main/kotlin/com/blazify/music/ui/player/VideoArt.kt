package com.blazify.music.ui.player

import android.content.Context
import android.net.ConnectivityManager
import android.os.SystemClock
import android.util.LruCache
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil3.compose.AsyncImage
import com.blazify.innertube.YouTube
import com.blazify.innertube.models.SongItem
import com.blazify.music.constants.SaveDataOnMobileKey
import com.blazify.music.models.MediaMetadata
import com.blazify.music.playback.PlayerConnection
import com.blazify.music.utils.YTPlayerUtils
import com.blazify.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max

/**
 * The video that goes behind a song in the Video player.
 *
 * [synced] says whether it can follow the song second by second: true when the song is the video
 * itself, or when the music video is the same length as the song. Otherwise it is usually a
 * different cut — a longer intro, a shorter edit — and it loops on its own as a moving backdrop.
 */
data class SongVideo(
    val videoId: String,
    val streamUrl: String,
    val synced: Boolean,
)

/** Looked up once per song and remembered, a song with no video included. */
private object SongVideos {
    private class Found(val video: SongVideo?, val at: Long)

    private val known = LruCache<String, Found>(200)

    /** Stream links stop working after a few hours, so an answer is trusted for less than that. */
    private const val KEEP_MS = 3 * 60 * 60 * 1000L

    /** "No video" may only mean the connection dropped, so that is asked again soon. */
    private const val KEEP_NONE_MS = 2 * 60 * 1000L

    /** How far apart a song and its video may be in length and still be the same cut. */
    private const val SAME_CUT_SECONDS = 3

    /**
     * Lookups run on their own, not on the screen asking: the player lays itself out again as it
     * opens, and an answer cut short there would otherwise be taken for "no video". The preview
     * and the player asking at once share one lookup.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = HashMap<String, Deferred<SongVideo?>>()

    suspend fun forSong(song: MediaMetadata, maxHeight: Int): SongVideo? {
        val key = "${song.id}@$maxHeight"
        known.get(key)?.let { found ->
            val age = SystemClock.elapsedRealtime() - found.at
            if (age < if (found.video == null) KEEP_NONE_MS else KEEP_MS) return found.video
        }
        val lookup =
            synchronized(pending) {
                pending.getOrPut(key) {
                    scope.async {
                        val found = runCatching { find(song, maxHeight) }.getOrNull()
                        known.put(key, Found(found, SystemClock.elapsedRealtime()))
                        synchronized(pending) { pending.remove(key) }
                        found
                    }
                }
            }
        return lookup.await()
    }

    private suspend fun find(song: MediaMetadata, maxHeight: Int): SongVideo? {
        // A song that is itself a music video already is the picture to show.
        if (song.isVideoSong) {
            val url = YTPlayerUtils.videoStreamUrl(song.id, maxHeight) ?: return null
            return SongVideo(song.id, url, synced = true)
        }

        if (song.title.isBlank()) return null
        val artist = song.artists.joinToString { it.name }
        val candidate =
            YouTube
                .search("${song.title} $artist", YouTube.SearchFilter.FILTER_VIDEO)
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                ?.firstOrNull { it.isVideoSong }
                ?: return null
        val url = YTPlayerUtils.videoStreamUrl(candidate.id, maxHeight) ?: return null
        val sameCut = candidate.duration?.let { abs(it - song.duration) <= SAME_CUT_SECONDS } == true
        Timber.tag("VideoArt").d("video for ${song.id}: ${candidate.id}, ${if (sameCut) "in step" else "looping"}")
        return SongVideo(candidate.id, url, synced = sameCut)
    }
}

/** What is known about a song's video: still looking, or the answer — null when it has none. */
class VideoLookup(val done: Boolean, val video: SongVideo?)

/**
 * The song's video, when there is one and it is worth the data.
 *
 * A video is far heavier than a picture: on mobile data it comes in a smaller size, and not at all
 * while "save data on mobile" is on — the still artwork shows instead.
 */
@Composable
fun rememberVideoLookup(song: MediaMetadata?): VideoLookup {
    val context = LocalContext.current
    return produceState(initialValue = VideoLookup(done = false, video = null), song?.id) {
        value = VideoLookup(done = false, video = null)
        song ?: return@produceState
        val metered =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
        val video =
            if (metered && context.dataStore.data.first()[SaveDataOnMobileKey] == true) {
                null
            } else {
                SongVideos.forSong(song, maxHeight = if (metered) 360 else 720)
            }
        value = VideoLookup(done = true, video = video)
    }.value
}

@Composable
fun rememberSongVideo(song: MediaMetadata?): SongVideo? = rememberVideoLookup(song).video

/**
 * The Video design's picture, standing up: the whole video across the width of the screen, none
 * of it cut away at the sides, its top and bottom edges fading into [background]. Until the video
 * is moving — and for a song without one — the artwork stands there instead, square.
 */
@Composable
fun VideoPanel(
    song: MediaMetadata?,
    playerConnection: PlayerConnection,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val lookup = rememberVideoLookup(song)
    // Kept from one song to the next, so a run of videos does not keep changing shape while each
    // one loads; only a song found to have no video goes back to the square.
    var videoShape by remember { mutableStateOf(lastPanelShape) }
    LaunchedEffect(lookup) {
        if (lookup.done && lookup.video == null) videoShape = null
    }
    LaunchedEffect(videoShape) { lastPanelShape = videoShape }
    val shape by animateFloatAsState(
        targetValue = videoShape ?: 1f,
        animationSpec = tween(durationMillis = 600),
        label = "videoPanel",
    )

    Box(modifier.fillMaxWidth().aspectRatio(shape).clipToBounds()) {
        AsyncImage(
            model = song?.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        lookup.video?.let { video ->
            key(video.streamUrl) {
                VideoArt(
                    video = video,
                    playerConnection = playerConnection,
                    modifier = Modifier.fillMaxSize(),
                    wholeWidth = true,
                    onShape = { videoShape = it },
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to background,
                        PANEL_EDGE to Color.Transparent,
                        1f - PANEL_EDGE to Color.Transparent,
                        1f to background,
                    ),
                ),
        )
    }
}

/** The shape the Video design's picture last had, for the next time the player opens. */
private var lastPanelShape: Float? = null

/** How much of the picture's height, at the top and again at the bottom, fades into the page. */
private const val PANEL_EDGE = 0.1f

/**
 * The video, muted, faded in over the still artwork once it is moving. It fills the space the way
 * the artwork does, or with [wholeWidth] spans the width with nothing cut at the sides, telling
 * [onShape] the width-to-height of the part worth showing. The sound is always the song's own,
 * from the player; this is only the picture, and it stops whenever the app is out of sight.
 *
 * When [SongVideo.synced], it is kept to the song's position and follows every pause and seek.
 * Otherwise it starts about as far in as the song is, past the opening titles, and goes round
 * again for as long as the song plays.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoArt(
    video: SongVideo,
    playerConnection: PlayerConnection,
    modifier: Modifier = Modifier,
    wholeWidth: Boolean = false,
    onShape: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val inSight = lifecycle.isAtLeast(Lifecycle.State.STARTED)
    var firstFrame by remember { mutableStateOf(false) }
    var hasPlayed by remember { mutableStateOf(false) }
    // A video that has not played yet may be sitting on a black opening frame, so a paused song
    // keeps its artwork until then; a video in step is paused on the song's own moment instead.
    val onFrame = firstFrame && (video.synced || hasPlayed)
    var videoSize by remember { mutableStateOf<VideoSize?>(null) }
    var surface by remember { mutableStateOf<TextureView?>(null) }
    var bands by remember { mutableFloatStateOf(0f) }
    var bandsLooked by remember { mutableStateOf(false) }
    // Across the width the bands would show against the page, so the picture waits for the first
    // look at them.
    val shown = onFrame && (!wholeWidth || bandsLooked)

    val player =
        remember {
            val http = OkHttpClient.Builder().proxy(YouTube.proxy).build()
            ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(http)))
                .build()
                .apply {
                    volume = 0f
                    setMediaItem(MediaItem.fromUri(video.streamUrl))
                    prepare()
                }
        }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    firstFrame = true
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) hasPlayed = true
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

    // Follow the song: play when it plays, and — for a video of the same cut — stay on its second.
    // A jump in the video takes a moment to load while the song carries on, so it aims a little
    // ahead, learning from each jump how far; small differences are made up by running the
    // picture slightly faster or slower, which nobody notices without the sound.
    LaunchedEffect(player, inSight) {
        val song = playerConnection.player
        var lead = 800L
        var justJumped = false
        var placed = video.synced
        if (video.synced) player.seekTo(song.currentPosition + lead)
        while (isActive) {
            val playing = inSight && song.isPlaying
            player.playWhenReady = playing
            if (!video.synced) {
                val length = player.duration
                if (!placed && player.playbackState == Player.STATE_READY && length > 0) {
                    val along = if (song.duration > 0) song.currentPosition.toDouble() / song.duration else 0.0
                    player.seekTo(max((along * length).toLong(), OPENING_TITLES_MS.coerceAtMost(length / 4)))
                    placed = true
                } else if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(OPENING_TITLES_MS.coerceAtMost(length / 4))
                }
            }
            if (video.synced && playing && player.playbackState == Player.STATE_READY) {
                val drift = song.currentPosition - player.currentPosition
                if (justJumped) {
                    // Half the miss, so one slow answer does not throw the next jump far off.
                    lead = (lead + drift / 2).coerceIn(0L, MAX_LEAD_MS)
                    justJumped = false
                }
                when {
                    abs(drift) > JUMP_BEYOND_MS -> {
                        Timber.tag("VideoArt").d("jumping to the song, ${drift}ms off")
                        player.setPlaybackSpeed(1f)
                        player.seekTo(song.currentPosition + lead)
                        justJumped = true
                    }
                    abs(drift) > IN_STEP_MS -> player.setPlaybackSpeed(1f + (drift / 4000f).coerceIn(-0.25f, 0.25f))
                    else -> player.setPlaybackSpeed(1f)
                }
            }
            delay(250)
        }
    }

    // Some videos carry black bands above and below the picture — a cinema-shaped film inside a
    // TV-shaped frame. They are left outside: filling the screen, the picture is brought in close
    // enough; across the width, the space is made that much shorter. A few frames are looked at,
    // apart, and the thinnest bands seen are kept, so one dark scene is not taken for bands.
    LaunchedEffect(surface, onFrame) {
        val view = surface ?: return@LaunchedEffect
        if (!onFrame) return@LaunchedEffect
        var least = 1f
        for (wait in BAND_LOOKS_MS) {
            delay(wait)
            val frame = view.getBitmap(BAND_SAMPLE_WIDTH, BAND_SAMPLE_HEIGHT) ?: continue
            val pixels = IntArray(frame.width * frame.height)
            frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
            least = minOf(least, letterboxShare(pixels, frame.width, frame.height))
            frame.recycle()
            bands = if (least in MIN_BANDS..MAX_BANDS) least else 0f
            bandsLooked = true
        }
        bandsLooked = true
        if (bands > 0f) Timber.tag("VideoArt").d("bands above and below: ${(bands * 100).toInt()}% each")
    }
    val zoom by animateFloatAsState(
        targetValue = 1f / (1f - 2f * bands),
        animationSpec = tween(durationMillis = 600),
        label = "videoArtZoom",
    )

    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "videoArt",
    )

    val reportShape by rememberUpdatedState(onShape)
    LaunchedEffect(shown, videoSize, bands) {
        val size = videoSize ?: return@LaunchedEffect
        if (shown) reportShape(size.width * size.pixelWidthHeightRatio / size.height * (1f / (1f - 2f * bands)))
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds().alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        // Across the width, or cut to fill like the artwork: scaled until both sides are covered,
        // overflow trimmed evenly.
        val size = videoSize
        val drawModifier =
            if (size == null) {
                Modifier.requiredSize(maxWidth, maxHeight)
            } else {
                val aspect = size.width * size.pixelWidthHeightRatio / size.height
                if (wholeWidth) {
                    Modifier.requiredSize(maxWidth, (maxWidth.value / aspect).dp)
                } else {
                    val height = max(maxWidth.value / aspect, maxHeight.value) * zoom
                    Modifier.requiredSize((height * aspect).dp, height.dp)
                }
            }
        AndroidView(
            factory = {
                TextureView(it).also { view ->
                    player.setVideoTextureView(view)
                    surface = view
                }
            },
            modifier = drawModifier,
        )
    }
}

/**
 * How much of a frame's height, [pixels] being [width] by [height], is black band at the top and
 * again at the bottom, going by the thinner of the two. A label's logo or name printed in a band
 * leaves it a band. A frame dark almost all over says nothing either way, so it counts as none.
 */
internal fun letterboxShare(pixels: IntArray, width: Int, height: Int): Float {
    fun dark(row: Int) =
        (0 until width).count { x ->
            val colour = pixels[row * width + x]
            val red = colour shr 16 and 0xFF
            val green = colour shr 8 and 0xFF
            val blue = colour and 0xFF
            (red * 299 + green * 587 + blue * 114) / 1000 >= BAND_BRIGHTNESS
        } <= width * BAND_PRINT_SHARE
    var top = 0
    while (top < height / 2 && dark(top)) top++
    var bottom = 0
    while (bottom < height / 2 && dark(height - 1 - bottom)) bottom++
    val share = minOf(top, bottom).toFloat() / height
    return if (share > MAX_BANDS) 0f else share
}

/** Size of the frame taken to look for bands: plenty to find a band, cheap to read. */
private const val BAND_SAMPLE_WIDTH = 96
private const val BAND_SAMPLE_HEIGHT = 54

/** Brightness, out of 255, below which a pixel counts as band. */
private const val BAND_BRIGHTNESS = 24

/** How much of a band's row a logo or name printed in it may cover. */
private const val BAND_PRINT_SHARE = 0.25f

/** When the frames are looked at, each wait after the one before. */
private val BAND_LOOKS_MS = longArrayOf(300, 1200, 2000)

/** Bands thinner than this are left alone, and more than this is a dark scene, not a band. */
private const val MIN_BANDS = 0.04f
private const val MAX_BANDS = 0.22f

/** Label logos and title cards at the start of a video, skipped when it is only a backdrop. */
private const val OPENING_TITLES_MS = 6000L

/** Close enough to the sound that nobody could tell. */
private const val IN_STEP_MS = 80L

/** Further apart than this, the picture jumps to the song rather than catching up. */
private const val JUMP_BEYOND_MS = 2000L

/** The most a jump aims ahead of the song, however slow the connection. */
private const val MAX_LEAD_MS = 4000L
