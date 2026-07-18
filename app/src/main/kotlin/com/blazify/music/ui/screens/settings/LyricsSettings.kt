/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.ExperimentalLyricsKey
import com.blazify.music.constants.HideStatusBarOnFullscreenKey
import com.blazify.music.constants.LyricsAnimationStyle
import com.blazify.music.constants.LyricsAnimationStyleKey
import com.blazify.music.constants.LyricsClickKey
import com.blazify.music.constants.LyricsGlowEffectKey
import com.blazify.music.constants.LyricsLineSpacingKey
import com.blazify.music.constants.LyricsScrollKey
import com.blazify.music.constants.LyricsTextPositionKey
import com.blazify.music.constants.LyricsTextSizeKey
import com.blazify.music.constants.RespectAgentPositioningKey
import com.blazify.music.ui.component.DefaultDialog
import com.blazify.music.ui.component.EnumDialog
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.component.Material3SettingsGroup
import com.blazify.music.ui.component.Material3SettingsItem
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One home for lyrics settings — display, sources, translation and romanization —
 * so users don't have to hunt across three separate screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettings(navController: NavController) {
    val (lyricsPosition, onLyricsPositionChange) =
        rememberEnumPreference(LyricsTextPositionKey, defaultValue = LyricsPosition.CENTER)
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) =
        rememberPreference(HideStatusBarOnFullscreenKey, defaultValue = false)
    val (respectAgentPositioning, onRespectAgentPositioningChange) =
        rememberPreference(RespectAgentPositioningKey, defaultValue = true)
    val (experimentalLyrics, onExperimentalLyricsChange) =
        rememberPreference(ExperimentalLyricsKey, defaultValue = true)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) =
        rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) =
        rememberEnumPreference(LyricsAnimationStyleKey, defaultValue = LyricsAnimationStyle.FADE)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.2f)

    var showExperimentalLyricsBetaDialog by remember { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }
    var showLyricsPositionDialog by rememberSaveable { mutableStateOf(false) }

    fun switchIcon(checked: Boolean): @Composable () -> Unit = {
        Icon(
            painter = painterResource(id = if (checked) R.drawable.check else R.drawable.close),
            contentDescription = null,
            modifier = Modifier.size(SwitchDefaults.IconSize),
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(64.dp))

        // ── Sources / translation / romanization (links for now; see step B) ──
        Material3SettingsGroup(
            title = stringResource(R.string.lyrics_sources_group),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.translate),
                    title = { Text(stringResource(R.string.ai_lyrics_translation)) },
                    description = { Text(stringResource(R.string.hint_lyrics)) },
                    onClick = { navController.navigate("settings/ai") },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.lyrics_romanize_title)) },
                    onClick = { navController.navigate("settings/content/romanization") },
                ),
            ),
        )

        Spacer(Modifier.height(27.dp))

        // ── Display (moved from Appearance) ──
        Material3SettingsGroup(
            title = stringResource(R.string.lyrics_display_group),
            items = buildList {
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
                                    if (!experimentalLyrics) showExperimentalLyricsBetaDialog = true
                                    else onExperimentalLyricsChange(false)
                                },
                                thumbContent = switchIcon(experimentalLyrics),
                            )
                        },
                        onClick = {
                            if (!experimentalLyrics) showExperimentalLyricsBetaDialog = true
                            else onExperimentalLyricsChange(false)
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
                                Switch(checked = lyricsGlowEffect, onCheckedChange = onLyricsGlowEffectChange, thumbContent = switchIcon(lyricsGlowEffect))
                            },
                            onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_animation_style_title)) },
                            description = { Text(lyricsAnimationStyle.label()) },
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
                        description = { Text(lyricsPosition.positionLabel()) },
                        onClick = { showLyricsPositionDialog = true },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.respect_agent_positioning)) },
                        description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                        trailingContent = {
                            Switch(checked = respectAgentPositioning, onCheckedChange = onRespectAgentPositioningChange, thumbContent = switchIcon(respectAgentPositioning))
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
                            Switch(checked = lyricsClick, onCheckedChange = onLyricsClickChange, thumbContent = switchIcon(lyricsClick))
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
                            Switch(checked = lyricsScroll, onCheckedChange = onLyricsScrollChange, thumbContent = switchIcon(lyricsScroll))
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
                            Switch(checked = hideStatusBarOnFullscreen, onCheckedChange = onHideStatusBarOnFullscreenChange, thumbContent = switchIcon(hideStatusBarOnFullscreen))
                        },
                        onClick = { onHideStatusBarOnFullscreenChange(!hideStatusBarOnFullscreen) },
                    ),
                )
            },
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = { onLyricsPositionChange(it); showLyricsPositionDialog = false },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = { it.positionLabel() },
        )
    }
    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = { onLyricsAnimationStyleChange(it); showLyricsAnimationStyleDialog = false },
            title = stringResource(R.string.lyrics_animation_style_title),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = { it.label() },
        )
    }
    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
        DefaultDialog(
            onDismiss = { tempTextSize = lyricsTextSize; showLyricsTextSizeDialog = false },
            buttons = {
                TextButton(onClick = { tempTextSize = 24f }) { Text(stringResource(R.string.reset)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { tempTextSize = lyricsTextSize; showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsTextSizeChange(tempTextSize); showLyricsTextSizeDialog = false }) { Text(stringResource(android.R.string.ok)) }
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.lyrics_text_size), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text("${tempTextSize.roundToInt()} sp", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempTextSize, onValueChange = { tempTextSize = it }, valueRange = 12f..48f, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        DefaultDialog(
            onDismiss = { tempLineSpacing = lyricsLineSpacing; showLyricsLineSpacingDialog = false },
            buttons = {
                TextButton(onClick = { tempLineSpacing = 1.3f }) { Text(stringResource(R.string.reset)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { tempLineSpacing = lyricsLineSpacing; showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLyricsLineSpacingChange(tempLineSpacing); showLyricsLineSpacingDialog = false }) { Text(stringResource(android.R.string.ok)) }
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.lyrics_line_spacing), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(String.format(Locale.US, "%.1f", tempLineSpacing), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempLineSpacing, onValueChange = { tempLineSpacing = it }, valueRange = 1.0f..3.0f, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (showExperimentalLyricsBetaDialog) {
        DefaultDialog(
            onDismiss = { showExperimentalLyricsBetaDialog = false },
            title = { Text(stringResource(R.string.experimental_lyrics_beta_title)) },
            buttons = {
                TextButton(onClick = { showExperimentalLyricsBetaDialog = false }) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = { showExperimentalLyricsBetaDialog = false; onExperimentalLyricsChange(true) }) { Text(stringResource(R.string.enable)) }
            },
        ) { Text(stringResource(R.string.experimental_lyrics_beta_message)) }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lyrics)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun LyricsPosition.positionLabel(): String = when (this) {
    LyricsPosition.LEFT -> stringResource(R.string.left)
    LyricsPosition.CENTER -> stringResource(R.string.center)
    LyricsPosition.RIGHT -> stringResource(R.string.right)
}

@Composable
private fun LyricsAnimationStyle.label(): String = when (this) {
    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
}
