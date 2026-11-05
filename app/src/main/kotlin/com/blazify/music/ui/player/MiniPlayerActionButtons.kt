/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * Draws the buttons chosen for the mini-player. The rounded designs show them as outlined
 * circles, as the like and add buttons always were; the flat bar as plain icons.
 */

package com.blazify.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.blazify.music.LocalDatabase
import com.blazify.music.LocalDownloadUtil
import com.blazify.music.LocalNavController
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.SleepTimerDefaultKey
import com.blazify.music.constants.SleepTimerFadeOutKey
import com.blazify.music.constants.SleepTimerStopAfterCurrentSongKey
import com.blazify.music.extensions.toggleRepeatMode
import com.blazify.music.models.MediaMetadata
import com.blazify.music.playback.ExoDownloadService
import com.blazify.music.ui.component.BlazeDownloadLoader
import com.blazify.music.ui.component.BlazeSleepTimerDialog
import com.blazify.music.ui.component.LocalMenuState
import com.blazify.music.ui.menu.AddToPlaylistDialog
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Colours of the mini-player the buttons sit on. */
internal class MiniPlayerColors(
    val primary: Color,
    val outline: Color,
    val onSurface: Color,
    val error: Color,
)

/**
 * The chosen buttons in order. Previous, play and next sit close together as one set of
 * controls; anything else keeps the usual gap. A button that cannot apply to this song,
 * like subscribing when there is no artist page, is left out rather than shown dead.
 */
@Composable
internal fun MiniPlayerActionRow(
    actions: List<MiniPlayerAction>,
    mediaMetadata: MediaMetadata,
    isOnAir: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    colors: MiniPlayerColors,
    flat: Boolean,
    flatPlayButton: @Composable () -> Unit,
) {
    var showSleepTimer by remember { mutableStateOf(false) }
    if (showSleepTimer) MiniSleepTimerDialog(onDismiss = { showSleepTimer = false })

    val shown = actions.filter { it != MiniPlayerAction.SUBSCRIBE_ARTIST || mediaMetadata.artists.firstOrNull()?.id != null }
    shown.forEachIndexed { index, action ->
        key(action) {
            if (index > 0 && !flat) {
                val tight = action.isTransport && shown[index - 1].isTransport
                Spacer(Modifier.width(if (tight) 0.dp else 8.dp))
            }
            if (flat && action == MiniPlayerAction.PLAY_PAUSE) {
                flatPlayButton()
            } else {
                MiniPlayerActionButton(
                    action = action,
                    mediaMetadata = mediaMetadata,
                    isOnAir = isOnAir,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    colors = colors,
                    flat = flat,
                    onSleepTimer = { showSleepTimer = true },
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerActionButton(
    action: MiniPlayerAction,
    mediaMetadata: MediaMetadata,
    isOnAir: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    colors: MiniPlayerColors,
    flat: Boolean,
    onSleepTimer: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val label = stringResource(action.labelRes)

    // The three buttons the rounded designs always had keep their own look exactly.
    if (!flat) {
        when (action) {
            MiniPlayerAction.LIKE -> {
                FavoriteButton(mediaMetadata.id, colors.error, colors.outline, colors.onSurface, enabled = !isOnAir)
                return
            }
            MiniPlayerAction.ADD_TO_PLAYLIST -> {
                AddToPlaylistButton(
                    onClick = {
                        menuState.show {
                            AddToPlaylistDialog(
                                isVisible = true,
                                onGetSong = { listOf(mediaMetadata.id) },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    outlineColor = colors.outline,
                    onSurfaceColor = colors.onSurface,
                    enabled = !isOnAir,
                )
                return
            }
            MiniPlayerAction.SUBSCRIBE_ARTIST -> {
                val artistId = mediaMetadata.artists.firstOrNull()?.id ?: return
                SubscribeButton(artistId, mediaMetadata, colors.primary, colors.outline, colors.onSurface)
                return
            }
            // Plain arrows beside the play button, as the Rounded design always drew them; as
            // outlined circles side by side they ran into each other.
            MiniPlayerAction.PREVIOUS, MiniPlayerAction.NEXT -> {
                val previous = action == MiniPlayerAction.PREVIOUS
                val canSkip = if (previous) canSkipPrevious else canSkipNext
                IconButton(
                    onClick = {
                        if (previous) playerConnection.player.seekToPreviousMediaItem() else playerConnection.player.seekToNext()
                    },
                    enabled = canSkip,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(action.iconRes),
                        contentDescription = label,
                        tint = colors.onSurface.copy(alpha = if (canSkip) 1f else 0.4f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                return
            }
            MiniPlayerAction.PLAY_PAUSE -> {
                val isPlaying by playerConnection.isPlaying.collectAsState()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable { playerConnection.togglePlayPause() },
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                return
            }
            else -> Unit
        }
    }

    // Everything else: what it shows, whether it is on, and what a tap does.
    var icon = action.iconRes
    var on = false
    var enabled = true
    var loading = false
    var onClick: () -> Unit = {}
    when (action) {
        MiniPlayerAction.PREVIOUS -> {
            enabled = canSkipPrevious
            onClick = { playerConnection.player.seekToPreviousMediaItem() }
        }
        MiniPlayerAction.NEXT -> {
            enabled = canSkipNext
            onClick = { playerConnection.player.seekToNext() }
        }
        MiniPlayerAction.LIKE -> {
            val song by database.song(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = null)
            on = song?.song?.liked == true
            icon = if (on) R.drawable.favorite else R.drawable.favorite_border
            enabled = !isOnAir
            onClick = { playerConnection.service.toggleLike() }
        }
        MiniPlayerAction.ADD_TO_PLAYLIST -> {
            enabled = !isOnAir
            onClick = {
                menuState.show {
                    AddToPlaylistDialog(isVisible = true, onGetSong = { listOf(mediaMetadata.id) }, onDismiss = menuState::dismiss)
                }
            }
        }
        MiniPlayerAction.SUBSCRIBE_ARTIST -> {
            // Only reached on the flat bar; the rounded designs use SubscribeButton above.
            val artistId = mediaMetadata.artists.firstOrNull()?.id ?: return
            val artist by database.artist(artistId).collectAsStateWithLifecycle(initialValue = null)
            on = artist?.artist?.bookmarkedAt != null
            icon = if (on) R.drawable.subscribed else R.drawable.subscribe
            onClick = { artist?.artist?.let { a -> database.transaction { update(a.toggleLike()) } } }
        }
        MiniPlayerAction.VIEW_ARTIST -> {
            val navController = LocalNavController.current
            val artistId = mediaMetadata.artists.firstOrNull()?.id
            enabled = artistId != null
            onClick = { artistId?.let { navController.navigate("artist/$it") } }
        }
        MiniPlayerAction.START_RADIO -> {
            enabled = !isOnAir
            onClick = { playerConnection.startRadioSeamlessly() }
        }
        MiniPlayerAction.DOWNLOAD -> {
            val downloadUtil = LocalDownloadUtil.current
            val download by downloadUtil.getDownload(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = null)
            val state = download?.state
            on = state == Download.STATE_COMPLETED
            loading = state == Download.STATE_DOWNLOADING || state == Download.STATE_QUEUED
            if (on) icon = R.drawable.offline
            enabled = !isOnAir && !on && !loading
            onClick = {
                database.transaction { insert(mediaMetadata) }
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    DownloadRequest
                        .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                        .setCustomCacheKey(mediaMetadata.id)
                        .setData(mediaMetadata.title.toByteArray())
                        .build(),
                    false,
                )
            }
        }
        MiniPlayerAction.SLEEP_TIMER -> {
            on = playerConnection.service.sleepTimer?.isActive == true
            onClick = onSleepTimer
        }
        MiniPlayerAction.SHUFFLE -> {
            val shuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
            on = shuffle
            onClick = { playerConnection.player.shuffleModeEnabled = !shuffle }
        }
        MiniPlayerAction.REPEAT -> {
            val repeat by playerConnection.repeatMode.collectAsStateWithLifecycle()
            on = repeat != Player.REPEAT_MODE_OFF
            if (repeat == Player.REPEAT_MODE_ONE) icon = R.drawable.repeat_one
            onClick = { playerConnection.player.toggleRepeatMode() }
        }
        MiniPlayerAction.PLAY_PAUSE -> Unit
    }

    val tint =
        when {
            !enabled && !on -> colors.onSurface.copy(alpha = 0.25f)
            on -> colors.primary
            flat -> MaterialTheme.colorScheme.onSurface
            else -> colors.onSurface.copy(alpha = 0.7f)
        }

    if (flat) {
        IconButton(onClick = onClick, enabled = enabled) {
            if (loading) {
                BlazeDownloadLoader(modifier = Modifier.size(22.dp))
            } else {
                Icon(painterResource(icon), contentDescription = label, tint = tint)
            }
        }
        return
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color =
                        when {
                            on -> colors.primary.copy(alpha = 0.5f)
                            enabled -> colors.outline.copy(alpha = 0.3f)
                            else -> colors.outline.copy(alpha = 0.12f)
                        },
                    shape = CircleShape,
                ).background(if (on) colors.primary.copy(alpha = 0.1f) else Color.Transparent, CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (loading) {
            BlazeDownloadLoader(modifier = Modifier.size(20.dp))
        } else {
            Icon(painterResource(icon), contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/** The player's own sleep-timer sheet, opened from the mini-player. */
@Composable
private fun MiniSleepTimerDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val sleepTimer = playerConnection.service.sleepTimer
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    val stopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val fadeOut by rememberPreference(SleepTimerFadeOutKey, false)
    var timeLeft by remember { mutableLongStateOf(0L) }
    val enabled = sleepTimer?.isActive == true
    LaunchedEffect(enabled) {
        while (enabled && isActive) {
            timeLeft =
                if (sleepTimer?.pauseWhenSongEnd == true) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    (sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                }
            delay(1000L)
        }
    }
    BlazeSleepTimerDialog(
        sleepTimerEnabled = enabled,
        sleepTimerTimeLeft = timeLeft,
        pauseWhenSongEnd = sleepTimer?.pauseWhenSongEnd == true,
        sleepTimerSongsLeft = sleepTimer?.songsLeft ?: 0,
        initialMinutes = sleepTimerDefault,
        onDismiss = onDismiss,
        onStart = { minutes ->
            onDismiss()
            sleepTimer?.start(minute = minutes, stopAfterCurrentSong = stopAfterCurrentSong, fadeOut = fadeOut)
        },
        onStartEndOfSong = {
            onDismiss()
            sleepTimer?.start(minute = -1)
        },
        onStartAfterSongs = { count ->
            onDismiss()
            sleepTimer?.startAfterSongs(count)
        },
        onClear = { sleepTimer?.clear() },
    )
}
