/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.music.constants.HideExplicitKey
import com.blazify.music.constants.HideVideoSongsKey
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.Album
import com.blazify.music.db.entities.Artist
import com.blazify.music.db.entities.LocalItem
import com.blazify.music.db.entities.Playlist
import com.blazify.music.db.entities.Song
import com.blazify.music.extensions.filterExplicit
import com.blazify.music.utils.OfflineSongs
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.searchOfflineSongs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalSearchViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    offlineSongs: OfflineSongs,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    /**
     * True while there is no working connection. Songs are then looked for among those that play
     * without one (downloaded, kept in the song cache, or on the phone) instead of the library,
     * whose songs mostly cannot play.
     */
    val offline = MutableStateFlow(false)

    /**
     * The songs that play without a connection, read once each time the connection is lost; null
     * while they are being read, so "no results" does not flash up first.
     */
    private val savedSongs: StateFlow<List<Song>?> =
        offline
            .flatMapLatest { offline ->
                if (offline) {
                    flow<List<Song>?> {
                        val hideExplicit = context.dataStore.data.map { it[HideExplicitKey] ?: false }.first()
                        emit(offlineSongs.all().map { it.song }.filterExplicit(hideExplicit))
                    }
                } else {
                    flowOf(null)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private fun songs(
        query: String,
        offline: Boolean,
        limit: Int = Int.MAX_VALUE,
    ): Flow<List<Song>> =
        if (offline) {
            savedSongs.filterNotNull().map { searchOfflineSongs(query, it).take(limit) }
        } else {
            database.searchSongs(query, limit)
        }

    private data class Search(
        val query: String,
        val filter: LocalFilter,
        val hideVideoSongs: Boolean,
        val offline: Boolean,
    )

    val result =
        combine(
            query,
            filter,
            context.dataStore.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged(),
            offline,
        ) { query, filter, hideVideoSongs, offline ->
            Search(query, filter, hideVideoSongs, offline)
        }.flatMapLatest { (query, filter, hideVideoSongs, offline) ->
            if (query.isEmpty()) {
                flowOf(LocalSearchResult("", filter, emptyMap()))
            } else {
                when (filter) {
                    LocalFilter.ALL ->
                        combine(
                            // Offline the songs are what can be played, so more of them show at once.
                            songs(query, offline, if (offline) OFFLINE_PREVIEW_SIZE else PREVIEW_SIZE),
                            database.searchAlbums(query, PREVIEW_SIZE),
                            database.searchArtists(query, PREVIEW_SIZE),
                            database.searchPlaylists(query, PREVIEW_SIZE),
                        ) { songs, albums, artists, playlists ->
                            val filteredSongs = if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                            val localSongs = filteredSongs.map { it }
                            val localAlbums = albums.map { it }
                            val localArtists = artists.map { it }
                            val localPlaylists = playlists.map { it }
                            localSongs + localAlbums + localArtists + localPlaylists
                        }

                    LocalFilter.SONG -> songs(query, offline).map { songs ->
                        if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                    }
                    LocalFilter.ALBUM -> database.searchAlbums(query)
                    LocalFilter.ARTIST -> database.searchArtists(query)
                    LocalFilter.PLAYLIST -> database.searchPlaylists(query)
                }.map { list ->
                    LocalSearchResult(
                        query = query,
                        filter = filter,
                        map =
                        list.groupBy {
                            when (it) {
                                is Song -> LocalFilter.SONG
                                is Album -> LocalFilter.ALBUM
                                is Artist -> LocalFilter.ARTIST
                                is Playlist -> LocalFilter.PLAYLIST
                            }
                        },
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    companion object {
        const val PREVIEW_SIZE = 3
        const val OFFLINE_PREVIEW_SIZE = 10
    }
}

enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)
