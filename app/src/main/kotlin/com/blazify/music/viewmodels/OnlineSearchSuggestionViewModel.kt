/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.innertube.YouTube
import com.blazify.innertube.models.AlbumItem
import com.blazify.innertube.models.ArtistItem
import com.blazify.innertube.models.PlaylistItem
import com.blazify.innertube.models.SongItem
import com.blazify.innertube.models.WatchEndpoint
import com.blazify.innertube.models.YTItem
import com.blazify.innertube.models.filterExplicit
import com.blazify.innertube.models.filterVideoSongs
import com.blazify.innertube.utils.YouTubeUrlParser
import com.blazify.music.constants.HideExplicitKey
import com.blazify.music.constants.HideVideoSongsKey
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.SearchHistory
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        database: MusicDatabase,
    ) : ViewModel() {
        val query = MutableStateFlow("")
        private val _viewState = MutableStateFlow(SearchSuggestionViewState())
        val viewState = _viewState.asStateFlow()

        init {
            viewModelScope.launch {
                query
                    .flatMapLatest { query ->
                        if (query.isEmpty()) {
                            database.searchHistory().map { history ->
                                SearchSuggestionViewState(
                                    history = history,
                                )
                            }
                        } else {
                            // Check if query is a YouTube URL
                            val parsedUrl = YouTubeUrlParser.parse(query)
                            if (parsedUrl != null) {
                                // Fetch content from YouTube URL
                                val parsedItem = fetchParsedUrlItem(parsedUrl)
                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions = emptyList(),
                                            items = parsedItem?.let { listOf(it) } ?: emptyList(),
                                            parsedUrlItem = parsedItem,
                                            isUrlQuery = true,
                                        )
                                    }
                            } else {
                                val result = YouTube.searchSuggestions(query).getOrNull()
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions =
                                                result
                                                    ?.queries
                                                    ?.filter { suggestionQuery ->
                                                        history.none { it.query == suggestionQuery }
                                                    }.orEmpty(),
                                            items =
                                                result
                                                    ?.recommendedItems
                                                    ?.distinctBy { it.id }
                                                    ?.filterExplicit(hideExplicit)
                                                    ?.filterVideoSongs(hideVideoSongs)
                                                    .orEmpty(),
                                        )
                                    }
                            }
                        }
                    }.collect {
                        _viewState.value = it
                    }
            }
        }

        private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? =
            when (parsedUrl) {
                is YouTubeUrlParser.ParsedUrl.Video -> {
                    // Use next() to get the song details from a video ID
                    YouTube
                        .next(WatchEndpoint(videoId = parsedUrl.id))
                        .getOrNull()
                        ?.items
                        ?.firstOrNull()
                }

                is YouTubeUrlParser.ParsedUrl.Playlist -> {
                    // Fetch playlist details
                    YouTube
                        .playlist(parsedUrl.id)
                        .getOrNull()
                        ?.playlist
                }

                is YouTubeUrlParser.ParsedUrl.Album -> {
                    // For albums, we need to get the browseId from the playlist
                    // First, try to get the album page
                    val albumResult = YouTube.album("MPREb_${parsedUrl.id}")
                    if (albumResult.isSuccess) {
                        albumResult.getOrNull()?.album
                    } else {
                        // If that fails, treat it as a playlist
                        YouTube
                            .playlist(parsedUrl.id)
                            .getOrNull()
                            ?.playlist
                    }
                }

                is YouTubeUrlParser.ParsedUrl.Artist -> {
                    // Fetch artist details
                    if (parsedUrl.id.startsWith("MPRE")) {
                        // It's a browse ID
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    } else {
                        // It's a channel ID, we need to find the browse ID
                        // For now, try using the channel ID as browse ID
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    }
                }
            }
    }

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val parsedUrlItem: YTItem? = null,
    val isUrlQuery: Boolean = false,
)
