/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import com.blazify.innertube.YouTube
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.PlaylistEntity
import com.blazify.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Keeps a playlist somebody shared. The link carries only ids, so each song's name, artist
 * and artwork are asked of YouTube here, and what is found becomes a playlist of one's own.
 */
object SharedPlaylistImport {
    /** How many ids one lookup takes; YouTube refuses more. */
    private const val AT_ONCE = 50

    data class Outcome(val playlistId: String, val name: String, val saved: Int, val total: Int)

    suspend fun save(shared: PlaylistLink.Shared, database: MusicDatabase): Result<Outcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                val songs =
                    shared.songIds.chunked(AT_ONCE).flatMap { chunk ->
                        YouTube.queue(videoIds = chunk).getOrElse {
                            Timber.tag("SharedPlaylist").w(it, "could not look up %d songs", chunk.size)
                            emptyList()
                        }
                    }
                if (songs.isEmpty()) throw IllegalStateException("none of the shared songs could be found")

                val entity =
                    PlaylistEntity(
                        name = shared.name,
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true,
                    )
                // Written straight through rather than through database.query{}, which hands the
                // work to a background executor and returns before any of it has happened.
                database.insert(entity)
                songs.forEach { database.insert(it.toMediaMetadata()) }
                val stored = database.playlistBlocking(entity.id) ?: throw IllegalStateException("playlist vanished")
                // In the order they were shared, not the order they came back in.
                val order = shared.songIds.withIndex().associate { (i, id) -> id to i }
                database.addSongToPlaylist(stored, songs.sortedBy { order[it.id] ?: Int.MAX_VALUE }.map { it.id })

                Timber.tag("SharedPlaylist").i("%s: kept %d of %d", shared.name, songs.size, shared.songIds.size)
                Outcome(entity.id, shared.name, songs.size, shared.songIds.size)
            }.onFailure { Timber.tag("SharedPlaylist").w(it, "could not keep the shared playlist") }
        }
}
