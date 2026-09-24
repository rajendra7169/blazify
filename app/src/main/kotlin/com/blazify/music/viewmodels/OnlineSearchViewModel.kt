/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.innertube.YouTube
import com.blazify.innertube.models.filterExplicit
import com.blazify.innertube.models.filterVideoSongs
import com.blazify.innertube.models.filterYoutubeShorts
import com.blazify.innertube.models.EpisodeItem
import com.blazify.innertube.models.SongItem
import com.blazify.innertube.models.YTItem
import com.blazify.innertube.pages.SearchSummaryPage
import com.blazify.music.constants.HideExplicitKey
import com.blazify.music.constants.BlockedArtistsKey
import com.blazify.music.constants.HideVideoSongsKey
import com.blazify.music.constants.HideYoutubeShortsKey
import com.blazify.music.models.ItemsPage
import com.blazify.music.extensions.filterBlockedArtists
import com.blazify.music.utils.SearchRoutes
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.get
import com.blazify.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = SearchRoutes.decodeQuery(savedStateHandle.get<String>("query").orEmpty())
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    private suspend fun loadSummaryPage() {
        if (summaryPage != null) return
        YouTube
            .searchSummary(query)
            .onSuccess { page ->
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                val blocked = context.dataStore.get(BlockedArtistsKey, emptySet())
                val shown =
                    page.songOnTop()
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                        .withoutBlockedArtists(blocked)
                summaryPage = shown
                // The results show as soon as YouTube answers. Artist names it left
                // unlinked are looked up afterwards, all together, and become tappable
                // when they come back: waiting for them first took 2.5 to 3 seconds more.
                viewModelScope.launch {
                    val items = shown.summaries.flatMap { it.items }
                    val resolved = YouTube.resolveArtistIds(items)
                    if (resolved === items || summaryPage !== shown) return@launch
                    var from = 0
                    summaryPage =
                        shown.copy(
                            summaries = shown.summaries.map { summary ->
                                summary.copy(items = resolved.subList(from, from + summary.items.size))
                                    .also { from += summary.items.size }
                            },
                        )
                }
            }.onFailure {
                reportException(it)
            }
    }

    /** Links the unlinked artist names in [page] once they are found, if it is still the page shown. */
    private fun resolveArtistsLater(filterValue: String, page: ItemsPage) {
        viewModelScope.launch {
            val resolved = YouTube.resolveArtistIds(page.items)
            if (resolved !== page.items && viewStateMap[filterValue] === page) {
                viewStateMap[filterValue] = page.copy(items = resolved)
            }
        }
    }

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    loadSummaryPage()
                } else if (filter == YouTube.SearchFilter.FILTER_EPISODE) {
                    // The FILTER_EPISODE API returns episodes in a format that differs from the
                    // summary search: playlistItemData is absent and the subtitle structure is
                    // different, making reliable isEpisode detection fail for many items.
                    // Reuse the "Episodes" section from the summary page instead — it is already
                    // parsed correctly by fromMusicResponsiveListItemRenderer and guaranteed to
                    // show the same results as the episodes section in the "All" filter.
                    if (viewStateMap[filter.value] == null) {
                        loadSummaryPage()
                        summaryPage?.let { page ->
                            val episodes = page.summaries
                                .firstOrNull { it.title == "Episodes" }
                                ?.items
                                .orEmpty()
                            viewStateMap[filter.value] = ItemsPage(episodes, null)
                        }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                                val page =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(hideExplicit)
                                            .filterVideoSongs(hideVideoSongs)
                                            .filterYoutubeShorts(hideYoutubeShorts)
                                            .filterBlockedArtists(context.dataStore.get(BlockedArtistsKey, emptySet())),
                                        result.continuation,
                                    )
                                viewStateMap[filter.value] = page
                                resolveArtistsLater(filter.value, page)
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val currentFilter = filter.value
        val filterValue = currentFilter?.value ?: return
        viewModelScope.launch {
            val viewState = viewStateMap[filterValue] ?: return@launch
            val continuation = viewState.continuation ?: return@launch
            val searchResult =
                YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val newItems = searchResult.items
                .filterExplicit(hideExplicit)
                .filterVideoSongs(hideVideoSongs)
                .filterYoutubeShorts(hideYoutubeShorts)
                .filterBlockedArtists(context.dataStore.get(BlockedArtistsKey, emptySet()))
            val page = ItemsPage(
                (viewState.items + newItems).distinctBy { it.id },
                searchResult.continuation
            )
            viewStateMap[filterValue] = page
            resolveArtistsLater(filterValue, page)
        }
    }
}

/** The same page without anything by a blocked artist, and without a section left empty. */
private fun SearchSummaryPage.withoutBlockedArtists(blocked: Set<String>): SearchSummaryPage =
    if (blocked.isEmpty()) {
        this
    } else {
        copy(
            summaries = summaries.mapNotNull { summary ->
                summary.copy(items = summary.items.filterBlockedArtists(blocked)).takeIf { it.items.isNotEmpty() }
            },
        )
    }

/**
 * In a music app the top result should be a song. When YouTube ranks a video or a podcast
 * episode first ("Shape of You" gives the official video; "Tum Hi Ho" gave a podcast
 * episode), the first song YouTube found moves to the top and its pick stays right below.
 * An artist, album or playlist on top is left alone: that is usually what was searched for.
 */
private fun SearchSummaryPage.songOnTop(): SearchSummaryPage {
    fun YTItem.isSong() = this is SongItem && !isVideoSong && !isEpisode
    val top = summaries.firstOrNull { it.isTopResult } ?: return this
    val lead = top.items.firstOrNull() ?: return this
    val leadIsVideoOrEpisode = (lead is SongItem && (lead.isVideoSong || lead.isEpisode)) || lead is EpisodeItem
    if (!leadIsVideoOrEpisode) return this
    val song =
        summaries.firstOrNull { !it.isTopResult && it.items.firstOrNull()?.isSong() == true }
            ?.items?.first()
            ?: return this
    val topItems = listOf(song) + top.items.filterNot { it.id == song.id }
    return copy(summaries = summaries.map { if (it === top) it.copy(items = topItems) else it })
}
