/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import android.media.AudioDeviceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blazify.music.R
import com.blazify.music.utils.AudioOutput
import com.blazify.music.utils.AudioOutputDevice
import com.blazify.music.utils.availableAudioOutputs

/**
 * Picks which connected output plays the music.
 *
 * Routing goes through ExoPlayer's preferred-device hook rather than the system
 * output panel, whose Intent action is not public and does not resolve on every
 * OEM build. Pairing still belongs to the system, so there is a way out to
 * Bluetooth settings at the bottom.
 *
 * Only already-connected devices can appear — Android exposes nothing else.
 *
 * @param onSelect null means "let the system decide".
 */
@Composable
fun AudioOutputDialog(
    currentDeviceId: Int?,
    onSelect: (AudioDeviceInfo?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf<List<AudioOutputDevice>>(emptyList()) }
    LaunchedEffect(Unit) { devices = availableAudioOutputs(context) }

    DefaultDialog(
        onDismiss = onDismiss,
        buttons = {
            TextButton(
                onClick = {
                    AudioOutput.openOutputSwitcher(context)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.output_more))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.audio_output),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            devices.forEach { entry ->
                OutputRow(
                    name = entry.name,
                    iconRes = iconFor(entry.type),
                    selected = entry.id == currentDeviceId,
                    onClick = {
                        onSelect(entry.device)
                        onDismiss()
                    },
                )
            }
            if (devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.output_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OutputRow(
    name: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

private fun iconFor(type: Int): Int = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_HEARING_AID,
    -> R.drawable.bluetooth
    else -> R.drawable.volume_up
}
