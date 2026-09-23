/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.playback.queues

import androidx.media3.common.MediaItem
import com.blazify.music.extensions.metadata
import com.blazify.music.models.MediaMetadata
import com.blazify.music.utils.BlockedArtists

interface Queue {
    val preloadItem: MediaMetadata?

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) =
            if (enabled) {
                copy(
                    items = items.filterExplicit(),
                )
            } else {
                this
            }

        fun filterVideoSongs(disableVideos: Boolean = false) =
            if (disableVideos) {
                copy(
                    items = items.filterVideoSongs(true),
                )
            } else {
                this
            }

        /**
         * Leaves out songs the listener hid with "Don't play this song". The song at
         * [mediaItemIndex] stays even if it is hidden, because that is the one they
         * chose to play, and the index is moved so it still points at it.
         */
        /**
         * Leaves out songs by a blocked artist, keeping the song at [mediaItemIndex] as
         * [filterHidden] does: that is the one they asked to hear.
         */
        fun filterBlockedArtists(blocked: Set<String>): Status {
            if (blocked.isEmpty() || items.isEmpty()) return this
            val start = mediaItemIndex.coerceIn(0, items.lastIndex)
            val kept = ArrayList<MediaItem>(items.size)
            var newIndex = 0
            items.forEachIndexed { i, item ->
                if (i == start) newIndex = kept.size
                if (i == start || !BlockedArtists.blocksAnyOf(blocked, item.metadata?.artists.orEmpty())) kept.add(item)
            }
            return copy(items = kept, mediaItemIndex = newIndex)
        }

        fun filterHidden(hiddenIds: Set<String>): Status {
            if (hiddenIds.isEmpty() || items.isEmpty()) return this
            val start = mediaItemIndex.coerceIn(0, items.lastIndex)
            val kept = ArrayList<MediaItem>(items.size)
            var newIndex = 0
            items.forEachIndexed { i, item ->
                if (i == start) newIndex = kept.size
                if (i == start || item.mediaId !in hiddenIds) kept.add(item)
            }
            return copy(items = kept, mediaItemIndex = newIndex)
        }
    }
}

/** Leaves out songs hidden with "Don't play this song", for batches nobody picked by hand. */
fun List<MediaItem>.filterHidden(hiddenIds: Set<String>) =
    if (hiddenIds.isEmpty()) this else filterNot { it.mediaId in hiddenIds }

/** Leaves out songs by an artist blocked with "Block this artist". */
fun List<MediaItem>.filterBlockedArtists(blocked: Set<String>) =
    if (blocked.isEmpty()) {
        this
    } else {
        filterNot { item -> BlockedArtists.blocksAnyOf(blocked, item.metadata?.artists.orEmpty()) }
    }

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it.metadata?.isVideoSong == true }
    } else {
        this
    }
