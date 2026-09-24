/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackException
import com.blazify.music.BuildConfig
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import java.time.Instant

@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata = playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val streamClient = playerConnection?.currentStreamClient?.collectAsStateWithLifecycle()?.value

    // Build detailed error info for debugging
    val rawErrorMessage = error.cause?.cause?.message
        ?: error.cause?.message
        ?: error.message
        ?: stringResource(R.string.error_unknown)

    val isAgeRestricted = isAgeRestrictedMessage(rawErrorMessage)

    // Check if this is a "job cancelled" error from YouTube
    // YouTube returns this when the playback job cannot be started (often transient)
    val isJobCancelled = rawErrorMessage.contains("job", ignoreCase = true) &&
            (rawErrorMessage.contains("cancelled", ignoreCase = true) ||
                    rawErrorMessage.contains("canceled", ignoreCase = true) ||
                    rawErrorMessage.contains("cancellat", ignoreCase = true))

    val errorMessage = if (isAgeRestricted) {
        stringResource(R.string.error_age_restricted)
    } else if (isJobCancelled) {
        stringResource(R.string.error_job_cancelled)
    } else {
        rawErrorMessage
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Error icon
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Main error message
        Text(
            text = stringResource(R.string.error_playback_failed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error details
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Error code
        Text(
            text = "Code: ${getErrorCodeName(error.errorCode)} (${error.errorCode})",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Retry button
            Button(
                onClick = retry,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.replay),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.retry))
            }

            // A plain-text report someone can paste into a bug report, so "it doesn't play"
            // arrives with the error, the song and the phone it happened on.
            OutlinedButton(
                onClick = {
                    val song = mediaMetadata?.let { meta ->
                        val artists = meta.artists.joinToString { it.name }
                        listOf(meta.title, artists).filter { it.isNotBlank() }.joinToString(" - ") + " (${meta.id})"
                    }
                    val report = playbackErrorReport(
                        error = error,
                        errorCodeName = getErrorCodeName(error.errorCode),
                        errorCode = error.errorCode,
                        timestampMs = error.timestampMs,
                        app = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.FLAVOR}, ${BuildConfig.ARCHITECTURE}",
                        android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        device = "${Build.MANUFACTURER} ${Build.MODEL}",
                        song = song,
                        streamClient = streamClient,
                    )
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Blazify playback report", report))
                    // Android 13 and newer confirm a copy on their own.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.content_copy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.copy_details))
            }
        }
    }
}

/**
 * The text "Copy details" puts on the clipboard.
 *
 * It holds what a bug report needs and nothing personal: no account, no cookies, and links in
 * error messages are cut down to their host, since stream links carry signed tokens.
 */
internal fun playbackErrorReport(
    error: Throwable,
    errorCodeName: String,
    errorCode: Int,
    timestampMs: Long,
    app: String,
    android: String,
    device: String,
    song: String?,
    streamClient: String?,
): String = buildString {
    appendLine("Blazify playback report")
    appendLine("Time: ${Instant.ofEpochMilli(timestampMs)}")
    appendLine("App: $app")
    appendLine("Android: $android")
    appendLine("Device: $device")
    appendLine("Song: ${song ?: "unknown"}")
    appendLine("Stream: ${streamClient ?: "unknown"}")
    appendLine("Error: $errorCodeName ($errorCode)")
    appendLine("Causes:")
    generateSequence(error) { it.cause }.take(8).forEach { cause ->
        val message = cause.message?.let(::withoutLinkDetails)?.takeIf { it.isNotBlank() }
        appendLine("- ${cause.javaClass.simpleName}" + (message?.let { ": $it" } ?: ""))
    }
}.trimEnd()

private val LINK = Regex("""https?://([^/\s?#]+)[^\s]*""")

internal fun withoutLinkDetails(text: String): String = LINK.replace(text) { "${it.groupValues[1]}/…" }

/**
 * Whether an error message really says the song is age-restricted.
 *
 * Only the site's own wording counts. A bare 403 or a bad HTTP status is far more often a
 * stream link that has expired, and the old check for plain "age" also matched words like
 * "page" and "image", so ordinary failures were blamed on age restrictions.
 */
internal fun isAgeRestrictedMessage(message: String): Boolean =
    message.contains("confirm your age", ignoreCase = true) ||
        message.contains("age-restricted", ignoreCase = true) ||
        message.contains("age restricted", ignoreCase = true) ||
        message.contains("LOGIN_REQUIRED", ignoreCase = true)

/**
 * Get human-readable error code name from PlaybackException error code
 */
private fun getErrorCodeName(errorCode: Int): String {
    return when (errorCode) {
        PlaybackException.ERROR_CODE_UNSPECIFIED -> "UNSPECIFIED"
        PlaybackException.ERROR_CODE_REMOTE_ERROR -> "REMOTE_ERROR"
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "BEHIND_LIVE_WINDOW"
        PlaybackException.ERROR_CODE_TIMEOUT -> "TIMEOUT"
        PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "FAILED_RUNTIME_CHECK"
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "IO_UNSPECIFIED"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "IO_NETWORK_CONNECTION_FAILED"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "IO_NETWORK_CONNECTION_TIMEOUT"
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "IO_INVALID_HTTP_CONTENT_TYPE"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "IO_BAD_HTTP_STATUS"
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "IO_FILE_NOT_FOUND"
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "IO_NO_PERMISSION"
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "IO_CLEARTEXT_NOT_PERMITTED"
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "IO_READ_POSITION_OUT_OF_RANGE"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "PARSING_CONTAINER_MALFORMED"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "PARSING_MANIFEST_MALFORMED"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "PARSING_CONTAINER_UNSUPPORTED"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "PARSING_MANIFEST_UNSUPPORTED"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "DECODER_INIT_FAILED"
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "DECODER_QUERY_FAILED"
        PlaybackException.ERROR_CODE_DECODING_FAILED -> "DECODING_FAILED"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "DECODING_FORMAT_EXCEEDS_CAPABILITIES"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "DECODING_FORMAT_UNSUPPORTED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "AUDIO_TRACK_INIT_FAILED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "AUDIO_TRACK_WRITE_FAILED"
        PlaybackException.ERROR_CODE_DRM_UNSPECIFIED -> "DRM_UNSPECIFIED"
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED -> "DRM_SCHEME_UNSUPPORTED"
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> "DRM_PROVISIONING_FAILED"
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> "DRM_CONTENT_ERROR"
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> "DRM_LICENSE_ACQUISITION_FAILED"
        PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION -> "DRM_DISALLOWED_OPERATION"
        PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> "DRM_SYSTEM_ERROR"
        PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> "DRM_DEVICE_REVOKED"
        PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> "DRM_LICENSE_EXPIRED"
        else -> "UNKNOWN_ERROR_$errorCode"
    }
}
