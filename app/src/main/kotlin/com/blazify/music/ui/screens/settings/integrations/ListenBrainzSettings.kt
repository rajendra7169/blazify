package com.blazify.music.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.EnableListenBrainzKey
import com.blazify.music.constants.ListenBrainzTokenKey
import com.blazify.music.constants.ListenBrainzUsernameKey
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.component.Material3SettingsGroup
import com.blazify.music.ui.component.Material3SettingsItem
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.ListenBrainz
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The listening history that belongs to the listener.
 *
 * ListenBrainz asks for nothing but a token from its own settings page, so this screen is that
 * one field, a check that the token is real, and a switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenBrainzSettings(navController: NavController) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val (enabled, onEnabledChange) = rememberPreference(EnableListenBrainzKey, defaultValue = false)
    val (token, onTokenChange) = rememberPreference(ListenBrainzTokenKey, defaultValue = "")
    val (userName, onUserNameChange) = rememberPreference(ListenBrainzUsernameKey, defaultValue = "")

    var field by remember { mutableStateOf(TextFieldValue(token)) }
    var checking by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Material3SettingsGroup(
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.music_note),
                        title = { Text(stringResource(R.string.listenbrainz_scrobbling)) },
                        description = {
                            Text(
                                if (userName.isNotEmpty()) {
                                    stringResource(R.string.listenbrainz_signed_in, userName)
                                } else {
                                    stringResource(R.string.listenbrainz_desc)
                                },
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enabled,
                                onCheckedChange = onEnabledChange,
                                enabled = token.isNotEmpty(),
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (enabled) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { if (token.isNotEmpty()) onEnabledChange(!enabled) },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        TextField(
            value = field,
            onValueChange = {
                field = it
                refused = false
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(),
            label = { Text(stringResource(R.string.listenbrainz_token)) },
            isError = refused,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text =
                when {
                    checking -> stringResource(R.string.listenbrainz_checking)
                    refused -> stringResource(R.string.listenbrainz_refused)
                    else -> stringResource(R.string.listenbrainz_token_hint)
                },
            style = MaterialTheme.typography.bodySmall,
            color =
                if (refused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Material3SettingsGroup(
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.check),
                        title = { Text(stringResource(R.string.listenbrainz_save)) },
                        description = { Text(stringResource(R.string.listenbrainz_save_desc)) },
                        onClick = {
                            val typed = field.text.trim()
                            if (typed.isEmpty() || checking) return@Material3SettingsItem
                            checking = true
                            refused = false
                            scope.launch(Dispatchers.IO) {
                                val name = ListenBrainz.userName(typed)
                                checking = false
                                if (name == null) {
                                    refused = true
                                } else {
                                    onTokenChange(typed)
                                    onUserNameChange(name)
                                    onEnabledChange(true)
                                }
                            }
                        },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.link),
                        title = { Text(stringResource(R.string.listenbrainz_get_token)) },
                        description = { Text(stringResource(R.string.listenbrainz_get_token_desc)) },
                        onClick = { uriHandler.openUri("https://listenbrainz.org/settings/") },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.logout),
                        title = { Text(stringResource(R.string.listenbrainz_forget)) },
                        description = { Text(stringResource(R.string.listenbrainz_forget_desc)) },
                        onClick = {
                            field = TextFieldValue("")
                            onTokenChange("")
                            onUserNameChange("")
                            onEnabledChange(false)
                        },
                    ),
                ),
        )

        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.listenbrainz_integration)) },
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
