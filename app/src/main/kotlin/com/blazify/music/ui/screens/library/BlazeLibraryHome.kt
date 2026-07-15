/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * BlazePlayer-style Library landing: system playlists as colour+glyph gradient
 * cards (Liked long, Cached/Downloaded pair, Your Top 50 long, Uploaded long),
 * then user playlists under "Created by you", then "Artists you liked".
 */

package com.blazify.music.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.ui.component.BlazeMusicCard
import com.blazify.music.ui.component.BlazePlaylistCard
import com.blazify.music.ui.component.BlazePlaylistPalette
import com.blazify.music.ui.component.BlazeSectionHeader
import com.blazify.music.viewmodels.YoursViewModel

private const val LONG_RATIO = 2.9f
private const val BOX_RATIO = 1.5f
private const val USER_RATIO = 1.55f

@Composable
fun BlazeLibraryHome(
    navController: NavController,
    viewModel: YoursViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val artists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val likedThumbs by viewModel.likedThumbnails.collectAsStateWithLifecycle()
    val downloadedThumbs by viewModel.downloadedThumbnails.collectAsStateWithLifecycle()
    val uploadedThumbs by viewModel.uploadedThumbnails.collectAsStateWithLifecycle()
    val topThumbs by viewModel.topThumbnails.collectAsStateWithLifecycle()
    val cachedThumbs by viewModel.cachedThumbnails.collectAsStateWithLifecycle()
    val songsWord = stringResource(R.string.songs).lowercase()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item("top_gap") {
            Spacer(Modifier.height(8.dp))
        }

        // ---- System playlists ----
        item("liked") {
            LongPad {
                BlazePlaylistCard(
                    title = stringResource(R.string.liked),
                    subtitle = "${likedSongs.size} $songsWord",
                    thumbnails = likedThumbs,
                    seedColor = Color(0xFFB71C5A),
                    aspectRatio = LONG_RATIO,
                    iconRes = R.drawable.favorite,
                    onClick = { navController.navigate("auto_playlist/liked") },
                )
            }
        }
        item("cached_downloaded") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BlazePlaylistCard(
                    title = stringResource(R.string.cached_playlist),
                    subtitle = "",
                    thumbnails = cachedThumbs,
                    seedColor = Color(0xFF00838F),
                    aspectRatio = BOX_RATIO,
                    iconRes = R.drawable.cached,
                    onClick = { navController.navigate("cache_playlist/cached") },
                    modifier = Modifier.weight(1f),
                )
                BlazePlaylistCard(
                    title = stringResource(R.string.offline),
                    subtitle = "",
                    thumbnails = downloadedThumbs,
                    seedColor = Color(0xFF283593),
                    aspectRatio = BOX_RATIO,
                    iconRes = R.drawable.download,
                    onClick = { navController.navigate("auto_playlist/downloaded") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item("top") {
            LongPad {
                BlazePlaylistCard(
                    title = stringResource(R.string.your_top_50),
                    subtitle = "",
                    thumbnails = topThumbs,
                    seedColor = Color(0xFFEF6C00),
                    aspectRatio = LONG_RATIO,
                    iconRes = R.drawable.trending_up,
                    onClick = { navController.navigate("top_playlist/50") },
                )
            }
        }
        item("uploaded") {
            LongPad {
                BlazePlaylistCard(
                    title = stringResource(R.string.uploaded_playlist),
                    subtitle = "",
                    thumbnails = uploadedThumbs,
                    seedColor = Color(0xFF6A1B9A),
                    aspectRatio = LONG_RATIO,
                    iconRes = R.drawable.upload,
                    onClick = { navController.navigate("auto_playlist/uploaded") },
                )
            }
        }

        // ---- Created by you ----
        if (playlists.isNotEmpty()) {
            item("cby_head") {
                Spacer(Modifier.height(8.dp))
                BlazeSectionHeader(stringResource(R.string.created_by_you))
            }
            itemsIndexed(playlists.chunked(2), key = { _, row -> "cby_${row.first().id}" }) { rowIndex, rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEachIndexed { j, pl ->
                        BlazePlaylistCard(
                            title = pl.title,
                            subtitle = "${pl.songCount} $songsWord",
                            thumbnails = pl.thumbnails.take(4),
                            seedColor = BlazePlaylistPalette[(rowIndex * 2 + j) % BlazePlaylistPalette.size],
                            aspectRatio = USER_RATIO,
                            onClick = { navController.navigate("local_playlist/${pl.id}") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // ---- Artists you liked ----
        if (artists.isNotEmpty()) {
            item("art_head") {
                Spacer(Modifier.height(8.dp))
                BlazeSectionHeader(stringResource(R.string.artists_you_liked))
            }
            item("art_rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(artists, key = { it.id }) { artist ->
                        BlazeMusicCard(
                            title = artist.title,
                            subtitle = if (artist.songCount > 0) "${artist.songCount} $songsWord" else "",
                            thumbnailUrl = artist.thumbnailUrl,
                            isCircular = true,
                            fallbackIcon = R.drawable.artist,
                            onClick = { navController.navigate("artist/${artist.id}") },
                        )
                    }
                }
            }
        }

        item("bottom") { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun LongPad(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        content()
    }
}
