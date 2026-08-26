/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blazify.music.R
import com.blazify.music.ui.screens.settings.MarkdownText
import com.blazify.music.utils.ReleaseInfo
import com.blazify.music.utils.UpdateInstaller
import com.blazify.music.utils.Updater
import kotlinx.coroutines.launch

/**
 * What happens between "there is a new version" and "it is installed".
 *
 * Deliberately one dialog rather than a link out to a web page. The alternative
 * — open the releases page in a browser, find the right file among several, work
 * out what to do with it once it has downloaded — is where most people stop, and
 * the ones who do not stop are the ones who did not need the help.
 *
 * The awkward case is handled rather than hidden. A build signed with a
 * different key cannot install over the old one; instead of letting somebody
 * discover that as a bare "App not installed", they are told before they start,
 * told to back up first, and the download is put somewhere that survives the
 * uninstall they are about to do.
 */
private sealed interface Stage {
    data object Offer : Stage
    data class Downloading(val done: Long, val total: Long?) : Stage
    data class Ready(val fetched: UpdateInstaller.Fetched) : Stage
    data class Failed(val message: String) : Stage
}

@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf<Stage>(Stage.Offer) }
    var needsPermission by remember { mutableStateOf(false) }

    val asset = remember(release) { Updater.pickAsset(release.assets) }

    fun startInstall(fetched: UpdateInstaller.Fetched) {
        if (!UpdateInstaller.mayInstall(context)) {
            needsPermission = true
            return
        }
        needsPermission = false
        UpdateInstaller.install(context, fetched)
    }

    AlertDialog(
        onDismissRequest = { if (stage !is Stage.Downloading) onDismiss() },
        title = {
            Text(
                when (val s = stage) {
                    is Stage.Ready ->
                        if (s.fetched.needsUninstallFirst) {
                            stringResource(R.string.update_uninstall_first_title)
                        } else {
                            stringResource(R.string.update_new_version, release.tagName.removePrefix("v"))
                        }
                    else -> stringResource(R.string.update_new_version, release.tagName.removePrefix("v"))
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val s = stage) {
                    is Stage.Offer -> {
                        if (asset == null) {
                            Text(stringResource(R.string.update_no_download))
                        }
                        MarkdownText(release.description)
                    }

                    is Stage.Downloading -> {
                        val total = s.total
                        if (total != null && total > 0) {
                            val pct = ((s.done * 100) / total).toInt().coerceIn(0, 100)
                            Text(stringResource(R.string.update_downloading, pct))
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(stringResource(R.string.update_downloading_indeterminate))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    is Stage.Ready -> {
                        if (s.fetched.needsUninstallFirst) {
                            Text(
                                stringResource(
                                    R.string.update_uninstall_first_body,
                                    s.fetched.displayName,
                                ),
                            )
                        } else {
                            Text(
                                stringResource(
                                    R.string.update_saved_to_downloads,
                                    s.fetched.displayName,
                                ),
                            )
                        }
                        if (needsPermission) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.update_allow_installs),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    is Stage.Failed ->
                        Text(
                            stringResource(R.string.update_failed, s.message),
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            }
        },
        confirmButton = {
            when (val s = stage) {
                is Stage.Offer ->
                    if (asset != null) {
                        TextButton(onClick = {
                            scope.launch {
                                stage = Stage.Downloading(0, asset.size.takeIf { it > 0 })
                                UpdateInstaller.fetch(
                                    context = context,
                                    url = asset.downloadUrl,
                                    displayName = asset.name,
                                    onProgress = { done, total ->
                                        stage = Stage.Downloading(done, total ?: asset.size.takeIf { it > 0 })
                                    },
                                ).onSuccess { stage = Stage.Ready(it) }
                                    .onFailure {
                                        stage = Stage.Failed(readable(it))
                                    }
                            }
                        }) { Text(stringResource(R.string.update_download)) }
                    }

                is Stage.Downloading -> Unit

                is Stage.Ready ->
                    if (needsPermission) {
                        TextButton(onClick = { UpdateInstaller.requestInstallPermission(context) }) {
                            Text(stringResource(R.string.update_allow_installs_button))
                        }
                    } else if (s.fetched.needsUninstallFirst) {
                        TextButton(onClick = { openDownloads(context) }) {
                            Text(stringResource(R.string.update_open_downloads))
                        }
                    } else {
                        TextButton(onClick = { startInstall(s.fetched) }) {
                            Text(stringResource(R.string.update_install_now))
                        }
                    }

                is Stage.Failed -> TextButton(onClick = { stage = Stage.Offer }) {
                    Text(stringResource(R.string.update_download))
                }
            }
        },
        dismissButton = {
            if (stage !is Stage.Downloading) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
            }
        },
    )
}

/**
 * A failure somebody can read.
 *
 * These messages arrive with the whole signed download URL appended — several
 * hundred characters of token — which fills the dialog and buries the one part
 * that means anything.
 */
private fun readable(t: Throwable): String =
    (t.message ?: t::class.java.simpleName)
        .substringBefore(" [url=")
        .substringBefore(", url=")
        .take(160)

/** Open the system Downloads view, where the fetched build is waiting. */
private fun openDownloads(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        // Some builds have no Downloads app at all; the file manager's document
        // tree is the next best door into the same folder.
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(
                        android.net.Uri.parse("content://com.android.externalstorage.documents/root/primary"),
                        "vnd.android.document/root",
                    )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
