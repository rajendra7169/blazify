/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.blazify.music.R
import com.blazify.music.utils.PlaylistLink
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Hands a playlist of one's own to someone else: a square to point a camera at, or a link to
 * send. Both carry the same thing — the playlist's name and its songs — and open in Blazify.
 */
@Composable
fun SharePlaylistDialog(
    playlistName: String,
    songIds: List<String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val link = remember(playlistName, songIds) { PlaylistLink.build(playlistName, songIds) }
    val code = remember(link) { qrCode(link) }
    val leftOut = (songIds.size - PlaylistLink.MAX_SONGS).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_playlist)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (code != null) {
                    Image(
                        bitmap = code.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(232.dp)
                            .clip(RoundedCornerShape(12.dp))
                            // A white surround, without which a camera has nothing to read against.
                            .background(androidx.compose.ui.graphics.Color.White)
                            .padding(12.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.share_playlist_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (leftOut > 0) {
                    Text(
                        text = stringResource(R.string.share_playlist_trimmed, PlaylistLink.MAX_SONGS, leftOut),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { copyLink(context, link) }) {
                    Text(stringResource(R.string.copy_link))
                }
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, link)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun copyLink(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.share_playlist), link))
    Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
}

/** [text] as a square of black and white pixels, or null if it will not fit in one. */
private fun qrCode(text: String, size: Int = 640): Bitmap? =
    runCatching {
        val matrix = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(
                EncodeHintType.MARGIN to 1,
                // The lowest correction holds the most: a link this long needs the room.
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val row = y * size
            for (x in 0 until size) {
                pixels[row + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        createBitmap(size, size).apply { setPixels(pixels, 0, size, 0, 0, size, size) }
    }.getOrNull()
