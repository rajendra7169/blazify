/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0
 */

package com.blazify.music.ui.screens.settings

import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.blazify.innertube.YouTube
import com.blazify.innertube.models.AccountChannel
import com.blazify.music.R
import com.blazify.music.constants.AccountEmailKey
import com.blazify.music.constants.AccountNameKey
import com.blazify.music.constants.DataSyncIdKey
import com.blazify.music.ui.component.DefaultDialog
import com.blazify.music.utils.AccountSwitcher
import com.blazify.music.utils.SavedAccount
import com.blazify.music.utils.rememberPreference
import com.blazify.music.viewmodels.AccountSettingsViewModel
import kotlinx.coroutines.launch

/**
 * The channels of the account in use, then the other accounts on this phone, then adding one.
 * A choice restarts the app on it, which is said here before anybody taps.
 */
@Composable
fun AccountsDialog(
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
) {
    var switching by remember { mutableStateOf(false) }
    DefaultDialog(
        onDismiss = { if (!switching) onDismiss() },
        title = { Text(stringResource(R.string.accounts_and_channels)) },
        content = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                AccountsPanel(
                    isLoggedIn = isLoggedIn,
                    onAddAccount = onAddAccount,
                    onSwitching = { switching = it },
                )
            }
        },
        buttons = {
            TextButton(enabled = !switching, onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/**
 * The accounts on this phone and the channels of the one in use: switch to any,
 * forget one, or add another. Shown in its own dialog, and opened in place under
 * the account card, so the same list behaves the same wherever it is reached.
 */
@Composable
internal fun AccountsPanel(
    isLoggedIn: Boolean,
    onAddAccount: () -> Unit,
    onSwitching: (Boolean) -> Unit = {},
    showNote: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: AccountSettingsViewModel = hiltViewModel()
    val (currentName) = rememberPreference(AccountNameKey, "")
    val (currentEmail) = rememberPreference(AccountEmailKey, "")
    val (currentSyncId) = rememberPreference(DataSyncIdKey, "")
    val currentKey = "$currentEmail|${currentSyncId.substringBefore("||")}|$currentName"

    var saved by remember { mutableStateOf<List<SavedAccount>>(emptyList()) }
    var channels by remember { mutableStateOf<List<AccountChannel>>(emptyList()) }
    var loadingChannels by remember { mutableStateOf(isLoggedIn) }
    var switching by remember { mutableStateOf(false) }
    LaunchedEffect(switching) { onSwitching(switching) }
    val expired = stringResource(R.string.accounts_expired)

    LaunchedEffect(Unit) {
        AccountSwitcher.rememberCurrent(context)
        saved = AccountSwitcher.saved(context)
        if (isLoggedIn) {
            channels = YouTube.accountChannels().getOrDefault(emptyList())
            loadingChannels = false
        }
    }

    fun switchTo(account: SavedAccount) {
        if (switching) return
        switching = true
        scope.launch {
            if (!viewModel.switchAccount(context, account)) {
                Toast.makeText(context, expired, Toast.LENGTH_LONG).show()
                AccountSwitcher.forget(context, account)
                saved = AccountSwitcher.saved(context)
                switching = false
            }
        }
    }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (showNote) {
                    Text(
                        text = stringResource(R.string.accounts_restart_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }

                if (loadingChannels) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }

                // One line for a person with one channel would only repeat the row they came from.
                if (channels.size > 1) {
                    Heading(stringResource(R.string.accounts_channels_heading))
                    channels.forEach { channel ->
                        AccountRow(
                            name = channel.name,
                            detail = channel.handle.orEmpty(),
                            photo = channel.thumbnailUrl,
                            selected = channel.id == activeChannel(channels, currentSyncId)?.id,
                            onClick = {
                                if (channel.id != activeChannel(channels, currentSyncId)?.id) {
                                    val base = saved.firstOrNull { it.key == currentKey } ?: return@AccountRow
                                    switchTo(
                                        base.copy(
                                            dataSyncId = channel.id,
                                            name = channel.name,
                                            handle = channel.handle.orEmpty(),
                                            photo = channel.thumbnailUrl.orEmpty(),
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }

                val others = saved.filterNot { it.key == currentKey && isLoggedIn }
                    .filterNot { account -> channels.any { it.name == account.name && it.id == account.dataSyncId.substringBefore("||") } }
                if (others.isNotEmpty()) {
                    Heading(stringResource(R.string.accounts_other_heading))
                    others.forEach { account ->
                        AccountRow(
                            name = account.name,
                            detail = account.email.ifBlank { account.handle },
                            photo = account.photo,
                            selected = false,
                            onClick = { switchTo(account) },
                            onRemove = {
                                scope.launch {
                                    AccountSwitcher.forget(context, account)
                                    saved = AccountSwitcher.saved(context)
                                }
                            },
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !switching) {
                                // The login page keeps its own sign-in, and on seeing it would
                                // go straight back to the account already in use.
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                onAddAccount()
                            }.padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Icon(painterResource(R.drawable.add), null, Modifier.size(40.dp).padding(8.dp))
                    Text(
                        text = stringResource(R.string.accounts_add_another),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                if (switching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Text(stringResource(R.string.accounts_switching))
                    }
                }
            }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** The channel in use: the one this phone was told to use, or else whichever the answer marks. */
internal fun activeChannel(channels: List<AccountChannel>, dataSyncId: String): AccountChannel? {
    val id = dataSyncId.substringBefore("||")
    return channels.firstOrNull { it.id == id } ?: channels.firstOrNull { it.isSelected } ?: channels.firstOrNull()
}

@Composable
internal fun AccountRow(
    name: String,
    detail: String,
    photo: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (photo.isNullOrBlank()) {
                Icon(painterResource(R.drawable.person), null, Modifier.size(24.dp))
            } else {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                painterResource(R.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.accounts_remove),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
