/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.blazify.music.BuildConfig
import com.blazify.music.LocalChangelogState
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.component.ReleaseNotesCard
import com.blazify.music.ui.utils.backToMain

private class SettingRow(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val badge: Boolean = false,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo("com.google.android.projection.gearhead", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    val showChangelog = LocalChangelogState.current
    val hasUpdate = BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME
    var query by rememberSaveable { mutableStateOf("") }

    val groups: List<Pair<String, List<SettingRow>>> = listOf(
        stringResource(R.string.settings_group_personalize) to listOf(
            SettingRow(R.drawable.gradient, stringResource(R.string.look_and_feel), stringResource(R.string.look_and_feel_desc)) {
                navController.navigate("settings/appearance/look_and_feel")
            },
            SettingRow(R.drawable.palette, stringResource(R.string.appearance), stringResource(R.string.hint_appearance)) {
                navController.navigate("settings/appearance")
            },
        ),
        stringResource(R.string.settings_group_playback) to listOf(
            SettingRow(R.drawable.play, stringResource(R.string.player_and_audio), stringResource(R.string.hint_player)) {
                navController.navigate("settings/player")
            },
            SettingRow(R.drawable.radio, stringResource(R.string.stream_sources), stringResource(R.string.hint_stream)) {
                navController.navigate("settings/stream_sources")
            },
        ),
        stringResource(R.string.settings_group_content) to buildList {
            add(SettingRow(R.drawable.language, stringResource(R.string.content), stringResource(R.string.hint_content)) {
                navController.navigate("settings/content")
            })
            add(SettingRow(R.drawable.translate, stringResource(R.string.ai_lyrics_translation), stringResource(R.string.hint_lyrics)) {
                navController.navigate("settings/ai")
            })
            if (hasAndroidAuto) {
                add(SettingRow(R.drawable.ic_android_auto, stringResource(R.string.android_auto), stringResource(R.string.hint_android_auto)) {
                    navController.navigate("settings/android_auto")
                })
            }
        },
        stringResource(R.string.settings_group_privacy_data) to listOf(
            SettingRow(R.drawable.security, stringResource(R.string.privacy), stringResource(R.string.hint_privacy)) {
                navController.navigate("settings/privacy")
            },
            SettingRow(R.drawable.storage, stringResource(R.string.storage), stringResource(R.string.hint_storage)) {
                navController.navigate("settings/storage")
            },
            SettingRow(R.drawable.restore, stringResource(R.string.backup_restore), stringResource(R.string.hint_backup)) {
                navController.navigate("settings/backup_restore")
            },
        ),
        stringResource(R.string.settings_group_about) to buildList {
            add(SettingRow(R.drawable.info, stringResource(R.string.about), stringResource(R.string.hint_about), badge = hasUpdate) {
                navController.navigate("settings/about")
            })
            add(SettingRow(R.drawable.newspaper, stringResource(R.string.changelog), stringResource(R.string.hint_changelog)) {
                showChangelog.value = true
            })
            if (BuildConfig.UPDATER_AVAILABLE) {
                add(SettingRow(R.drawable.update, stringResource(R.string.updater), stringResource(R.string.hint_updater)) {
                    navController.navigate("settings/updater")
                })
            }
            if (isAndroid12OrLater) {
                add(SettingRow(R.drawable.link, stringResource(R.string.default_links), stringResource(R.string.hint_links)) {
                    openDefaultLinksSettings(context)
                })
            }
        },
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(8.dp))

        SettingsSearchField(query = query, onQueryChange = { query = it })

        val q = query.trim()
        var chipIndex = 0
        groups.forEach { (groupTitle, rows) ->
            val filtered = if (q.isEmpty()) rows else rows.filter {
                it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
            }
            if (filtered.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = groupTitle.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    filtered.forEachIndexed { i, row ->
                        if (i > 0) {
                            Box(
                                Modifier
                                    .padding(start = 68.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            )
                        }
                        BlazeSettingRow(row = row, colorIndex = chipIndex)
                        chipIndex++
                    }
                }
            }
        }

        if (hasUpdate) {
            Spacer(Modifier.height(18.dp))
            ReleaseNotesCard()
        }

        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

/** Chip background/foreground derived from the app's dynamic theme. */
@Composable
private fun chipColorsAt(index: Int): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> cs.primary to cs.onPrimary
        1 -> cs.secondary to cs.onSecondary
        else -> cs.tertiary to cs.onTertiary
    }
}

@Composable
private fun BlazeSettingRow(row: SettingRow, colorIndex: Int) {
    val (chipBg, chipInk) = chipColorsAt(colorIndex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = row.onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(chipBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(row.icon),
                contentDescription = null,
                tint = chipInk,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.badge) {
                    Spacer(Modifier.width(8.dp))
                    Badge()
                }
            }
            Text(
                text = row.subtitle,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_settings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun openDefaultLinksSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            "package:${context.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, R.string.open_app_settings_error, Toast.LENGTH_LONG).show()
    }
}
