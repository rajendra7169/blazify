/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.BlockedArtistsKey
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.BlockedArtists
import com.blazify.music.utils.rememberPreference

/**
 * Artists blocked with "Block this artist". Their songs stay out of picks, radio, autoplay,
 * Home and search until they are let back in here or from the artist's menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedArtistsSettings(navController: NavController) {
    val (blocked, setBlocked) = rememberPreference(BlockedArtistsKey, emptySet())
    val entries = remember(blocked) { blocked.sortedBy { BlockedArtists.nameOf(it).lowercase() } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current),
    ) {
        item(key = "intro") {
            Text(
                text = stringResource(
                    if (entries.isEmpty()) R.string.blocked_artists_empty else R.string.blocked_artists_desc,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        items(entries, key = { it }) { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = BlockedArtists.nameOf(entry),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { setBlocked(blocked - entry) }) {
                    Text(stringResource(R.string.unblock))
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.blocked_artists)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
