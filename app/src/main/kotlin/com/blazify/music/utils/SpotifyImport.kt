package com.blazify.music.utils

import com.blazify.innertube.YouTube
import com.blazify.innertube.models.SongItem
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.PlaylistEntity
import com.blazify.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Rebuilds a Spotify playlist here.
 *
 * Spotify's tracks are names, not addresses: nothing in one library points at
 * anything in the other. So each one is searched for by title and artist and
 * accepted only when the length agrees too — the wrong song under the right
 * name is worse than an honest gap, and a gap is reported rather than hidden.
 */
object SpotifyImport {
    /** How far a result's length may differ from Spotify's before it is somebody else's song. */
    private const val TOLERANCE_SECONDS = 8

    /** Searches in flight at once: enough to be quick, few enough to stay polite. */
    private const val AT_ONCE = 4

    data class Progress(val done: Int, val total: Int)

    data class Outcome(
        val playlistId: String,
        val name: String,
        val matched: Int,
        val total: Int,
        /** Titles nothing was found for, in the order they appear on Spotify. */
        val missing: List<String>,
        val mayHaveMore: Boolean,
    )

    suspend fun import(
        link: String,
        database: MusicDatabase,
        onProgress: (Progress) -> Unit = {},
    ): Result<Outcome> {
        val playlist = SpotifyPlaylist.read(link).getOrElse { return Result.failure(it) }
        val found = arrayOfNulls<SongItem>(playlist.tracks.size)
        var done = 0

        coroutineScope {
            val limit = Semaphore(AT_ONCE)
            playlist.tracks
                .mapIndexed { index, track ->
                    async(Dispatchers.IO) {
                        limit.withPermit {
                            found[index] = search(track)
                            onProgress(Progress(++done, playlist.tracks.size))
                        }
                    }
                }.forEach { it.await() }
        }

        val entity =
            PlaylistEntity(
                name = playlist.name,
                bookmarkedAt = LocalDateTime.now(),
                isEditable = true,
            )

        return withContext(Dispatchers.IO) {
            runCatching {
                // Written straight through rather than through database.query{}, which hands the
                // work to a background executor and returns before any of it has happened — the
                // playlist is read back on the next line.
                database.insert(entity)
                found.filterNotNull().forEach { database.insert(it.toMediaMetadata()) }
                val stored = database.playlistBlocking(entity.id) ?: throw IllegalStateException("playlist vanished")
                database.addSongToPlaylist(stored, found.filterNotNull().map { it.id })

                val missing =
                    playlist.tracks
                        .filterIndexed { index, _ -> found[index] == null }
                        .map { it.title }
                Timber
                    .tag("SpotifyImport")
                    .i("${playlist.name}: ${playlist.tracks.size - missing.size} of ${playlist.tracks.size} found")

                Outcome(
                    playlistId = entity.id,
                    name = playlist.name,
                    matched = playlist.tracks.size - missing.size,
                    total = playlist.tracks.size,
                    missing = missing,
                    mayHaveMore = playlist.mayHaveMore,
                )
            }.onFailure { Timber.tag("SpotifyImport").w(it, "import failed") }
        }
    }

    private suspend fun search(track: SpotifyPlaylist.Track): SongItem? {
        val query = listOf(track.title, track.artists).filter { it.isNotBlank() }.joinToString(" ")
        val results =
            runCatching {
                YouTube
                    .search(query, YouTube.SearchFilter.FILTER_SONG)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    .orEmpty()
            }.getOrDefault(emptyList())
        return pickMatch(results, track)
    }

    /**
     * The closest result by length, or nothing.
     *
     * A track Spotify has no length for (it happens) is matched on names
     * alone, since there is nothing to compare.
     */
    internal fun pickMatch(results: List<SongItem>, track: SpotifyPlaylist.Track): SongItem? {
        if (results.isEmpty()) return null
        if (track.durationSeconds <= 0) return results.first()
        return results
            .mapNotNull { item -> item.duration?.let { item to abs(it - track.durationSeconds) } }
            .filter { (_, gap) -> gap <= TOLERANCE_SECONDS }
            .minByOrNull { (_, gap) -> gap }
            ?.first
    }
}
