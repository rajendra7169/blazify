/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.DarkModeKey
import com.blazify.music.constants.DefaultOpenTabKey
import com.blazify.music.constants.DynamicThemeKey
import com.blazify.music.constants.GridItemSize
import com.blazify.music.constants.GridItemsSizeKey
import com.blazify.music.constants.LyricsTextPositionKey
import com.blazify.music.constants.MiniPlayerBackgroundStyle
import com.blazify.music.constants.MiniPlayerBackgroundStyleKey
import com.blazify.music.constants.MiniPlayerDesignKey
import com.blazify.music.constants.PlayerDesignKey
import com.blazify.music.constants.PureBlackKey
import com.blazify.music.constants.PureBlackMiniPlayerKey
import com.blazify.music.constants.SelectedThemeColorKey
import com.blazify.music.constants.ShowHomeGreetingKey
import com.blazify.music.constants.ShowHomeSearchBarKey
import com.blazify.music.constants.SlimNavBarKey
import com.blazify.music.constants.UseNewMiniPlayerDesignKey
import com.blazify.music.models.MediaMetadata
import com.blazify.music.ui.component.EnumDialog
import com.blazify.music.ui.component.Material3SettingsGroup
import com.blazify.music.ui.component.Material3SettingsItem
import com.blazify.music.ui.player.MiniPlayerDesign
import com.blazify.music.ui.player.PlayerDesign
import com.blazify.music.ui.theme.BlazeThemeColor
import com.blazify.music.ui.theme.BlazifyTheme
import com.blazify.music.ui.theme.DefaultThemeColor
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

/** Tabs of the Look & Feel hub. More (Player / Lyrics / Home) land in later increments. */
private enum class LookFeelTab(val labelRes: Int) {
    THEME(R.string.theme),
    PLAYER(R.string.player),
    MINI(R.string.mini_player),
    LYRICS(R.string.lyrics),
    HOME(R.string.home),
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

    // ── Player state ──
    val (playerDesignId) = rememberPreference(PlayerDesignKey, PlayerDesign.CLASSIC.id)
    val playerDesign = remember(playerDesignId) {
        PlayerDesign.entries.firstOrNull { it.id == playerDesignId } ?: PlayerDesign.CLASSIC
    }
    val playerConnection = LocalPlayerConnection.current

    // ── Lyrics state ──
    val (lyricsPosition, onLyricsPositionChange) =
        rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    var showLyricsPositionDialog by rememberSaveable { mutableStateOf(false) }

    // ── Home / layout state ──
    val (defaultOpenTab, onDefaultOpenTabChange) =
        rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
    var showDefaultOpenTabDialog by rememberSaveable { mutableStateOf(false) }
    val (gridItemSize, onGridItemSizeChange) =
        rememberEnumPreference(GridItemsSizeKey, GridItemSize.SMALL)
    var showGridSizeDialog by rememberSaveable { mutableStateOf(false) }
    val (slimNavBar, onSlimNavBarChange) = rememberPreference(SlimNavBarKey, defaultValue = false)
    val (showHomeGreeting, onShowHomeGreetingChange) = rememberPreference(ShowHomeGreetingKey, defaultValue = true)
    val (showHomeSearchBar, onShowHomeSearchBarChange) = rememberPreference(ShowHomeSearchBarKey, defaultValue = true)

    var tab by rememberSaveable { mutableStateOf(LookFeelTab.THEME) }

    // Preview frame scales with the screen (responsive) and leaves room for the tabs + controls.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val frameHeight = (screenHeightDp * 0.48f).coerceIn(260f, 480f).dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            .padding(top = 56.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Pinned live preview (interior switches with the active tab) ──
        ThemePhoneFrame(modifier = Modifier.height(frameHeight)) {
            when (tab) {
                LookFeelTab.PLAYER -> LivePreview(playerDesign, playerConnection)
                LookFeelTab.LYRICS -> LyricsSampleInterior(
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    themeColor = selectedThemeColor,
                    position = lyricsPosition,
                )
                else -> ThemePhonePreview(
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    themeColor = selectedThemeColor,
                )
            }
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

                LookFeelTab.PLAYER ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        Material3SettingsGroup(
                            items = listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.palette),
                                    title = { Text(stringResource(R.string.player_theme)) },
                                    description = { Text(stringResource(playerDesign.nameRes)) },
                                    onClick = { navController.navigate("settings/appearance/player_design") },
                                ),
                            ),
                        )
                    }

                LookFeelTab.LYRICS ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        Material3SettingsGroup(
                            items = listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.lyrics),
                                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                                    description = { Text(lyricsPosition.label()) },
                                    onClick = { showLyricsPositionDialog = true },
                                ),
                            ),
                        )
                    }

                LookFeelTab.HOME ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        Material3SettingsGroup(
                            items = listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.home_outlined),
                                    title = { Text(stringResource(R.string.default_open_tab)) },
                                    description = { Text(defaultOpenTab.label()) },
                                    onClick = { showDefaultOpenTabDialog = true },
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.grid_view),
                                    title = { Text(stringResource(R.string.grid_cell_size)) },
                                    description = { Text(gridItemSize.label()) },
                                    onClick = { showGridSizeDialog = true },
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.nav_bar),
                                    title = { Text(stringResource(R.string.slim_navbar)) },
                                    trailingContent = {
                                        Switch(checked = slimNavBar, onCheckedChange = onSlimNavBarChange)
                                    },
                                    onClick = { onSlimNavBarChange(!slimNavBar) },
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.home_outlined),
                                    title = { Text(stringResource(R.string.show_home_greeting)) },
                                    description = { Text(stringResource(R.string.show_home_greeting_desc)) },
                                    trailingContent = {
                                        Switch(checked = showHomeGreeting, onCheckedChange = onShowHomeGreetingChange)
                                    },
                                    onClick = { onShowHomeGreetingChange(!showHomeGreeting) },
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.search),
                                    title = { Text(stringResource(R.string.show_home_search_bar)) },
                                    description = { Text(stringResource(R.string.show_home_search_bar_desc)) },
                                    trailingContent = {
                                        Switch(checked = showHomeSearchBar, onCheckedChange = onShowHomeSearchBarChange)
                                    },
                                    onClick = { onShowHomeSearchBarChange(!showHomeSearchBar) },
                                ),
                            ),
                        )
                    }

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

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.entries.toList(),
            valueText = { it.label() },
        )
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
            values = NavigationTab.entries.toList(),
            valueText = { it.label() },
        )
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
            values = GridItemSize.entries.toList(),
            valueText = { it.label() },
        )
    }
}

@Composable
private fun LyricsPosition.label(): String = when (this) {
    LyricsPosition.LEFT -> stringResource(R.string.left)
    LyricsPosition.CENTER -> stringResource(R.string.center)
    LyricsPosition.RIGHT -> stringResource(R.string.right)
}

@Composable
private fun NavigationTab.label(): String = when (this) {
    NavigationTab.HOME -> stringResource(R.string.home)
    NavigationTab.SEARCH -> stringResource(R.string.search)
    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
}

@Composable
private fun GridItemSize.label(): String = when (this) {
    GridItemSize.BIG -> stringResource(R.string.big)
    GridItemSize.SMALL -> stringResource(R.string.small)
}

/**
 * A faithful mini of the real lyrics page: Language pill up top, dimmed previous
 * stanza → bright active stanza → dimmed next, then the song bar (real art ·
 * title/artist · fullscreen/theme/more buttons) and the progress bar with times,
 * all over the tinted gradient. Lyric alignment follows the position setting.
 */
@Composable
private fun LyricsSampleInterior(
    darkMode: DarkMode,
    pureBlack: Boolean,
    themeColor: Color,
    position: LyricsPosition,
) {
    val useDark = when (darkMode) {
        DarkMode.AUTO -> isSystemInDarkTheme()
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    BlazifyTheme(darkTheme = useDark, pureBlack = pureBlack, themeColor = themeColor) {
        val cs = MaterialTheme.colorScheme
        val pc = LocalPlayerConnection.current
        val meta by (pc?.mediaMetadata ?: remember { MutableStateFlow<MediaMetadata?>(null) }).collectAsState()
        val horizontal = when (position) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.RIGHT -> Alignment.End
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
        }
        val textAlign = when (position) {
            LyricsPosition.LEFT -> TextAlign.Start
            LyricsPosition.RIGHT -> TextAlign.End
            LyricsPosition.CENTER -> TextAlign.Center
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(lerp(cs.primary, Color.Black, 0.62f), cs.background)))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Language pill — a little below the notch, like the real page.
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp),
                ) {
                    Icon(painterResource(R.drawable.translate), null, tint = Color.White, modifier = Modifier.size(7.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Language", fontSize = 5.5.sp, lineHeight = 6.sp, color = Color.White)
                }
            }
            // Stanzas fill the whole page height — dimmed · bright active · dimmed —
            // spread out like the real lyrics screen, not clustered in the middle.
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = horizontal,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Text(
                    "Baatein teri,\nraatein-saugaate\nin teri",
                    color = Color.White.copy(alpha = 0.30f), fontSize = 10.sp, lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold, textAlign = textAlign, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Kyun tera sab\nyeh ho gaya?\nHua kya?",
                    color = Color.White, fontSize = 13.sp, lineHeight = 15.5.sp,
                    fontWeight = FontWeight.Bold, textAlign = textAlign, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Main kahin bhi\njaata hoon, tum\nse hi mil jaata hoon",
                    color = Color.White.copy(alpha = 0.30f), fontSize = 10.sp, lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold, textAlign = textAlign, modifier = Modifier.fillMaxWidth(),
                )
            }
            // Song bar: real art · title/artist · fullscreen / theme / more.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))) {
                    val url = meta?.thumbnailUrl
                    if (url != null) {
                        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(cs.primary))
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(meta?.title ?: "Song title", color = Color.White, fontSize = 7.sp, lineHeight = 7.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        meta?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() } ?: "Artist",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 5.5.sp, lineHeight = 6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(4.dp))
                listOf(R.drawable.fullscreen, R.drawable.palette, R.drawable.more_horiz).forEach { icon ->
                    Box(
                        Modifier.padding(start = 3.dp).size(13.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(icon), null, tint = Color.Black, modifier = Modifier.size(7.dp))
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            // Progress bar + times.
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth(0.48f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(cs.primary))
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("2:33", color = Color.White.copy(alpha = 0.8f), fontSize = 5.sp, lineHeight = 5.5.sp)
                Text("5:21", color = Color.White.copy(alpha = 0.8f), fontSize = 5.sp, lineHeight = 5.5.sp)
            }
        }
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

/** Amber pill tab strip; horizontally scrollable so it scales to any number of tabs. */
@Composable
private fun LookFeelTabRow(
    selected: LookFeelTab,
    onSelect: (LookFeelTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LookFeelTab.entries.forEach { t ->
            val active = t == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onSelect(t) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
