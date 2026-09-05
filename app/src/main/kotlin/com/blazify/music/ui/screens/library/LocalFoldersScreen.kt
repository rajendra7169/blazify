/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.library

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.component.ListItem
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.viewmodels.LocalFoldersViewModel

/**
 * The music on this device, arranged the way it is arranged on the device.
 *
 * A flat list is fine for a library somebody else sorted. Files a person put
 * on their own phone are already sorted, by them, into folders that mean
 * something — and throwing that away and showing four hundred filenames in
 * one column loses the only organisation those files had.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFoldersScreen(
    navController: NavController,
    viewModel: LocalFoldersViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding =
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
            modifier =
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
                ),
        ) {
            if (folders.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.local_music_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            items(items = folders, key = { it.path }) { folder ->
                ListItem(
                    title = folder.name,
                    subtitle =
                        pluralStringResource(
                            R.plurals.n_song,
                            folder.songCount,
                            folder.songCount,
                        ),
                    thumbnailContent = {
                        Icon(
                            painter = painterResource(R.drawable.folder),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    modifier =
                        Modifier.clickable {
                            val encoded = Uri.encode(folder.path)
                            navController.navigate("auto_playlist/local?folder=$encoded")
                        },
                )
            }
        }

        TopAppBar(
            title = { Text(stringResource(R.string.local_folders)) },
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
}
