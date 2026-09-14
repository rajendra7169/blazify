/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.blazify.music.constants.EnableBetterLyricsKey
import com.blazify.music.constants.EnableKugouKey
import com.blazify.music.constants.EnableLrcLibKey
import com.blazify.music.constants.EnableLyricsPlus
import com.blazify.music.constants.EnablePaxsenixKey
import com.blazify.music.constants.HideStatusBarOnFullscreenKey
import com.blazify.music.constants.LyricsGlowEffectKey
import com.blazify.music.constants.LyricsLineSpacingKey
import com.blazify.music.constants.LyricsProviderOrderKey
import com.blazify.music.constants.LyricsClickKey
import com.blazify.music.constants.LyricsTextPositionKey
import com.blazify.music.constants.LyricsTextSizeKey
import com.blazify.music.constants.RespectAgentPositioningKey
import com.blazify.music.lyrics.LyricsProviderRegistry
import com.blazify.music.ui.component.DraggableLyricsProviderItem
import com.blazify.music.ui.component.DraggableLyricsProviderList
import com.blazify.music.ui.component.EnumDialog
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.component.Material3SettingsGroup
import com.blazify.music.ui.component.Material3SettingsItem
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference
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
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) =
        rememberPreference(HideStatusBarOnFullscreenKey, defaultValue = false)
    val (respectAgentPositioning, onRespectAgentPositioningChange) =
        rememberPreference(RespectAgentPositioningKey, defaultValue = true)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 36f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (lyricsGlow, onLyricsGlowChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = true)

    // Sources (moved from Content).
    val (enableKugou, onEnableKugouChange) = rememberPreference(EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(EnableBetterLyricsKey, defaultValue = true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(EnablePaxsenixKey, defaultValue = true)
    val (enableLyricsPlus, onEnableLyricsPlusChange) = rememberPreference(EnableLyricsPlus, defaultValue = true)
    val (lyricsProviderOrder, onLyricsProviderOrderChange) = rememberPreference(
        LyricsProviderOrderKey,
        defaultValue = LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder()),
    )
    val providerDisplayNames = mapOf(
        "BetterLyrics" to "Better Lyrics",
        "Paxsenix" to "Paxsenix",
        "LrcLib" to "LrcLib",
        "KuGou" to "KuGou",
        "LyricsPlus" to "LyricsPlus",
        "YouTubeSubtitle" to "YouTube Subtitles",
        "YouTube" to "YouTube",
    )

    var showLyricsPositionDialog by rememberSaveable { mutableStateOf(false) }
    var showProviderSelectionDialog by rememberSaveable { mutableStateOf(false) }
    var showProviderPriorityDialog by rememberSaveable { mutableStateOf(false) }

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
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_provider_selection)) },
                    description = { Text(stringResource(R.string.lyrics_provider_selection_desc)) },
                    onClick = { showProviderSelectionDialog = true },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_provider_priority)) },
                    description = { Text(stringResource(R.string.lyrics_provider_priority_desc)) },
                    onClick = { showProviderPriorityDialog = true },
                ),
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
                        title = { Text(stringResource(R.string.lyrics_text_position)) },
                        description = { Text(lyricsPosition.positionLabel()) },
                        onClick = { showLyricsPositionDialog = true },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_text_size)) },
                        description = {
                            Column {
                                Text(lyricsTextSize.roundToInt().toString())
                                Slider(
                                    value = lyricsTextSize,
                                    onValueChange = { onLyricsTextSizeChange(it.roundToInt().toFloat()) },
                                    valueRange = 24f..48f,
                                    steps = 11,
                                )
                            }
                        },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                        description = {
                            Column {
                                Text("%.1f×".format(lyricsLineSpacing))
                                Slider(
                                    value = lyricsLineSpacing,
                                    onValueChange = { onLyricsLineSpacingChange((it * 10).roundToInt() / 10f) },
                                    valueRange = 1.0f..1.8f,
                                    steps = 7,
                                )
                            }
                        },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                        trailingContent = {
                            Switch(checked = lyricsGlow, onCheckedChange = onLyricsGlowChange, thumbContent = switchIcon(lyricsGlow))
                        },
                        onClick = { onLyricsGlowChange(!lyricsGlow) },
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

    if (showProviderSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showProviderSelectionDialog = false },
            title = { Text(stringResource(R.string.lyrics_provider_selection)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    providerSwitchRow(stringResource(R.string.enable_lrclib), stringResource(R.string.enable_lrclib_desc), enableLrclib, onEnableLrclibChange)
                    providerSwitchRow(stringResource(R.string.enable_kugou), stringResource(R.string.enable_kugou_desc), enableKugou, onEnableKugouChange)
                    providerSwitchRow(stringResource(R.string.enable_better_lyrics), stringResource(R.string.enable_better_lyrics_desc), enableBetterLyrics, onEnableBetterLyricsChange)
                    providerSwitchRow(stringResource(R.string.enable_paxsenix), stringResource(R.string.enable_paxsenix_desc), enablePaxsenix, onEnablePaxsenixChange)
                    providerSwitchRow(stringResource(R.string.enable_lyricsplus), stringResource(R.string.enable_lyricsplus_desc), enableLyricsPlus, onEnableLyricsPlusChange)
                    Text(
                        text = stringResource(R.string.youtube_music_lyrics_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderSelectionDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (showProviderPriorityDialog) {
        val currentOrder = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder)
        val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
        val normalizedOrder = currentOrder.filter { it in defaultOrder } + defaultOrder.filter { it !in currentOrder }
        val enabledProviders = setOf(
            "LrcLib".takeIf { enableLrclib },
            "KuGou".takeIf { enableKugou },
            "BetterLyrics".takeIf { enableBetterLyrics },
            "Paxsenix".takeIf { enablePaxsenix },
            "LyricsPlus".takeIf { enableLyricsPlus },
        ).filterNotNull().toSet()
        val lyricsIcon = painterResource(R.drawable.lyrics)
        val draggableItems = remember { mutableStateListOf<DraggableLyricsProviderItem>() }
        LaunchedEffect(normalizedOrder, enableLrclib, enableKugou, enableBetterLyrics, enablePaxsenix, enableLyricsPlus) {
            val orderedEnabledProviders = normalizedOrder.filter { it in enabledProviders }
            draggableItems.clear()
            draggableItems.addAll(
                orderedEnabledProviders.mapNotNull { providerName ->
                    LyricsProviderRegistry.getProviderByName(providerName) ?: return@mapNotNull null
                    DraggableLyricsProviderItem(id = providerName, name = providerDisplayNames[providerName] ?: providerName, icon = lyricsIcon)
                },
            )
        }
        AlertDialog(
            onDismissRequest = { showProviderPriorityDialog = false },
            title = { Text(stringResource(R.string.lyrics_provider_priority)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    Text(
                        stringResource(R.string.lyrics_provider_priority_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DraggableLyricsProviderList(
                        items = draggableItems,
                        onItemsReordered = { reorderedItems ->
                            val enabledOrder = reorderedItems.map { it.id }
                            val disabledOrder = normalizedOrder.filter { it !in enabledProviders }
                            onLyricsProviderOrderChange(LyricsProviderRegistry.serializeProviderOrder(enabledOrder + disabledOrder))
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderPriorityDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
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
private fun providerSwitchRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    painter = painterResource(id = if (checked) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    }
}

@Composable
private fun LyricsPosition.positionLabel(): String = when (this) {
    LyricsPosition.LEFT -> stringResource(R.string.left)
    LyricsPosition.CENTER -> stringResource(R.string.center)
    LyricsPosition.RIGHT -> stringResource(R.string.right)
}
