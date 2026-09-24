/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.playback.queues

import androidx.media3.common.MediaItem
import com.blazify.innertube.YouTube
import com.blazify.innertube.models.WatchEndpoint
import com.blazify.music.extensions.toMediaItem
import com.blazify.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var continuation: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    private class EmptyRadioQueueException : IllegalStateException()

    override suspend fun getInitialStatus(): Queue.Status {
        return withContext(IO) {
            var lastException: Throwable? = null

            if (endpoint.videoId != null && endpoint.playlistId == null) {
                endpoint = WatchEndpoint(
                    videoId = endpoint.videoId,
                    playlistId = "RDAMVM${endpoint.videoId}"
                )
            }

            val isRadioRequest =
                endpoint.playlistId?.startsWith("RDAMVM") == true ||
                (endpoint.videoId != null && endpoint.playlistId == null)

            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    
                    var items = nextResult.items
                    val relEndpoint = nextResult.relatedEndpoint
                    
                    if (isRadioRequest && continuation == null && items.size <= 1) {
                        if (endpoint.playlistId?.startsWith("RDAMVM") == true) {
                            throw EmptyRadioQueueException()
                        } else if (relEndpoint != null) {
                            val relatedPage = YouTube.related(relEndpoint).getOrNull()
                            if (relatedPage != null && relatedPage.songs.isNotEmpty()) {
                                val relatedSongs = relatedPage.songs.filter { it.id != endpoint.videoId }
                                items = items + relatedSongs
                            }
                        }
                    }

                    if (items.isEmpty()) {
                        // An empty answer is as useless as a failed one: the player is left with
                        // nothing and the song simply never starts, with nothing on screen to say
                        // why. Ask for the item itself instead.
                        fallbackStatus()?.let { return@withContext it }
                    }

                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    retryCount = 0
                    return@withContext Queue.Status(
                        title = nextResult.title,
                        items = items.map { it.toMediaItem() },
                        mediaItemIndex = nextResult.currentIndex ?: 0,
                    )
                } catch (e: Exception) {
                    lastException = e
                    if (
                        e is EmptyRadioQueueException &&
                        endpoint.playlistId?.startsWith("RDAMVM") == true &&
                        endpoint.videoId != null
                    ) {
                        endpoint = WatchEndpoint(videoId = endpoint.videoId)
                        // It will loop again and try with just videoId
                    }
                }
            }
            // YouTube has no queue for some items, and a song nobody can queue is still a song
            // somebody asked to hear. Falling back to that one song turns "nothing happens, for
            // ever" into music playing, without a queue behind it.
            fallbackStatus()?.let { return@withContext it }

            throw lastException ?: Exception("Failed to get initial status")
        }
    }

    /**
     * What to play when YouTube has no queue for an item: the item itself.
     *
     * Some videos and mixes come back with no queue at all, or with an empty one. Without this
     * the player is handed nothing, and a song tapped from the home screen sits there loading
     * for ever with no error to act on.
     */
    private suspend fun fallbackStatus(): Queue.Status? {
        val current = endpoint
        val videoId = current.videoId
        val playlistId = current.playlistId
        val songs =
            when {
                videoId != null -> YouTube.queue(videoIds = listOf(videoId)).getOrNull()
                playlistId != null -> YouTube.queue(playlistId = playlistId).getOrNull()
                else -> null
            }
        if (songs.isNullOrEmpty()) return null
        return Queue.Status(
            title = null,
            items = songs.map { it.toMediaItem() },
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        return withContext(IO) {
            var lastException: Throwable? = null

            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    retryCount = 0
                    return@withContext nextResult.items.map { it.toMediaItem() }
                } catch (e: Exception) {
                    lastException = e
                    retryCount++
                    if (retryCount >= maxRetries) {
                        continuation = null // Stop trying to load more
                    }
                }
            }
            throw lastException ?: Exception("Failed to get next page")
        }
    }

    companion object {
        /**
         * Creates a radio queue based on a song.
         * Explicitly requests the RDAMVM playlist to trigger automotive/radio mixing.
         */
        fun radio(song: MediaMetadata): YouTubeQueue {
            return YouTubeQueue(
                WatchEndpoint(
                    videoId = song.id,
                    playlistId = "RDAMVM${song.id}"
                ),
                song
            )
        }
    }
}
