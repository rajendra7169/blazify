package com.blazify.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.blazify.music.LocalDatabase
import com.blazify.music.R
import com.blazify.music.utils.SpotifyImport
import com.blazify.music.utils.SpotifyPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Paste a Spotify link, get the playlist here.
 *
 * The work is a search per track, so it takes a while and says how far along
 * it is. At the end it says plainly how many were found and how many were not,
 * rather than leaving somebody to count.
 */
@Composable
fun SpotifyImportDialog(
    onDismiss: () -> Unit,
    onImported: (String) -> Unit,
) {
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    var link by remember { mutableStateOf(TextFieldValue("")) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<SpotifyImport.Outcome?>(null) }
    var failed by remember { mutableStateOf(false) }

    val result = outcome
    val looksRight = SpotifyPlaylist.parseLink(link.text) != null

    DefaultDialog(
        onDismiss = { if (!running) onDismiss() },
        icon = { Icon(painterResource(R.drawable.playlist_add), contentDescription = null) },
        title = { Text(stringResource(R.string.import_spotify)) },
        buttons = {
            if (result != null) {
                TextButton(onClick = { onImported(result.playlistId) }) {
                    Text(stringResource(R.string.open_playlist))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
            } else {
                TextButton(enabled = !running, onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    enabled = looksRight && !running,
                    onClick = {
                        running = true
                        failed = false
                        scope.launch(Dispatchers.IO) {
                            SpotifyImport
                                .import(link.text, database) { progress ->
                                    done = progress.done
                                    total = progress.total
                                }.onSuccess { outcome = it }
                                .onFailure { failed = true }
                            running = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.import_action))
                }
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        ) {
            when {
                result != null -> {
                    Text(
                        text = stringResource(R.string.import_spotify_done, result.matched, result.total),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (result.missing.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.import_spotify_missing, result.missing.take(5).joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (result.mayHaveMore) {
                        Text(
                            text = stringResource(R.string.import_spotify_only_hundred),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                running -> {
                    Text(
                        text =
                            if (total == 0) {
                                stringResource(R.string.import_spotify_reading)
                            } else {
                                stringResource(R.string.import_spotify_progress, done, total)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (total == 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { done.toFloat() / total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = link,
                        onValueChange = {
                            link = it
                            failed = false
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.import_spotify_link)) },
                        isError = link.text.isNotBlank() && !looksRight,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text =
                            if (failed) {
                                stringResource(R.string.import_spotify_failed)
                            } else {
                                stringResource(R.string.import_spotify_hint)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (failed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
    }
}
