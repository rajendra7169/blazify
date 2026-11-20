/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.Song
import com.blazify.music.di.DownloadCache
import com.blazify.music.di.PlayerCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Where a song that plays without a connection is kept. */
enum class OfflineSource {
    /** Kept whole in the song cache after being played. */
    CACHED,

    /** Downloaded on purpose. */
    DOWNLOADED,

    /** One of the listener's own files on the phone. */
    DEVICE,
}

/** Which kinds of saved songs Home plays when there is no connection. All of them unless switched off. */
data class OfflineKinds(
    val cached: Boolean = true,
    val downloaded: Boolean = true,
    val device: Boolean = true,
) {
    /** True when a song kept in [sources] is one of the kinds switched on. */
    fun allows(sources: Set<OfflineSource>): Boolean =
        sources.any {
            when (it) {
                OfflineSource.CACHED -> cached
                OfflineSource.DOWNLOADED -> downloaded
                OfflineSource.DEVICE -> device
            }
        }
}

/** A song that plays without a connection, and where it is kept. */
data class SavedSong(
    val song: Song,
    val sources: Set<OfflineSource>,
)

/**
 * Every song that plays without a connection: downloaded, kept whole in the song cache, or one of
 * the listener's own files, each with where it is kept. Read when asked rather than kept up to
 * date, since most days nobody asks.
 */
@Singleton
class OfflineSongs
@Inject
constructor(
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: Cache,
    @DownloadCache private val downloadCache: Cache,
) {
    suspend fun all(): List<SavedSong> =
        withContext(Dispatchers.IO) {
            val ids = (playerCache.keys + downloadCache.keys).filterNot { LocalMusic.isLocal(it) }
            // A few hundred at a time: Android 8's database takes at most 999 values in one query.
            val saved =
                ids.chunked(500).flatMap { database.getSongsByIds(it) }.mapNotNull { song ->
                    val id = song.id
                    val length =
                        song.format?.contentLength
                            ?: ContentMetadata.getContentLength(playerCache.getContentMetadata(id)).takeIf { it > 0 }
                            ?: ContentMetadata.getContentLength(downloadCache.getContentMetadata(id)).takeIf { it > 0 }
                            ?: return@mapNotNull null
                    val sources =
                        buildSet {
                            if (downloadCache.isCached(id, 0, length)) add(OfflineSource.DOWNLOADED)
                            if (playerCache.isCached(id, 0, length)) add(OfflineSource.CACHED)
                        }
                    if (sources.isEmpty()) null else SavedSong(song, sources)
                }
            val local = database.localSongsBlocking().map { SavedSong(it, setOf(OfflineSource.DEVICE)) }
            (saved + local).distinctBy { it.song.id }
        }
}

/** Lower case, without accents, so "Beyoncé" is found by typing "beyonce". */
private fun searchable(text: String): String =
    Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)

/**
 * The items whose title, artists or album hold every word of [query], best first: titles that
 * start with what was typed, then titles that hold it, then the rest in their own order.
 */
fun <T> searchOffline(
    query: String,
    items: List<T>,
    title: (T) -> String,
    others: (T) -> List<String?>,
): List<T> {
    val typed = searchable(query).trim()
    val words = typed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return items
        .mapNotNull { item ->
            val name = searchable(title(item))
            val all = (listOf(name) + others(item).filterNotNull().map(::searchable)).joinToString(" ")
            if (words.all { it in all }) {
                val rank =
                    when {
                        name.startsWith(typed) -> 0
                        typed in name -> 1
                        else -> 2
                    }
                rank to item
            } else {
                null
            }
        }.sortedBy { it.first }
        .map { it.second }
}

/** [searchOffline] for songs: by title, artists and album. */
fun searchOfflineSongs(
    query: String,
    songs: List<Song>,
): List<Song> =
    searchOffline(
        query,
        songs,
        title = { it.song.title },
        others = { song -> song.orderedArtists.map { it.name } + (song.album?.title ?: song.song.albumName) },
    )
