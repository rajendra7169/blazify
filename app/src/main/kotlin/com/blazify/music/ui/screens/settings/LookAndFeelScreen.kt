/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.DarkModeKey
import com.blazify.music.constants.DynamicThemeKey
import com.blazify.music.constants.MiniPlayerBackgroundStyle
import com.blazify.music.constants.MiniPlayerBackgroundStyleKey
import com.blazify.music.constants.MiniPlayerDesignKey
import com.blazify.music.constants.PureBlackKey
import com.blazify.music.constants.PureBlackMiniPlayerKey
import com.blazify.music.constants.SelectedThemeColorKey
import com.blazify.music.constants.UseNewMiniPlayerDesignKey
import com.blazify.music.ui.component.EnumDialog
import com.blazify.music.ui.component.Material3SettingsGroup
import com.blazify.music.ui.component.Material3SettingsItem
import com.blazify.music.ui.player.MiniPlayerDesign
import com.blazify.music.ui.theme.BlazeThemeColor
import com.blazify.music.ui.theme.DefaultThemeColor
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference

/** Tabs of the Look & Feel hub. More (Player / Lyrics / Home) land in later increments. */
private enum class LookFeelTab(val labelRes: Int) {
    THEME(R.string.theme),
    MINI(R.string.mini_player),
}

/**
 * Unified personalization hub: one live phone-frame preview pinned on top, with tabbed
 * controls below. The preview reuses [ThemePhoneFrame] + [ThemePhonePreview]; the controls
 * reuse [ThemeControls] and [MiniPlayerDesignPicker], so edits reflect instantly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookAndFeelScreen(
    navController: NavController,
) {
    // ── Theme state (mirrors ThemeScreen so the preview + controls stay in sync) ──
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (pureBlack, onPureBlackChangeRaw) = rememberPreference(PureBlackKey, defaultValue = true)
    val (_, onPureBlackMiniPlayerChange) = rememberPreference(PureBlackMiniPlayerKey, defaultValue = true)
    val (selectedThemeColorInt, onSelectedThemeColorChange) =
        rememberPreference(SelectedThemeColorKey, BlazeThemeColor.toArgb())
    val (_, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = true)
    val selectedThemeColor = Color(selectedThemeColorInt)

    val onPureBlackChange: (Boolean) -> Unit = { enabled ->
        onPureBlackChangeRaw(enabled)
        onPureBlackMiniPlayerChange(enabled)
    }
    val handleColorSelection: (Color) -> Unit = { color ->
        onSelectedThemeColorChange(color.toArgb())
        onDynamicThemeChange(color == DefaultThemeColor)
    }

    // ── Mini-player state ──
    val (miniPlayerDesignId, onMiniPlayerDesignChange) =
        rememberPreference(MiniPlayerDesignKey, defaultValue = "")
    val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val selectedMiniPlayerDesign =
        remember(miniPlayerDesignId, useNewMiniPlayerDesign) {
            if (miniPlayerDesignId.isBlank()) {
                if (useNewMiniPlayerDesign) MiniPlayerDesign.MODERN else MiniPlayerDesign.FLAT
            } else {
                MiniPlayerDesign.fromId(miniPlayerDesignId)
            }
        }
    val miniPlayerUsesArtBackground = selectedMiniPlayerDesign != MiniPlayerDesign.FLAT
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) =
        rememberEnumPreference(MiniPlayerBackgroundStyleKey, MiniPlayerBackgroundStyle.GRADIENT)
    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }

    var tab by rememberSaveable { mutableStateOf(LookFeelTab.THEME) }

    // Preview frame scales with the screen (responsive) and leaves room for the tabs + controls.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val frameHeight = (screenHeightDp * 0.42f).coerceIn(240f, 440f).dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            .padding(top = 56.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Pinned live preview ──
        ThemePhoneFrame(modifier = Modifier.height(frameHeight)) {
            ThemePhonePreview(
                darkMode = darkMode,
                pureBlack = pureBlack,
                themeColor = selectedThemeColor,
            )
        }
        Spacer(Modifier.height(18.dp))

        // ── Tab strip ──
        LookFeelTabRow(selected = tab, onSelect = { tab = it })
        Spacer(Modifier.height(16.dp))

        // ── Controls for the active tab ──
        Crossfade(targetState = tab, label = "lookfeel-controls") { active ->
            when (active) {
                LookFeelTab.THEME ->
                    ThemeControls(
                        darkMode = darkMode,
                        onDarkModeChange = onDarkModeChange,
                        pureBlack = pureBlack,
                        onPureBlackChange = onPureBlackChange,
                        selectedThemeColor = selectedThemeColor,
                        onSelectedThemeColorChange = handleColorSelection,
                    )

                LookFeelTab.MINI ->
                    Column(Modifier.fillMaxWidth()) {
                        MiniPlayerDesignPicker(
                            selected = selectedMiniPlayerDesign,
                            onSelect = { onMiniPlayerDesignChange(it.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            Material3SettingsGroup(
                                items = listOf(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.gradient),
                                        enabled = miniPlayerUsesArtBackground,
                                        title = { Text(stringResource(R.string.mini_player_background_style)) },
                                        description = {
                                            Text(
                                                if (!miniPlayerUsesArtBackground) {
                                                    stringResource(R.string.mini_player_background_not_available)
                                                } else {
                                                    miniPlayerBackground.label()
                                                },
                                            )
                                        },
                                        onClick = {
                                            if (miniPlayerUsesArtBackground) showMiniPlayerBackgroundDialog = true
                                        },
                                    ),
                                ),
                            )
                        }
                    }
            }
        }

        // Clear the now-playing mini-player + nav bar.
        Spacer(Modifier.windowInsetsBottomHeight(LocalPlayerAwareWindowInsets.current))
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.look_and_feel)) },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
    )

    if (showMiniPlayerBackgroundDialog) {
        val values = MiniPlayerBackgroundStyle.entries.filter {
            it != MiniPlayerBackgroundStyle.BLUR ||
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        }
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = {
                onMiniPlayerBackgroundChange(it)
                showMiniPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.mini_player_background_style),
            current = miniPlayerBackground,
            values = values,
            valueText = { it.label() },
        )
    }
}

@Composable
private fun MiniPlayerBackgroundStyle.label(): String = when (this) {
    MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
    MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
    MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
    MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
    MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
}

/** Amber segmented tab strip; scales cleanly as more tabs are added. */
@Composable
private fun LookFeelTabRow(
    selected: LookFeelTab,
    onSelect: (LookFeelTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LookFeelTab.entries.forEach { t ->
            val active = t == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(t.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
