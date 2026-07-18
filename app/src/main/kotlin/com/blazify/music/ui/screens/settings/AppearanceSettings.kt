/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.blazify.music.ui.player.MiniPlayerDesign
import com.blazify.music.constants.MiniPlayerDesignKey
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.models.MediaMetadata
import com.blazify.music.ui.theme.PlayerColorExtractor
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.ChipSortTypeKey
import com.blazify.music.constants.CropAlbumArtKey
import com.blazify.music.constants.DefaultOpenTabKey
import com.blazify.music.constants.DensityScale
import com.blazify.music.constants.DensityScaleKey
import com.blazify.music.constants.DynamicThemeKey
import com.blazify.music.constants.EnableDynamicIconKey
import com.blazify.music.constants.EnableHighRefreshRateKey
import com.blazify.music.constants.EnableLandscapeScalingKey
import com.blazify.music.constants.ExperimentalLyricsKey
import com.blazify.music.constants.GridItemSize
import com.blazify.music.constants.GridItemsSizeKey
import com.blazify.music.constants.HidePlayerThumbnailKey
import com.blazify.music.constants.HideStatusBarOnFullscreenKey
import com.blazify.music.constants.LibraryFilter
import com.blazify.music.constants.ListenTogetherInTopBarKey
import com.blazify.music.constants.LyricsAnimationStyle
import com.blazify.music.constants.LyricsAnimationStyleKey
import com.blazify.music.constants.LyricsClickKey
import com.blazify.music.constants.LyricsGlowEffectKey
import com.blazify.music.constants.LyricsLineSpacingKey
import com.blazify.music.constants.LyricsScrollKey
import com.blazify.music.constants.LyricsTextPositionKey
import com.blazify.music.constants.LyricsTextSizeKey
import com.blazify.music.constants.MiniPlayerBackgroundStyle
import com.blazify.music.constants.MiniPlayerBackgroundStyleKey
import com.blazify.music.constants.PlayerBackgroundStyle
import com.blazify.music.constants.PlayerBackgroundStyleKey
import com.blazify.music.constants.PlayerButtonsStyle
import com.blazify.music.constants.PlayerButtonsStyleKey
import com.blazify.music.constants.PureBlackMiniPlayerKey
import com.blazify.music.constants.RespectAgentPositioningKey
import com.blazify.music.constants.SelectedThemeColorKey
import com.blazify.music.constants.ShowCachedPlaylistKey
import com.blazify.music.constants.ShowDownloadedPlaylistKey
import com.blazify.music.constants.ShowLikedPlaylistKey
import com.blazify.music.constants.ShowTopPlaylistKey
import com.blazify.music.constants.ShowUploadedPlaylistKey
import com.blazify.music.constants.SliderStyle
import com.blazify.music.constants.SliderStyleKey
import com.blazify.music.constants.SlimNavBarKey
import com.blazify.music.constants.SquigglySliderKey
import com.blazify.music.constants.SwipeSensitivityKey
import com.blazify.music.constants.SwipeThumbnailKey
import com.blazify.music.constants.SwipeToRemoveSongKey
import com.blazify.music.constants.SwipeToSongKey
import com.blazify.music.constants.UseNewMiniPlayerDesignKey
import com.blazify.music.constants.UseNewPlayerDesignKey
import com.blazify.music.ui.component.DefaultDialog
import com.blazify.music.ui.component.EnumDialog
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.component.Material3SettingsGroup
import com.blazify.music.ui.component.Material3SettingsItem
import com.blazify.music.ui.component.PlayerSliderTrack
import com.blazify.music.ui.component.SquigglySlider
import com.blazify.music.ui.component.WavySlider
import com.blazify.music.ui.theme.DefaultThemeColor
import com.blazify.music.ui.theme.PlayerSliderColors
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.IconUtils
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.launch
import java.util.Locale
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
    val (enableDynamicIcon, onEnableDynamicIconPrefChange) =
        rememberPreference(
            EnableDynamicIconKey,
            defaultValue = true,
        )
    val iconContext = LocalContext.current
    val onEnableDynamicIconChange: (Boolean) -> Unit = { newValue ->
        onEnableDynamicIconPrefChange(newValue)
        IconUtils.setIcon(iconContext, newValue)
    }
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

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = false,
        )
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) =
        rememberEnumPreference(
            MiniPlayerBackgroundStyleKey,
            defaultValue = MiniPlayerBackgroundStyle.GRADIENT,
        )

    val availableMiniPlayerBackgroundStyles =
        MiniPlayerBackgroundStyle.entries.filter {
            it != MiniPlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }

    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) =
        rememberPreference(
            UseNewMiniPlayerDesignKey,
            defaultValue = true,
        )
    val (miniPlayerDesignId, onMiniPlayerDesignChange) =
        rememberPreference(MiniPlayerDesignKey, defaultValue = "")
    val selectedMiniPlayerDesign =
        remember(miniPlayerDesignId, useNewMiniPlayerDesign) {
            if (miniPlayerDesignId.isBlank()) {
                if (useNewMiniPlayerDesign) MiniPlayerDesign.MODERN else MiniPlayerDesign.FLAT
            } else {
                MiniPlayerDesign.fromId(miniPlayerDesignId)
            }
        }
    // The album-art background style only applies to the non-flat designs.
    val miniPlayerUsesArtBackground = selectedMiniPlayerDesign != MiniPlayerDesign.FLAT
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(
            HidePlayerThumbnailKey,
            defaultValue = false,
        )
    val (cropAlbumArt, onCropAlbumArtChange) =
        rememberPreference(
            CropAlbumArtKey,
            defaultValue = false,
        )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.GRADIENT,
        )

    val (defaultOpenTab, onDefaultOpenTabChange) =
        rememberEnumPreference(
            DefaultOpenTabKey,
            defaultValue = NavigationTab.HOME,
        )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(
            PlayerButtonsStyleKey,
            defaultValue = PlayerButtonsStyle.DEFAULT,
        )
    val (lyricsPosition, onLyricsPositionChange) =
        rememberEnumPreference(
            LyricsTextPositionKey,
            defaultValue = LyricsPosition.CENTER,
        )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) =
        rememberPreference(
            LyricsScrollKey,
            defaultValue = true,
        )
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) =
        rememberPreference(
            HideStatusBarOnFullscreenKey,
            defaultValue = false,
        )
    val (respectAgentPositioning, onRespectAgentPositioningChange) = rememberPreference(RespectAgentPositioningKey, defaultValue = true)
    val (experimentalLyrics, onExperimentalLyricsChange) = rememberPreference(ExperimentalLyricsKey, defaultValue = true)

    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) =
        rememberEnumPreference(
            LyricsAnimationStyleKey,
            defaultValue = LyricsAnimationStyle.FADE,
        )
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.2f)

    var showExperimentalLyricsBetaDialog by remember { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }

    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(
            SliderStyleKey,
            defaultValue = SliderStyle.SLIM,
        )
    val (squigglySlider, onSquigglySliderChange) =
        rememberPreference(
            SquigglySliderKey,
            defaultValue = false,
        )
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
    val (gridItemSize, onGridItemSizeChange) =
        rememberEnumPreference(
            GridItemsSizeKey,
            defaultValue = GridItemSize.SMALL,
        )

    val (slimNav, onSlimNavChange) =
        rememberPreference(
            SlimNavBarKey,
            defaultValue = false,
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
            defaultValue = false,
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

    val (defaultChip, onDefaultChipChange) =
        rememberEnumPreference(
            key = ChipSortTypeKey,
            defaultValue = LibraryFilter.LIBRARY,
        )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerBackgroundDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerButtonsStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsPositionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style_title),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                }
            },
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }

        DefaultDialog(
            onDismiss = {
                tempTextSize = lyricsTextSize
                showLyricsTextSizeDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempTextSize = 24f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempTextSize = lyricsTextSize
                        showLyricsTextSizeDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsTextSizeChange(tempTextSize)
                        showLyricsTextSizeDialog = false
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
                    text = stringResource(R.string.lyrics_text_size),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = "${tempTextSize.roundToInt()} sp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Slider(
                    value = tempTextSize,
                    onValueChange = { tempTextSize = it },
                    valueRange = 12f..48f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }

        DefaultDialog(
            onDismiss = {
                tempLineSpacing = lyricsLineSpacing
                showLyricsLineSpacingDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempLineSpacing = 1.3f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempLineSpacing = lyricsLineSpacing
                        showLyricsLineSpacingDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsLineSpacingChange(tempLineSpacing)
                        showLyricsLineSpacingDialog = false
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
                    text = stringResource(R.string.lyrics_line_spacing),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = String.format(Locale.US, "%.1f", tempLineSpacing),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Slider(
                    value = tempLineSpacing,
                    onValueChange = { tempLineSpacing = it },
                    valueRange = 1.0f..3.0f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

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

    if (showMiniPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = {
                onMiniPlayerBackgroundChange(it)
                showMiniPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.mini_player_background_style),
            current = miniPlayerBackground,
            values = availableMiniPlayerBackgroundStyles,
            valueText = {
                when (it) {
                    MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                    MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                }
            },
        )
    }

    var showDefaultOpenTabDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = {
                onDefaultOpenTabChange(it)
                showDefaultOpenTabDialog = false
            },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )
    }

    var showDefaultChipDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = {
                onDefaultChipChange(it)
                showDefaultChipDialog = false
            },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.values().toList(),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )
    }

    var showGridSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = {
                onGridItemSizeChange(it)
                showGridSizeDialog = false
            },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.values().toList(),
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
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

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            },
        ) {
            val sliderPreviewColors =
                PlayerSliderColors.getSliderColors(
                    MaterialTheme.colorScheme.primary,
                    PlayerBackgroundStyle.DEFAULT,
                    isSystemInDarkTheme(),
                )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle == SliderStyle.DEFAULT &&
                                        !squigglySlider
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.DEFAULT)
                                    onSquigglySliderChange(false)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderValue = 0.35f
                        Slider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            colors = sliderPreviewColors,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.default_),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle == SliderStyle.WAVY &&
                                        !squigglySlider
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.WAVY)
                                    onSquigglySliderChange(false)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderValue = 0.5f
                        WavySlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            colors = sliderPreviewColors,
                            modifier = Modifier.weight(1f),
                            isPlaying = true,
                            enabled = false,
                        )
                        Text(
                            text = stringResource(R.string.wavy),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle ==
                                        SliderStyle.SLIM
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.SLIM)
                                    onSquigglySliderChange(false)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderValue = 0.65f
                        Slider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = sliderPreviewColors,
                                )
                            },
                            colors = sliderPreviewColors,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                        )

                        Text(
                            text = stringResource(R.string.slim),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle == SliderStyle.WAVY &&
                                        squigglySlider
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.WAVY)
                                    onSquigglySliderChange(true)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderValue = 0.5f
                        SquigglySlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            colors = sliderPreviewColors,
                            isPlaying = true,
                        )
                        Text(
                            text = stringResource(R.string.squiggly),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
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
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.enable_dynamic_icon)) },
                            description = { Text(stringResource(R.string.enable_dynamic_icon_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableDynamicIcon,
                                    onCheckedChange = onEnableDynamicIconChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableDynamicIcon) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableDynamicIconChange(!enableDynamicIcon) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.theme)) },
                            description = { Text(stringResource(R.string.theme_desc)) },
                            onClick = { navController.navigate("settings/appearance/theme") },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        val (pureBlackMiniPlayer, onPureBlackMiniPlayerChange) =
            rememberPreference(
                PureBlackMiniPlayerKey,
                defaultValue = false,
            )

        MiniPlayerDesignPicker(
            selected = selectedMiniPlayerDesign,
            onSelect = { onMiniPlayerDesignChange(it.id) },
        )

        Spacer(modifier = Modifier.height(20.dp))

        Material3SettingsGroup(
            title = stringResource(id = R.string.mini_player),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.gradient),
                            title = {
                                Text(
                                    text = stringResource(R.string.mini_player_background_style),
                                    color =
                                        if (!miniPlayerUsesArtBackground) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                )
                            },
                            description = {
                                Text(
                                    text =
                                        if (!miniPlayerUsesArtBackground) {
                                            stringResource(R.string.mini_player_background_not_available)
                                        } else {
                                            when (miniPlayerBackground) {
                                                MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                                MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                                                MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                                MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                                MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                                            }
                                        },
                                    color =
                                        if (!miniPlayerUsesArtBackground) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            },
                            onClick = { if (miniPlayerUsesArtBackground) showMiniPlayerBackgroundDialog = true },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.new_player_design)) },
                        description = { Text(stringResource(R.string.new_player_design_desc)) },
                        trailingContent = {
                            Switch(
                                checked = useNewPlayerDesign,
                                onCheckedChange = onUseNewPlayerDesignChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (useNewPlayerDesign) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onUseNewPlayerDesignChange(!useNewPlayerDesign) },
                    ),
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
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.player_slider_style)) },
                        description = {
                            Text(
                                when (sliderStyle) {
                                    SliderStyle.DEFAULT -> {
                                        stringResource(R.string.default_)
                                    }

                                    SliderStyle.WAVY -> {
                                        if (squigglySlider) {
                                            stringResource(R.string.squiggly)
                                        } else {
                                            stringResource(
                                                R.string.wavy,
                                            )
                                        }
                                    }

                                    SliderStyle.SLIM -> {
                                        stringResource(R.string.slim)
                                    }
                                },
                            )
                        },
                        onClick = { showSliderOptionDialog = true },
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
            title = stringResource(R.string.lyrics),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.experimental_lyrics)) },
                            description = { Text(stringResource(R.string.experimental_lyrics_desc)) },
                            showBadge = true,
                            trailingContent = {
                                Switch(
                                    checked = experimentalLyrics,
                                    onCheckedChange = {
                                        if (!experimentalLyrics) {
                                            showExperimentalLyricsBetaDialog = true
                                        } else {
                                            onExperimentalLyricsChange(false)
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (experimentalLyrics) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = {
                                if (!experimentalLyrics) {
                                    showExperimentalLyricsBetaDialog = true
                                } else {
                                    onExperimentalLyricsChange(false)
                                }
                            },
                        ),
                    )

                    if (!experimentalLyrics) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                                description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                                trailingContent = {
                                    Switch(
                                        checked = lyricsGlowEffect,
                                        onCheckedChange = onLyricsGlowEffectChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (lyricsGlowEffect) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_animation_style_title)) },
                                description = {
                                    Text(
                                        when (lyricsAnimationStyle) {
                                            LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                                            LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                                            LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                                            LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                                            LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                                            LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                                        },
                                    )
                                },
                                onClick = { showLyricsAnimationStyleDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_text_size)) },
                                description = { Text("${lyricsTextSize.roundToInt()} sp") },
                                onClick = { showLyricsTextSizeDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                                description = { Text(String.format(Locale.US, "%.1f", lyricsLineSpacing)) },
                                onClick = { showLyricsLineSpacingDialog = true },
                            ),
                        )
                    }

                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_text_position)) },
                            description = {
                                Text(
                                    when (lyricsPosition) {
                                        LyricsPosition.LEFT -> stringResource(R.string.left)
                                        LyricsPosition.CENTER -> stringResource(R.string.center)
                                        LyricsPosition.RIGHT -> stringResource(R.string.right)
                                    },
                                )
                            },
                            onClick = { showLyricsPositionDialog = true },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.respect_agent_positioning)) },
                            description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = respectAgentPositioning,
                                    onCheckedChange = onRespectAgentPositioningChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (respectAgentPositioning) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onRespectAgentPositioningChange(!respectAgentPositioning) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_click_change)) },
                            description = { Text(stringResource(R.string.lyrics_click_change_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = lyricsClick,
                                    onCheckedChange = onLyricsClickChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (lyricsClick) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onLyricsClickChange(!lyricsClick) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                            description = { Text(stringResource(R.string.lyrics_auto_scroll_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = lyricsScroll,
                                    onCheckedChange = onLyricsScrollChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (lyricsScroll) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onLyricsScrollChange(!lyricsScroll) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.hide_status_bar_fullscreen)) },
                            description = { Text(stringResource(R.string.hide_status_bar_fullscreen_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = hideStatusBarOnFullscreen,
                                    onCheckedChange = onHideStatusBarOnFullscreenChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (hideStatusBarOnFullscreen) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onHideStatusBarOnFullscreenChange(!hideStatusBarOnFullscreen) },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.default_open_tab)) },
                        description = {
                            Text(
                                when (defaultOpenTab) {
                                    NavigationTab.HOME -> stringResource(R.string.home)
                                    NavigationTab.SEARCH -> stringResource(R.string.search)
                                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                },
                            )
                        },
                        onClick = { showDefaultOpenTabDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tab),
                        title = { Text(stringResource(R.string.default_lib_chips)) },
                        description = {
                            Text(
                                when (defaultChip) {
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                },
                            )
                        },
                        onClick = { showDefaultChipDialog = true },
                    ),
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
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.slim_navbar)) },
                        description = { Text(stringResource(R.string.slim_navbar_desc)) },
                        trailingContent = {
                            Switch(
                                checked = slimNav,
                                onCheckedChange = onSlimNavChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (slimNav) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSlimNavChange(!slimNav) },
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
                        title = { Text(stringResource(R.string.grid_cell_size)) },
                        description = {
                            Text(
                                when (gridItemSize) {
                                    GridItemSize.BIG -> stringResource(R.string.big)
                                    GridItemSize.SMALL -> stringResource(R.string.small)
                                },
                            )
                        },
                        onClick = { showGridSizeDialog = true },
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

        if (showExperimentalLyricsBetaDialog) {
            DefaultDialog(
                onDismiss = { showExperimentalLyricsBetaDialog = false },
                title = { Text(stringResource(R.string.experimental_lyrics_beta_title)) },
                buttons = {
                    TextButton(onClick = { showExperimentalLyricsBetaDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        showExperimentalLyricsBetaDialog = false
                        onExperimentalLyricsChange(true)
                    }) {
                        Text(stringResource(R.string.enable))
                    }
                },
            ) {
                Text(stringResource(R.string.experimental_lyrics_beta_message))
            }
        }
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
private fun MiniPlayerDesignPicker(
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
        // One design per full-width row.
        MiniPlayerDesign.entries.forEach { d ->
            MiniPlayerDesignCard(
                design = d,
                selected = d == selected,
                onClick = { onSelect(d) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
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
 * Compact but REAL preview of a mini-player design for the picker card: the
 * currently-playing song's art, title and artist over the album-art gradient,
 * with each design's real control icons. Non-interactive (the card selects).
 */
@Composable
private fun MiniPlayerDesignPreview(design: MiniPlayerDesign) {
    val pc = LocalPlayerConnection.current
    val meta by (pc?.mediaMetadata ?: remember { MutableStateFlow<MediaMetadata?>(null) }).collectAsState()
    val gradient = rememberMiniPreviewGradient(meta?.thumbnailUrl)
    val cs = MaterialTheme.colorScheme
    // Reflect the chosen mini-player background style in the preview.
    val bgStyle by rememberEnumPreference(MiniPlayerBackgroundStyleKey, MiniPlayerBackgroundStyle.GRADIENT)
    val lightText = bgStyle == MiniPlayerBackgroundStyle.GRADIENT || bgStyle == MiniPlayerBackgroundStyle.BLUR
    val onColor = if (lightText) Color.White else cs.onSurface
    val fallbackArt = cs.primary

    val barShape = when (design) {
        MiniPlayerDesign.FLAT -> RoundedCornerShape(6.dp)
        MiniPlayerDesign.FLOATING -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(50)
    }
    val artShape =
        if (design == MiniPlayerDesign.FLAT || design == MiniPlayerDesign.FLOATING) RoundedCornerShape(6.dp) else CircleShape

    val baseBg =
        if (bgStyle == MiniPlayerBackgroundStyle.GRADIENT && gradient.size >= 2) {
            Modifier.background(Brush.horizontalGradient(gradient))
        } else {
            Modifier.background(cs.surfaceContainerHighest)
        }

    Box(
        modifier = Modifier
            .fillMaxWidth(if (design == MiniPlayerDesign.FLOATING) 0.9f else 1f)
            .then(if (design == MiniPlayerDesign.FLOATING) Modifier.shadow(6.dp, barShape, clip = false) else Modifier)
            .height(46.dp)
            .clip(barShape)
            .then(baseBg),
        contentAlignment = Alignment.CenterStart,
    ) {
        when (bgStyle) {
            MiniPlayerBackgroundStyle.BLUR -> {
                meta?.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(16.dp),
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                }
            }
            MiniPlayerBackgroundStyle.GRADIENT -> {
                if (gradient.size >= 2) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
            }
            else -> {}
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp),
        ) {
            Box(Modifier.size(32.dp).clip(artShape)) {
                val url = meta?.thumbnailUrl
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(fallbackArt))
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = meta?.title ?: "Song title",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = onColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.unknown),
                    fontSize = 8.sp,
                    color = onColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

/** Album-art horizontal gradient for the mini-player preview (same extractor as the real one). */
@Composable
private fun rememberMiniPreviewGradient(url: String?): List<Color> {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.surfaceContainer.toArgb()
    var colors by remember { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(url) {
        if (url == null) {
            colors = emptyList()
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(100, 100)
                .allowHardware(false)
                .build()
            val bitmap = runCatching { context.imageLoader.execute(request) }.getOrNull()?.image?.toBitmap()
            if (bitmap != null) {
                val palette = Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate()
                val extracted = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallback)
                withContext(Dispatchers.Main) { colors = extracted }
            }
        }
    }
    return colors
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
