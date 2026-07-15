/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Backs the "Yours" tab: personal home rails (recently played, recommended,
 * playlists, favorite artists) sourced from the local library, plus the
 * logged-in account's YouTube playlists and Mood & Genres from the network.
 */

package com.blazify.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.innertube.YouTube
import com.blazify.innertube.models.PlaylistItem
import com.blazify.innertube.pages.MoodAndGenres
import com.blazify.innertube.utils.completed
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.Artist
import com.blazify.music.db.entities.Playlist
import com.blazify.music.db.entities.Song
import com.blazify.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoursViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
) : ViewModel() {

    // Recently played: distinct songs from the play-event log, newest first.
    val recentlyPlayed = database.events()
        .map { events -> events.map { it.song }.distinctBy { it.id }.take(15) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Recommended: locally-derived quick picks.
    val recommended = database.quickPicks()
        .map { it.take(15) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Liked songs (drives the Favorites card thumbnail + count).
    val likedSongs: kotlinx.coroutines.flow.StateFlow<List<Song>> =
        database.likedSongsByCreateDateAsc()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Local playlists, most-recently-updated first.
    val playlists: kotlinx.coroutines.flow.StateFlow<List<Playlist>> =
        database.playlistsByUpdatedDateAsc()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Followed/subscribed artists.
    val favoriteArtists: kotlinx.coroutines.flow.StateFlow<List<Artist>> =
        database.artistsBookmarkedByCreateDateAsc()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // The signed-in account's YouTube Music playlists (null until loaded).
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)

    // Mood & Genres browse tiles (null until loaded).
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (YouTube.cookie != null) {
                YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
                    accountPlaylists.value = page.items
                        .filterIsInstance<PlaylistItem>()
                        .filterNot { it.id == "SE" }
                }.onFailure { reportException(it) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.moodAndGenres().onSuccess {
                moodAndGenres.value = it
            }.onFailure { reportException(it) }
        }
    }
}
