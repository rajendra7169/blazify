/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.blazify.music.R

private val SideButton = 46.dp
private val PlayButton = 68.dp

/**
 * The buttons under a playlist's or an album's cover: keep it and download it
 * on the left, play in the middle, shuffle and more on the right.
 *
 * Always five places, so Play sits in the middle of the screen whatever the
 * page offers — a page with no shuffle keeps an empty place for it rather
 * than sliding Play off to one side.
 *
 * [downloadState] is a media3 [Download] state for the whole list, or null
 * where downloading does not apply (a podcast, an empty list). While the list
 * is downloading, the spinner can be tapped to stop it through [onStopDownload];
 * songs that already finished stay on the phone.
 */
@Composable
fun CollectionActionRow(
    liked: Boolean,
    onLike: () -> Unit,
    downloadState: Int?,
    onDownload: () -> Unit,
    onStopDownload: (() -> Unit)? = null,
    onPlay: () -> Unit,
    onShuffle: (() -> Unit)?,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(
            icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
            description = stringResource(R.string.like),
            tint = if (liked) MaterialTheme.colorScheme.error else null,
            onClick = onLike,
        )

        if (downloadState != null) {
            val done = downloadState == Download.STATE_COMPLETED
            val busy = downloadState == Download.STATE_QUEUED || downloadState == Download.STATE_DOWNLOADING
            var askStop by remember { mutableStateOf(false) }
            RoundButton(
                icon = if (done) R.drawable.offline else R.drawable.download,
                description = stringResource(
                    when {
                        done -> R.string.offline
                        busy -> R.string.downloading_tap_to_stop
                        else -> R.string.action_download
                    },
                ),
                tint = if (done) MaterialTheme.colorScheme.primary else null,
                enabled = !done && (!busy || onStopDownload != null),
                busy = busy,
                onClick = { if (busy) askStop = true else onDownload() },
            )
            if (askStop && busy && onStopDownload != null) {
                AlertDialog(
                    onDismissRequest = { askStop = false },
                    title = { Text(stringResource(R.string.stop_downloading_title)) },
                    text = { Text(stringResource(R.string.stop_downloading_text)) },
                    confirmButton = {
                        TextButton(onClick = { askStop = false; onStopDownload() }) {
                            Text(stringResource(R.string.stop_downloading_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { askStop = false }) {
                            Text(stringResource(R.string.keep_downloading))
                        }
                    },
                )
            }
        } else {
            Spacer(Modifier.size(SideButton))
        }

        Surface(
            onClick = onPlay,
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            shadowElevation = 6.dp,
            modifier = Modifier.size(PlayButton),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        if (onShuffle != null) {
            RoundButton(
                icon = R.drawable.shuffle,
                description = stringResource(R.string.shuffle),
                onClick = onShuffle,
            )
        } else {
            Spacer(Modifier.size(SideButton))
        }

        RoundButton(
            icon = R.drawable.more_vert,
            description = stringResource(R.string.more_options),
            onClick = onMore,
        )
    }
}

@Composable
private fun RoundButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    tint: Color? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    size: Dp = SideButton,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (busy) {
                BlazeDownloadLoader(
                    modifier = Modifier.size(22.dp).semantics { contentDescription = description },
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = description,
                    tint = tint ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
