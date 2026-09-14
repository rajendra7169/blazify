/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.quicksettings

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.blazify.music.R
import com.blazify.music.playback.MusicService
import com.google.common.util.concurrent.ListenableFuture

/**
 * Quick Settings tile that plays or pauses Blazify. It goes through the media
 * session, so with nothing loaded a tap picks up the last queue again. While the
 * panel is open it follows the player, so the icon and song name stay current.
 */
class PlaybackTileService : TileService() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val listener =
        object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updateTile()
            }
        }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        connect()
    }

    override fun onStopListening() {
        release()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val current = controller
        if (current != null) {
            current.togglePlayback()
        } else {
            connect { it.togglePlayback() }
        }
    }

    private fun MediaController.togglePlayback() {
        if (isPlaying) {
            pause()
        } else {
            if (playbackState == Player.STATE_IDLE) prepare()
            play()
        }
        updateTile()
    }

    private fun connect(then: ((MediaController) -> Unit)? = null) {
        val existing = controllerFuture
        if (existing != null) {
            if (then != null) {
                existing.addListener({ controller?.let(then) }, ContextCompat.getMainExecutor(this))
            }
            return
        }
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = connected
                connected.addListener(listener)
                updateTile()
                then?.invoke(connected)
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val current = controller
        val playing = current?.isPlaying == true
        tile.state = if (playing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, if (playing) R.drawable.pause else R.drawable.play)
        tile.label = getString(R.string.qs_tile_playback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = current?.mediaMetadata?.title?.toString()
                ?: getString(R.string.qs_tile_nothing_playing)
        }
        tile.updateTile()
    }
}
