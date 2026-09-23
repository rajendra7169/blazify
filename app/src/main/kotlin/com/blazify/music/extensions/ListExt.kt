/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.extensions

import com.blazify.innertube.models.AlbumItem
import com.blazify.innertube.models.ArtistItem
import com.blazify.innertube.models.SongItem
import com.blazify.innertube.models.YTItem
import com.blazify.music.utils.BlockedArtists
import com.blazify.music.db.entities.Album
import com.blazify.music.db.entities.Playlist
import com.blazify.music.db.entities.Song

fun <T> List<T>.reversed(reversed: Boolean) = if (reversed) asReversed() else this

fun <T> MutableList<T>.move(
    fromIndex: Int,
    toIndex: Int,
): MutableList<T> {
    add(toIndex, removeAt(fromIndex))
    return this
}

fun <T : Any> List<T>.mergeNearbyElements(
    key: (T) -> Any = { it },
    merge: (first: T, second: T) -> T = { first, _ -> first },
): List<T> {
    if (isEmpty()) return emptyList()

    val mergedList = mutableListOf<T>()
    var currentItem = this[0]

    for (i in 1 until size) {
        val nextItem = this[i]
        if (key(currentItem) == key(nextItem)) {
            currentItem = merge(currentItem, nextItem)
        } else {
            mergedList.add(currentItem)
            currentItem = nextItem
        }
    }
    mergedList.add(currentItem)

    return mergedList
}

// Extension function to filter explicit content for local Song entities
fun List<Song>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.song.explicit }
    } else {
        this
    }

// Extension function to filter video songs for local Song entities
fun List<Song>.filterVideoSongs(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.song.isVideo }
    } else {
        this
    }

/** Leaves out saved songs by an artist blocked with "Block this artist". */
fun List<Song>.filterBlockedArtists(blocked: Set<String>) =
    if (blocked.isEmpty()) {
        this
    } else {
        filterNot { song -> song.artists.any { BlockedArtists.isBlocked(blocked, it.id, it.name) } }
    }

/**
 * Leaves out anything by a blocked artist: their songs and albums, and the artist itself
 * where a list holds artists.
 */
@JvmName("filterBlockedArtistsYT")
fun List<YTItem>.filterBlockedArtists(blocked: Set<String>) =
    if (blocked.isEmpty()) {
        this
    } else {
        filterNot { item ->
            when (item) {
                is SongItem -> item.artists.any { BlockedArtists.isBlocked(blocked, it.id, it.name) }
                is AlbumItem -> item.artists?.any { BlockedArtists.isBlocked(blocked, it.id, it.name) } == true
                is ArtistItem -> BlockedArtists.isBlocked(blocked, item.id, item.title)
                else -> false
            }
        }
    }

// Extension function to filter explicit content for local Album entities
fun List<Album>.filterExplicitAlbums(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.album.explicit }
    } else {
        this
    }

// Extension function to filter YouTube Shorts playlist
fun List<Playlist>.filterYoutubeShorts(enabled: Boolean = false) =
    if (enabled) {
        filterNot { it.playlist.browseId?.startsWith("SS") == true }
    } else {
        this
    }
