/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blazify.music.R

/**
 * Correct what a file calls itself.
 *
 * Music that arrives on a phone rather than from a catalogue is often named
 * by whatever put it there, so a song can sit in the library as
 * AUD-20260905-WA0014 with no artist at all and no way to say otherwise.
 *
 * Blank means "leave the file's own answer alone", so clearing every box is
 * the same as never having corrected it. Nothing here is written to the file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocalTagsDialog(
    initialTitle: String,
    initialArtist: String,
    initialAlbum: String,
    hasOverride: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String) -> Unit,
    onReset: () -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }
    var album by remember { mutableStateOf(initialAlbum) }

    LaunchedEffect(initialTitle, initialArtist, initialAlbum) {
        title = initialTitle
        artist = initialArtist
        album = initialAlbum
    }

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.edit_tags)) },
        buttons = {
            if (hasOverride) {
                TextButton(onClick = { onReset(); onDismiss() }) {
                    Text(stringResource(R.string.edit_tags_reset))
                }
                Spacer(Modifier.fillMaxWidth(0.02f))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(
                onClick = {
                    onSave(title.trim(), artist.trim(), album.trim())
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.edit_tags_save))
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.edit_tags_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(R.string.edit_tags_artist)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text(stringResource(R.string.edit_tags_album)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.edit_tags_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
