/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.blazify.music.ui.player.MiniPlayerDesign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.CropAlbumArtDefault
import com.blazify.music.constants.CropAlbumArtKey
import com.blazify.music.constants.DensityScale
import com.blazify.music.constants.DensityScaleKey
import com.blazify.music.constants.DynamicThemeKey
import com.blazify.music.constants.EnableHighRefreshRateKey
import com.blazify.music.constants.EnableLandscapeScalingKey
import com.blazify.music.constants.HidePlayerThumbnailKey
import com.blazify.music.constants.ListenTogetherInTopBarKey
import com.blazify.music.constants.MiniPlayerBackgroundStyle
import com.blazify.music.constants.MiniPlayerBackgroundStyleKey
import com.blazify.music.constants.PlayerBackgroundStyle
import com.blazify.music.constants.PlayerBackgroundStyleKey
import com.blazify.music.constants.PlayerButtonsStyle
import com.blazify.music.constants.PlayerButtonsStyleKey
import com.blazify.music.constants.SelectedThemeColorKey
import com.blazify.music.constants.ShowCachedPlaylistKey
import com.blazify.music.constants.ShowDownloadedPlaylistKey
import com.blazify.music.constants.ShowLikedPlaylistKey
import com.blazify.music.constants.ShowTopPlaylistKey
import com.blazify.music.constants.ShowUploadedPlaylistKey
import com.blazify.music.constants.SwipeSensitivityKey
import com.blazify.music.constants.SwipeThumbnailKey
import com.blazify.music.constants.SwipeToRemoveSongKey
import com.blazify.music.constants.SwipeToSongKey
import com.blazify.music.ui.component.DefaultDialog
import com.blazify.music.ui.component.EnumDialog
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.component.Material3SettingsGroup
import com.blazify.music.ui.component.Material3SettingsItem
import com.blazify.music.ui.theme.DefaultThemeColor
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    val (dynamicTheme, onDynamicThemeChange) =
        rememberPreference(
            DynamicThemeKey,
            defaultValue = true,
        )
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) =
        rememberPreference(
            EnableHighRefreshRateKey,
            defaultValue = true,
        )
    val (enableLandscapeScaling, onEnableLandscapeScalingChange) =
        rememberPreference(
            EnableLandscapeScalingKey,
            defaultValue = false,
        )
    val (selectedThemeColorInt) =
        rememberPreference(
            SelectedThemeColorKey,
            defaultValue = DefaultThemeColor.toArgb(),
        )
    // Check if user has selected a custom color (not the default/dynamic color)
    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()

    // Mini-player design and background, slider style, default tab, grid size and
    // slim navbar are set in Look & Feel, which has a live preview for them.
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(
            HidePlayerThumbnailKey,
            defaultValue = false,
        )
    val (cropAlbumArt, onCropAlbumArtChange) =
        rememberPreference(
            CropAlbumArtKey,
            defaultValue = CropAlbumArtDefault,
        )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.GRADIENT,
        )

    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(
            PlayerButtonsStyleKey,
            defaultValue = PlayerButtonsStyle.DEFAULT,
        )
    // Lyrics settings moved to the dedicated Lyrics screen (settings/lyrics).

    val (swipeThumbnail, onSwipeThumbnailChange) =
        rememberPreference(
            SwipeThumbnailKey,
            defaultValue = true,
        )
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(
            SwipeSensitivityKey,
            defaultValue = 0.73f,
        )

    // Density scale preferences
    val context = activity as Context
    val sharedPreferences = remember { context.getSharedPreferences("blazify_settings", Context.MODE_PRIVATE) }
    val prefDensityScale =
        remember(sharedPreferences) {
            sharedPreferences.getFloat("density_scale_factor", 1.0f)
        }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, defaultValue = prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        // Write to SharedPreferences for DensityScaler to read on next startup
        sharedPreferences.edit {
            putFloat("density_scale_factor", newScale)
        }
        showRestartDialog = true
    }

    val (listenTogetherInTopBar, onListenTogetherInTopBarChange) =
        rememberPreference(
            ListenTogetherInTopBarKey,
            defaultValue = true,
        )

    val (swipeToSong, onSwipeToSongChange) =
        rememberPreference(
            SwipeToSongKey,
            defaultValue = true,
        )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) =
        rememberPreference(
            SwipeToRemoveSongKey,
            defaultValue = false,
        )

    val (showLikedPlaylist, onShowLikedPlaylistChange) =
        rememberPreference(
            ShowLikedPlaylistKey,
            defaultValue = true,
        )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) =
        rememberPreference(
            ShowDownloadedPlaylistKey,
            defaultValue = true,
        )
    val (showTopPlaylist, onShowTopPlaylistChange) =
        rememberPreference(
            ShowTopPlaylistKey,
            defaultValue = true,
        )
    val (showCachedPlaylist, onShowCachedPlaylistChange) =
        rememberPreference(
            ShowCachedPlaylistKey,
            defaultValue = true,
        )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) =
        rememberPreference(
            ShowUploadedPlaylistKey,
            defaultValue = true,
        )

    val availableBackgroundStyles =
        PlayerBackgroundStyle.entries.filter {
            it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

    var showPlayerBackgroundDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerButtonsStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Lyrics dialogs moved to the dedicated Lyrics screen (settings/lyrics).

    if (showPlayerButtonsStyleDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            onSelect = {
                onPlayerButtonsStyleChange(it)
                showPlayerButtonsStyleDialog = false
            },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                }
            },
        )
    }

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = {
                onPlayerBackgroundChange(it)
                showPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = availableBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                }
            },
        )
    }

    if (showRestartDialog) {
        DefaultDialog(
            onDismiss = { showRestartDialog = false },
            buttons = {
                TextButton(
                    onClick = { showRestartDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        val intent =
                            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    },
                ) {
                    Text(text = stringResource(R.string.restart))
                }
            },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.restart_required),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.density_restart_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showDensityScaleDialog) {
        DefaultDialog(
            onDismiss = { showDensityScaleDialog = false },
            buttons = {
                TextButton(
                    onClick = { showDensityScaleDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        ) {
            Column {
                DensityScale.entries.forEach { scale ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDensityScaleChange(scale.value)
                                    showDensityScaleDialog = false
                                }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = scale.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (densityScale == scale.value) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    }
                }
            }
        }
    }


    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.theme),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.speed),
                            title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                            description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableHighRefreshRate,
                                    onCheckedChange = onEnableHighRefreshRateChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableHighRefreshRate) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.fullscreen),
                            title = { Text(stringResource(R.string.enable_landscape_scaling)) },
                            description = { Text(stringResource(R.string.enable_landscape_scaling_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableLandscapeScaling,
                                    onCheckedChange = onEnableLandscapeScalingChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableLandscapeScaling) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableLandscapeScalingChange(!enableLandscapeScaling) },
                        ),
                    )
                    // Only show dynamic theme option when using the default/dynamic color
                    // When a custom color is selected, dynamic theme is automatically disabled
                    if (!isUsingCustomColor) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.palette),
                                title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                                description = { Text(stringResource(R.string.enable_dynamic_theme_desc)) },
                                trailingContent = {
                                    Switch(
                                        checked = dynamicTheme,
                                        onCheckedChange = onDynamicThemeChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (dynamicTheme) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onDynamicThemeChange(!dynamicTheme) },
                            ),
                        )
                    }
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.gradient),
                        title = { Text(stringResource(R.string.player_background_style)) },
                        description = {
                            Text(
                                when (playerBackground) {
                                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                },
                            )
                        },
                        onClick = { showPlayerBackgroundDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.hide_image),
                        title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                        description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hidePlayerThumbnail,
                                onCheckedChange = onHidePlayerThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (hidePlayerThumbnail) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.crop),
                        title = { Text(stringResource(R.string.crop_album_art)) },
                        description = { Text(stringResource(R.string.crop_album_art_desc)) },
                        trailingContent = {
                            Switch(
                                checked = cropAlbumArt,
                                onCheckedChange = onCropAlbumArtChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (cropAlbumArt) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onCropAlbumArtChange(!cropAlbumArt) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.player_buttons_style)) },
                        description = {
                            Text(
                                when (playerButtonsStyle) {
                                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                                },
                            )
                        },
                        onClick = { showPlayerButtonsStyleDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                        description = { Text(stringResource(R.string.enable_swipe_thumbnail_desc)) },
                        trailingContent = {
                            Switch(
                                checked = swipeThumbnail,
                                onCheckedChange = onSwipeThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeThumbnail) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeThumbnailChange(!swipeThumbnail) },
                    ),
                ) +
                    if (swipeThumbnail) {
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.tune),
                                title = { Text(stringResource(R.string.swipe_sensitivity)) },
                                description = {
                                    Text(
                                        stringResource(
                                            R.string.sensitivity_percentage,
                                            (swipeSensitivity * 100).roundToInt(),
                                        ),
                                    )
                                },
                                onClick = { showSensitivityDialog = true },
                            ),
                        )
                    } else {
                        emptyList()
                    },
        )

        if (showSensitivityDialog) {
            var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

            DefaultDialog(
                onDismiss = {
                    tempSensitivity = swipeSensitivity
                    showSensitivityDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempSensitivity = 0.73f
                        },
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempSensitivity = swipeSensitivity
                            showSensitivityDialog = false
                        },
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onSwipeSensitivityChange(tempSensitivity)
                            showSensitivityDialog = false
                        },
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.swipe_sensitivity),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Text(
                        text =
                            stringResource(
                                R.string.sensitivity_percentage,
                                (tempSensitivity * 100).roundToInt(),
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Slider(
                        value = tempSensitivity,
                        onValueChange = { tempSensitivity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.swipe_song_to_add)) },
                        description = { Text(stringResource(R.string.swipe_song_to_add_desc)) },
                        trailingContent = {
                            Switch(
                                checked = swipeToSong,
                                onCheckedChange = onSwipeToSongChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeToSong) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeToSongChange(!swipeToSong) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                        description = { Text(stringResource(R.string.swipe_song_to_remove_desc)) },
                        trailingContent = {
                            Switch(
                                checked = swipeToRemoveSong,
                                onCheckedChange = onSwipeToRemoveSongChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeToRemoveSong) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.group_outlined),
                        title = { Text(stringResource(R.string.listen_together_in_top_bar)) },
                        description = { Text(stringResource(R.string.listen_together_in_top_bar_desc)) },
                        trailingContent = {
                            Switch(
                                checked = listenTogetherInTopBar,
                                onCheckedChange = onListenTogetherInTopBarChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (listenTogetherInTopBar) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onListenTogetherInTopBarChange(!listenTogetherInTopBar) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(stringResource(R.string.display_density)) },
                        description = {
                            Text(DensityScale.fromValue(densityScale).label)
                        },
                        onClick = { showDensityScaleDialog = true },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.auto_playlists),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.favorite),
                        title = { Text(stringResource(R.string.show_liked_playlist)) },
                        description = { Text(stringResource(R.string.show_liked_playlist_desc)) },
                        trailingContent = {
                            Switch(
                                checked = showLikedPlaylist,
                                onCheckedChange = onShowLikedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showLikedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.offline),
                        title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                        description = { Text(stringResource(R.string.show_downloaded_playlist_desc)) },
                        trailingContent = {
                            Switch(
                                checked = showDownloadedPlaylist,
                                onCheckedChange = onShowDownloadedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showDownloadedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.trending_up),
                        title = { Text(stringResource(R.string.show_top_playlist)) },
                        description = { Text(stringResource(R.string.show_top_playlist_desc)) },
                        trailingContent = {
                            Switch(
                                checked = showTopPlaylist,
                                onCheckedChange = onShowTopPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showTopPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowTopPlaylistChange(!showTopPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.cached),
                        title = { Text(stringResource(R.string.show_cached_playlist)) },
                        description = { Text(stringResource(R.string.show_cached_playlist_desc)) },
                        trailingContent = {
                            Switch(
                                checked = showCachedPlaylist,
                                onCheckedChange = onShowCachedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showCachedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.backup),
                        title = { Text(stringResource(R.string.show_uploaded_playlist)) },
                        description = { Text(stringResource(R.string.show_uploaded_playlist_desc)) },
                        trailingContent = {
                            Switch(
                                checked = showUploadedPlaylist,
                                onCheckedChange = onShowUploadedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showUploadedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowUploadedPlaylistChange(!showUploadedPlaylist) },
                    ),
                ),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
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

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

/* ------------------------------------------------------------------------- */
/* Mini-player design picker: non-interactive preview cards, 2 per row.       */
/* ------------------------------------------------------------------------- */

@Composable
internal fun MiniPlayerDesignPicker(
    selected: MiniPlayerDesign,
    onSelect: (MiniPlayerDesign) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.mini_player_design),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.mini_player_design_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        // Two designs per row for tidy, consistent space use.
        MiniPlayerDesign.entries.chunked(2).forEach { rowDesigns ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                rowDesigns.forEach { d ->
                    MiniPlayerDesignCard(
                        design = d,
                        selected = d == selected,
                        onClick = { onSelect(d) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowDesigns.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniPlayerDesignCard(
    design: MiniPlayerDesign,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            MiniPlayerDesignPreview(design)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(design.nameRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Lightweight SKELETON preview of a mini-player design for the picker card:
 * placeholder art + title/subtitle bars laid out exactly like the real
 * mini-player, with each design's real control icons and shape. No image
 * loading or palette work (keeps the picker light); the chosen background
 * style is reflected representatively. Non-interactive (the card selects).
 */
@Composable
private fun MiniPlayerDesignPreview(design: MiniPlayerDesign) {
    val cs = MaterialTheme.colorScheme
    // Reflect the chosen mini-player background style in the preview.
    val bgStyle by rememberEnumPreference(MiniPlayerBackgroundStyleKey, MiniPlayerBackgroundStyle.GRADIENT)
    val lightText = bgStyle == MiniPlayerBackgroundStyle.GRADIENT || bgStyle == MiniPlayerBackgroundStyle.BLUR
    val onColor = if (lightText) Color.White else cs.onSurface
    val barFill = onColor.copy(alpha = 0.32f)   // skeleton title/subtitle lines
    val artFill = onColor.copy(alpha = 0.22f)   // skeleton album-art placeholder

    val barShape = when (design) {
        MiniPlayerDesign.FLAT -> RoundedCornerShape(6.dp)
        MiniPlayerDesign.FLOATING -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(50)
    }
    val artShape =
        if (design == MiniPlayerDesign.FLAT || design == MiniPlayerDesign.FLOATING) RoundedCornerShape(6.dp) else CircleShape

    // Representative background per style (theme-based, no album art needed).
    val bgMod = when (bgStyle) {
        MiniPlayerBackgroundStyle.GRADIENT ->
            Modifier.background(Brush.horizontalGradient(listOf(cs.primary, cs.primary.copy(alpha = 0.62f))))
        MiniPlayerBackgroundStyle.BLUR -> Modifier.background(cs.surfaceVariant)
        else -> Modifier.background(cs.surfaceContainerHighest)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(if (design == MiniPlayerDesign.FLOATING) 0.9f else 1f)
            .then(if (design == MiniPlayerDesign.FLOATING) Modifier.shadow(6.dp, barShape, clip = false) else Modifier)
            .height(44.dp)
            .clip(barShape)
            .then(bgMod),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Subtle overlay hints at a blurred art background.
        if (bgStyle == MiniPlayerBackgroundStyle.BLUR) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp),
        ) {
            Box(Modifier.size(28.dp).clip(artShape).background(artFill))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth(0.72f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(barFill))
                Box(Modifier.fillMaxWidth(0.46f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(barFill.copy(alpha = 0.7f)))
            }
            Spacer(Modifier.width(6.dp))
            when (design) {
                MiniPlayerDesign.ROUNDED -> {
                    MockGlyph(R.drawable.skip_previous, onColor, 12)
                    Spacer(Modifier.width(2.dp))
                    Box(
                        Modifier.size(18.dp).clip(CircleShape).background(onColor.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center,
                    ) { MockGlyph(R.drawable.play, Color.Black, 11) }
                    Spacer(Modifier.width(2.dp))
                    MockGlyph(R.drawable.skip_next, onColor, 12)
                }
                MiniPlayerDesign.FLAT -> {
                    MockGlyph(R.drawable.favorite_border, onColor, 13)
                }
                else -> {
                    MockGlyph(R.drawable.playlist_add, onColor.copy(alpha = 0.9f), 13)
                    Spacer(Modifier.width(6.dp))
                    MockGlyph(R.drawable.favorite_border, onColor.copy(alpha = 0.9f), 13)
                }
            }
        }
        if (design == MiniPlayerDesign.FLAT) {
            // Flat design shows a thin progress bar along the top edge.
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.42f)
                    .height(2.dp)
                    .background(onColor),
            )
        }
    }
}

@Composable
private fun MockGlyph(res: Int, tint: Color, size: Int = 12) {
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size.dp),
    )
}
