/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.search

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.offset
import com.blazify.music.utils.BrowseArt
import android.net.Uri
import androidx.compose.runtime.remember
import com.blazify.innertube.pages.MoodAndGenres
import com.blazify.music.viewmodels.BROWSE_GENRES_MIN
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.db.entities.SearchHistory
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.blazify.music.LocalNavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.innertube.models.AlbumItem
import com.blazify.innertube.models.ArtistItem
import com.blazify.innertube.models.EpisodeItem
import com.blazify.innertube.models.PlaylistItem
import com.blazify.innertube.models.PodcastItem
import com.blazify.innertube.models.SongItem
import com.blazify.music.LocalDatabase
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.AutoRadioQueueKey
import com.blazify.music.constants.SuggestionItemHeight
import com.blazify.music.extensions.toMediaItem
import com.blazify.music.models.toMediaMetadata
import com.blazify.music.playback.queues.ListQueue
import com.blazify.music.playback.queues.YouTubeQueue
import com.blazify.music.ui.component.LocalMenuState
import com.blazify.music.ui.component.YouTubeListItem
import com.blazify.music.ui.menu.YouTubeAlbumMenu
import com.blazify.music.ui.menu.YouTubeArtistMenu
import com.blazify.music.ui.menu.YouTubePlaylistMenu
import com.blazify.music.ui.menu.YouTubeSongMenu
import com.blazify.music.utils.rememberPreference
import com.blazify.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val moods by viewModel.moods.collectAsStateWithLifecycle()
    // The moods, then the listener's own genres and a couple more — the
    // ones their history points to, not the alphabet's first few. The
    // scores change as they listen and as the pages are read, so the
    // order is worked out here, from the same figures the model keeps.
    val genreScores by viewModel.genreScores.collectAsStateWithLifecycle()
    val genreTitles by viewModel.genreTitles.collectAsStateWithLifecycle()
    // Laid out as on the iPhone: every mood under Browse, then the genres under
    // their own heading — the listener's own first, with a couple beyond them,
    // and the rest behind "All genres" rather than a page of languages nobody
    // here has played.
    val moodTiles =
        remember(moods, genreTitles) { moods.filter { it.title !in genreTitles } }
    val genreTiles =
        remember(moods, genreScores, genreTitles) {
            moods.withIndex()
                .filter { it.value.title in genreTitles }
                .sortedWith(
                    compareByDescending<IndexedValue<MoodAndGenres.Item>> { genreScores[it.value.title] ?: 0.0 }
                        .thenBy { it.index },
                ).map { it.value }
        }
    var allGenres by rememberSaveable { mutableStateOf(false) }
    val ownGenres = genreTiles.count { (genreScores[it.title] ?: 0.0) > 0 }
    val genresShown =
        if (allGenres) genreTiles.size else maxOf(BROWSE_GENRES_MIN, minOf(genreTiles.size, ownGenres + 2))
    val browseArt by viewModel.browseArt.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    val autoRadioQueue by rememberPreference(AutoRadioQueueKey, defaultValue = true)

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        snapshotFlow { query }.debounce(300L).collectLatest {
            viewModel.query.value = it
        }
    }

    LazyColumn(
        state = lazyListState,
        // Reserve space for BOTH the now-playing mini-player (via the player-aware
        // insets) and the keyboard (IME) — not just the nav bar. Otherwise, when a
        // song is playing, the lower items sit BEHIND the mini-player and the list
        // can't be scrolled to reach them (it's laid out at full height, so nothing
        // overflows the viewport).
        contentPadding = LocalPlayerAwareWindowInsets.current
            .union(WindowInsets.ime)
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Show parsed URL item at the top if present
        if (viewState.isUrlQuery && viewState.parsedUrlItem != null) {
            item(key = "parsed_url_header") {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.parsed_from_link),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            item(key = "parsed_url_item") {
                val item = viewState.parsedUrlItem!!
                YouTubeListItem(
                    item = item,
                    isActive =
                        when (item) {
                            is SongItem -> mediaMetadata?.id == item.id
                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                            is EpisodeItem -> mediaMetadata?.id == item.id
                            else -> false
                        },
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> {
                                            YouTubeSongMenu(
                                                song = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is AlbumItem -> {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is ArtistItem -> {
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is PlaylistItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is PodcastItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item.asPlaylistItem(),
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is EpisodeItem -> {
                                            YouTubeSongMenu(
                                                song = item.asSongItem(),
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .combinedClickable(
                                onClick = {
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    if (autoRadioQueue) {
                                                        YouTubeQueue.radio(item.toMediaMetadata())
                                                    } else {
                                                        ListQueue(
                                                            title = item.title,
                                                            items = listOf(item.toMediaItem())
                                                        )
                                                    }
                                                )
                                                onDismiss()
                                            }
                                        }

                                        is AlbumItem -> {
                                            navController.navigate("album/${item.id}")
                                            onDismiss()
                                        }

                                        is ArtistItem -> {
                                            navController.navigate("artist/${item.id}")
                                            onDismiss()
                                        }

                                        is PlaylistItem -> {
                                            navController.navigate("online_playlist/${item.id}")
                                            onDismiss()
                                        }

                                        is PodcastItem -> {
                                            navController.navigate("online_podcast/${item.id}")
                                            onDismiss()
                                        }

                                        is EpisodeItem -> {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue.radio(item.toMediaMetadata()),
                                                )
                                                onDismiss()
                                            }
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> {
                                                YouTubeSongMenu(
                                                    song = item,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is AlbumItem -> {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is ArtistItem -> {
                                                YouTubeArtistMenu(
                                                    artist = item,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is PlaylistItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is PodcastItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item.asPlaylistItem(),
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is EpisodeItem -> {
                                                YouTubeSongMenu(
                                                    song = item.asSongItem(),
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }
                                        }
                                    }
                                },
                            ).background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                            .animateItem(),
                )
            }

            item(key = "parsed_url_divider") {
                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(vertical = 8.dp)
                            .animateItem(),
                )
            }
        }

        // Blazify: when the box is empty, show recent searches as a horizontal
        // "Recent Searches" chip rail (ported from the Flutter app). While typing,
        // fall back to the standard vertical history rows.
        if (query.isEmpty()) {
            if (viewState.history.isNotEmpty()) {
                item(key = "recent_searches_rail") {
                    RecentSearchesRail(
                        history = viewState.history,
                        onSearch = {
                            onSearch(it)
                            onDismiss()
                        },
                        onDelete = { history ->
                            database.query {
                                deleteSearchHistorySameAs(history.query)
                            }
                        },
                        onClear = {
                            database.query {
                                viewState.history.forEach { delete(it) }
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            // An empty search box used to be an empty screen. This gives it
            // somewhere to go: Charts and New releases, which nothing else in the
            // app opened, then the moods and genres Explore lists, two to a row,
            // each in its own colour.
            item(key = "browse_heading") {
                Text(
                    text = stringResource(R.string.browse),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .animateItem()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                )
            }
            item(key = "browse_explore") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    BrowseTile(
                        title = stringResource(R.string.charts),
                        stripeColor = 0xFFFFA726,
                        art = browseArt[BrowseArt.CHARTS].orEmpty(),
                        onClick = {
                            onDismiss()
                            navController.navigate("charts_screen")
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    BrowseTile(
                        title = stringResource(R.string.new_release_albums),
                        stripeColor = 0xFFFF7043,
                        art = browseArt[BrowseArt.NEW_RELEASES].orEmpty(),
                        onClick = {
                            onDismiss()
                            navController.navigate("new_release")
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
            items(
                items = moodTiles.chunked(2),
                key = { row -> "browse_${row.first().title}" },
            ) { row ->
                BrowseTileRow(
                    row = row,
                    browseArt = browseArt,
                    onOpen = { mood ->
                        onDismiss()
                        navController.navigate(
                            "youtube_browse/${mood.endpoint.browseId}?params=${mood.endpoint.params}&title=${Uri.encode(mood.title)}",
                        )
                    },
                    modifier = Modifier.animateItem(),
                )
            }
            if (genreTiles.isNotEmpty()) {
                item(key = "genres_heading") {
                    Text(
                        text = stringResource(R.string.genres),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .animateItem()
                                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp),
                    )
                }
                items(
                    items = genreTiles.take(genresShown).chunked(2),
                    key = { row -> "genre_${row.first().title}" },
                ) { row ->
                    BrowseTileRow(
                        row = row,
                        browseArt = browseArt,
                        onOpen = { mood ->
                            onDismiss()
                            navController.navigate(
                                "youtube_browse/${mood.endpoint.browseId}?params=${mood.endpoint.params}&title=${Uri.encode(mood.title)}",
                            )
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (genreTiles.size > BROWSE_GENRES_MIN) {
                    item(key = "genres_toggle") {
                        Text(
                            text = stringResource(if (allGenres) R.string.fewer_genres else R.string.all_genres),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clickable { allGenres = !allGenres }
                                    .padding(vertical = 14.dp),
                        )
                    }
                }
            }
        } else {
            items(viewState.history, key = { "history_${it.query}" }) { history ->
                SuggestionItem(
                    query = history.query,
                    online = false,
                    onClick = {
                        onSearch(history.query)
                        onDismiss()
                    },
                    onDelete = {
                        database.query {
                            deleteSearchHistorySameAs(history.query)
                        }
                    },
                    onFillTextField = {
                        onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                    },
                    modifier = Modifier.animateItem(),
                    pureBlack = pureBlack,
                )
            }
        }

        items(viewState.suggestions, key = { "suggestion_$it" }) { query ->
            SuggestionItem(
                query = query,
                online = true,
                onClick = {
                    onSearch(query)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(query, TextRange(query.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack,
            )
        }

        if (viewState.items.isNotEmpty() && viewState.history.size + viewState.suggestions.size > 0) {
            item(key = "search_divider") {
                HorizontalDivider(
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "search_divider_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(viewState.items, key = { "item_${it.id}" }) { item ->
            YouTubeListItem(
                item = item,
                isActive =
                    when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        is EpisodeItem -> mediaMetadata?.id == item.id
                        else -> false
                    },
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                when (item) {
                                    is SongItem -> {
                                        YouTubeSongMenu(
                                            song = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }

                                    is AlbumItem -> {
                                        YouTubeAlbumMenu(
                                            albumItem = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }

                                    is ArtistItem -> {
                                        YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }

                                    is PlaylistItem -> {
                                        YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }

                                    is PodcastItem -> {
                                        YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }

                                    is EpisodeItem -> {
                                        YouTubeSongMenu(
                                            song = item.asSongItem(),
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                        )
                    }
                },
                modifier =
                    Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                if (autoRadioQueue) {
                                                    YouTubeQueue.radio(item.toMediaMetadata())
                                                } else {
                                                    ListQueue(
                                                        title = item.title,
                                                        items = listOf(item.toMediaItem())
                                                    )
                                                }
                                            )
                                            onDismiss()
                                        }
                                    }

                                    is AlbumItem -> {
                                        navController.navigate("album/${item.id}")
                                        onDismiss()
                                    }

                                    is ArtistItem -> {
                                        navController.navigate("artist/${item.id}")
                                        onDismiss()
                                    }

                                    is PlaylistItem -> {
                                        navController.navigate("online_playlist/${item.id}")
                                        onDismiss()
                                    }

                                    is PodcastItem -> {
                                        navController.navigate("online_podcast/${item.id}")
                                        onDismiss()
                                    }

                                    is EpisodeItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(item.toMediaMetadata()),
                                            )
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (item) {
                                        is SongItem -> {
                                            YouTubeSongMenu(
                                                song = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is AlbumItem -> {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is ArtistItem -> {
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is PlaylistItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is PodcastItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item.asPlaylistItem(),
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is EpisodeItem -> {
                                            YouTubeSongMenu(
                                                song = item.asSongItem(),
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        ).background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                        .animateItem(),
            )
        }
    }
}

/**
 * Blazify: the "Recent Searches" block shown when the search box is empty.
 *
 * Two rows of chips that scroll sideways, so recent searches never take more
 * than two lines above Browse however many there are. Two rows show twice what
 * a single rail did before the rest runs off the right edge.
 */
@Composable
fun RecentSearchesRail(
    history: List<SearchHistory>,
    onSearch: (String) -> Unit,
    onDelete: (SearchHistory) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.clear_all),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Each chip goes into whichever row is shorter, so the two rows stay level.
        LazyHorizontalStaggeredGrid(
            rows = StaggeredGridCells.Fixed(2),
            horizontalItemSpacing = 6.dp,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    // Two 30dp chips and the gap between them.
                    .height(66.dp),
        ) {
            items(history.take(20), key = { it.query }) { entry ->
                RecentSearchChip(
                    query = entry.query,
                    onClick = { onSearch(entry.query) },
                    onDelete = { onDelete(entry) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** Two Browse tiles side by side, the same height, a lone last one keeping its column. */
@Composable
private fun BrowseTileRow(
    row: List<MoodAndGenres.Item>,
    browseArt: Map<String, List<String>>,
    onOpen: (MoodAndGenres.Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                // Both tiles take the height of the taller one,
                // so a two-line title does not leave a ragged row.
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        row.forEach { mood ->
            BrowseTile(
                title = mood.title,
                stripeColor = mood.stripeColor,
                art = browseArt[BrowseArt.keyOf(mood.endpoint)].orEmpty(),
                onClick = { onOpen(mood) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

/**
 * One tile of the Browse grid, coloured by the stripe YouTube ships with each
 * mood — the same colour its own apps use, so the grid is recognisable rather
 * than twelve identical grey rectangles. Covers from inside the mood fan out in
 * the corner once they have been found; until then the colour carries the tile.
 */
@Composable
private fun BrowseTile(
    title: String,
    stripeColor: Long,
    art: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seed = Color(stripeColor or 0xFF000000L)
    // Light tiles need dark type on them; the rest take white.
    val onSeed = if (seed.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier =
            modifier
                .heightIn(min = 92.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(seed.copy(alpha = 1f).lighten(0.24f), seed, seed.darken(0.18f)),
                    ),
                )
                .clickable(onClick = onClick),
    ) {
        // Up to three covers fanned like a hand of cards, the first in front and leaning
        // off the corner, the others spread behind it. Drawn back to front.
        val context = LocalContext.current
        val corner = with(LocalDensity.current) { 6.dp.toPx() }
        art.take(FAN.size).withIndex().reversed().forEach { (index, cover) ->
            val place = FAN[index]
            // A cover's corners are rounded once, as it loads, rather than cut out of the
            // tilted picture on every frame, which on slower phones cost more than all the
            // rest of the page. The placeholder is still cut live until the cover arrives.
            var rounded by remember(cover) { mutableStateOf(false) }
            val request =
                remember(cover, corner) {
                    ImageRequest.Builder(context)
                        .data(cover)
                        .transformations(RoundedCornersTransformation(corner))
                        .build()
                }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { rounded = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = place.x, y = place.y)
                        .size(56.dp)
                        .rotate(place.angle)
                        // A painted shadow, not an elevation one. Three tilted covers on each of
                        // forty tiles were a hundred and twenty live shadows, recalculated every
                        // time the screen redrew: 55 ms a frame, and Search crawled at six frames
                        // a second while every other page ran smooth.
                        .drawBehind {
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.28f),
                                topLeft = Offset(2.dp.toPx(), 4.dp.toPx()),
                                size = size,
                                cornerRadius = CornerRadius(6.dp.toPx()),
                            )
                        }
                        .then(if (rounded) Modifier else Modifier.clip(RoundedCornerShape(6.dp))),
            )
        }
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = onSeed,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    // Clear of the covers in the corner.
                    .fillMaxWidth(if (art.isNotEmpty()) 0.64f else 1f),
        )
    }
}

/** Where each cover sits in a Browse tile's fan, measured from the bottom-right corner. */
private class FanPlace(val x: androidx.compose.ui.unit.Dp, val y: androidx.compose.ui.unit.Dp, val angle: Float)

private val FAN =
    listOf(
        FanPlace(x = 14.dp, y = 14.dp, angle = 16f),
        FanPlace(x = (-14).dp, y = 10.dp, angle = 2f),
        FanPlace(x = (-40).dp, y = 16.dp, angle = -12f),
    )

/** Mixes a colour towards black, for the far corner of a Browse tile's gradient. */
private fun Color.darken(amount: Float) =
    Color(
        red = red * (1f - amount),
        green = green * (1f - amount),
        blue = blue * (1f - amount),
        alpha = alpha,
    )

/** Mixes a colour towards white, as the iPhone's browse tiles do. */
private fun Color.lighten(amount: Float) =
    Color(
        red = red + (1f - red) * amount,
        green = green + (1f - green) * amount,
        blue = blue + (1f - blue) * amount,
        alpha = alpha,
    )

@Composable
private fun RecentSearchChip(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                // Small enough that recent searches take a line or two, not the screen.
                .height(30.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(percent = 50),
                )
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 6.dp),
    ) {
        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 200.dp),
        )
        Box(
            modifier =
                Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .height(SuggestionItemHeight)
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Icon(
            painterResource(if (online) R.drawable.search else R.drawable.history),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f),
        )

        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(0.5f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                )
            }
        }

        IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(0.5f),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
            )
        }
    }
}
