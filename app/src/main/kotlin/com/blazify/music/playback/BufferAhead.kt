/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.playback

import androidx.annotation.OptIn
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator

/**
 * How far ahead a song is fetched, so a moment without a connection is a moment nobody hears.
 *
 * A connection that comes and goes is the ordinary case, not the exception: a lift, a tunnel, a
 * bus, a house with one good room. Holding fifty seconds of audio meant a drop longer than the
 * walk between two rooms stopped the music, and it came back only after the reconnect and the
 * retry. Holding minutes instead means most drops pass unheard, and the song simply plays on.
 *
 * On mobile data it goes back to holding little: everything fetched ahead of a song somebody
 * skips is data they paid for and never heard.
 *
 * Every decision is the player's own load control's; only how much to fetch is this one's. The
 * methods below are the ones that control implements, passed straight through. Handing the work
 * over with Kotlin's `by` instead looked tidier and killed the app at launch: the interface also
 * carries older versions of these methods which throw where they are not implemented, and a
 * delegate passes those on faithfully.
 */
@OptIn(UnstableApi::class)
class BufferAhead(
    private val metered: () -> Boolean,
) : LoadControl {
    private val inner =
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(MIN_MS, WIFI_MAX_MS, START_MS, RESUME_MS)
            .setTargetBufferBytes(MAX_BYTES)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        if (metered() && parameters.bufferedDurationUs >= MOBILE_MAX_US) return false
        return inner.shouldContinueLoading(parameters)
    }

    override fun onPrepared(playerId: PlayerId) = inner.onPrepared(playerId)

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection>,
    ) = inner.onTracksSelected(parameters, trackGroups, trackSelections)

    override fun onStopped(playerId: PlayerId) = inner.onStopped(playerId)

    override fun onReleased(playerId: PlayerId) = inner.onReleased(playerId)

    override fun getAllocator(playerId: PlayerId): Allocator = inner.getAllocator(playerId)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long = inner.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean =
        inner.retainBackBufferFromKeyframe(playerId)

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean =
        inner.shouldStartPlayback(parameters)

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long,
    ): Boolean = inner.shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)

    companion object {
        /** On a connection that costs nothing: minutes of the song, ready to play through a drop. */
        private const val WIFI_MAX_MS = 240_000

        /** Enough to start quickly and to ride out a short stumble. */
        private const val MIN_MS = 50_000

        /** What is held on mobile data, where every megabyte fetched ahead may be wasted. */
        private const val MOBILE_MAX_US = 50_000_000L

        /** Start playing once this much is ready — a touch under media3's second. */
        private const val START_MS = 750

        /** After a stall, wait for this much before going on, so it does not stall again at once. */
        private const val RESUME_MS = 2_000

        /** A ceiling in bytes as well as seconds: minutes of a high-quality song, and no more. */
        private const val MAX_BYTES = 16 * 1024 * 1024

        fun create(metered: () -> Boolean): LoadControl = BufferAhead(metered)
    }
}
