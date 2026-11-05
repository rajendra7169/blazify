/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.settings

import android.webkit.CookieManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.blazify.music.R
import com.blazify.music.constants.AccountEmailKey
import com.blazify.music.constants.AccountNameKey
import com.blazify.music.constants.DataSyncIdKey
import com.blazify.music.constants.SavedAccountsKey
import com.blazify.music.utils.AccountSwitcher
import com.blazify.music.utils.SavedAccount
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.launch

/**
 * Who is signed in, at the top of the account sheet, and every other account on
 * the phone one tap away beneath it.
 *
 * The line under the name says how many other accounts are saved, so it is plain
 * there is somewhere to switch to. The arrow opens the list in place: the other
 * accounts, the channels of this one, and a row to add another. Signed out, the
 * card offers to add an account, and still lists any saved ones to go back to.
 */
@Composable
internal fun AccountCard(
    isLoggedIn: Boolean,
    name: String,
    photo: String?,
    onOpen: () -> Unit,
    onLogout: () -> Unit,
    onAddAccount: () -> Unit,
) {
    val context = LocalContext.current
    val (savedText) = rememberPreference(SavedAccountsKey, "")
    val (currentName) = rememberPreference(AccountNameKey, "")
    val (currentEmail) = rememberPreference(AccountEmailKey, "")
    val (currentSyncId) = rememberPreference(DataSyncIdKey, "")
    val currentKey = "$currentEmail|${currentSyncId.substringBefore("||")}|$currentName"

    var saved by remember { mutableStateOf<List<SavedAccount>>(emptyList()) }
    LaunchedEffect(savedText) { saved = AccountSwitcher.saved(context) }
    val others = saved.count { !(isLoggedIn && it.key == currentKey) }

    var expanded by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "account_arrow")

    // The list is read before it opens, with the arrow turning while it is, so it opens
    // complete instead of opening on a spinner and jumping as the rows arrive.
    val scope = rememberCoroutineScope()
    var panelData by remember { mutableStateOf<AccountsPanelData?>(null) }
    var loadingPanel by remember { mutableStateOf(false) }
    val toggleList: () -> Unit = {
        when {
            expanded -> expanded = false
            loadingPanel -> Unit
            else -> {
                loadingPanel = true
                scope.launch {
                    panelData = loadAccountsPanel(context, isLoggedIn)
                    loadingPanel = false
                    expanded = true
                }
            }
        }
    }

    // Adding starts from an empty sign-in page; otherwise Google sees the
    // account already in use and hands it straight back.
    val addAccount = {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        onAddAccount()
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { if (isLoggedIn) onOpen() else addAccount() }
                        .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Box {
                    if (isLoggedIn && photo != null) {
                        AsyncImage(
                            model = photo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(CircleShape),
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Icon(
                                painter = painterResource(if (isLoggedIn) R.drawable.person else R.drawable.add),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) name else stringResource(R.string.accounts_add_account),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            when {
                                others > 0 -> pluralStringResource(R.plurals.accounts_more_saved, others, others)
                                isLoggedIn -> stringResource(R.string.accounts_tap_to_manage)
                                else -> stringResource(R.string.accounts_sign_in_hint)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (isLoggedIn || others > 0) {
                    IconButton(onClick = toggleList) {
                        if (loadingPanel) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = stringResource(R.string.accounts_and_channels),
                                modifier = Modifier.rotate(turn),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    AccountsPanel(
                        isLoggedIn = isLoggedIn,
                        onAddAccount = onAddAccount,
                        showNote = false,
                        preloaded = panelData,
                    )
                    // Inside the list rather than beside the name, where a button
                    // left room for only the first few letters of it.
                    if (isLoggedIn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onLogout)
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.logout),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp).padding(8.dp),
                            )
                            Text(
                                text = stringResource(R.string.action_logout),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
