package com.blazify.music.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import coil3.compose.AsyncImage
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.ui.component.ColorPickerDialog
import com.blazify.music.models.MediaMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import com.blazify.music.constants.DefaultOpenTabKey
import com.blazify.music.constants.GridItemSize
import com.blazify.music.constants.GridItemsSizeKey
import com.blazify.music.constants.MiniPlayerBackgroundStyle
import com.blazify.music.constants.MiniPlayerBackgroundStyleKey
import com.blazify.music.constants.MiniPlayerDesignKey
import com.blazify.music.constants.NavBarStyle
import com.blazify.music.constants.NavBarStyleKey
import com.blazify.music.constants.ShowHomeGreetingKey
import com.blazify.music.constants.ShowHomeSearchBarKey
import com.blazify.music.constants.SlimNavBarKey
import com.blazify.music.constants.UseNewMiniPlayerDesignKey
import com.blazify.music.ui.player.MiniPlayerDesign
import com.blazify.music.ui.theme.BlazeThemeColor
import com.blazify.music.ui.theme.DefaultThemeColor
import com.blazify.music.ui.theme.BlazifyTheme
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference

data class ThemePalette(
    val nameRes: Int,
    val seedColor: Color
)

val PaletteColors = listOf(
    ThemePalette(R.string.palette_dynamic, Color.Transparent), // Sentinel for System/Dynamic colors
    ThemePalette(R.string.palette_blaze, BlazeThemeColor), // Blazify brand color (default)
    ThemePalette(R.string.palette_crimson, Color(0xFFEC5464)), // Slightly shifted from DefaultThemeColor (0xFFED5564) to avoid conflict
    ThemePalette(R.string.palette_rose, Color(0xFFD81B60)),
    ThemePalette(R.string.palette_purple, Color(0xFF8E24AA)),
    ThemePalette(R.string.palette_deep_purple, Color(0xFF5E35B1)),
    ThemePalette(R.string.palette_indigo, Color(0xFF3949AB)),
    ThemePalette(R.string.palette_blue, Color(0xFF1E88E5)),
    ThemePalette(R.string.palette_sky_blue, Color(0xFF039BE5)),
    ThemePalette(R.string.palette_cyan, Color(0xFF00ACC1)),
    ThemePalette(R.string.palette_teal, Color(0xFF00897B)),
    ThemePalette(R.string.palette_green, Color(0xFF43A047)),
    ThemePalette(R.string.palette_light_green, Color(0xFF7CB342)),
    ThemePalette(R.string.palette_lime, Color(0xFFC0CA33)),
    ThemePalette(R.string.palette_yellow, Color(0xFFFDD835)),
    ThemePalette(R.string.palette_amber, Color(0xFFFFB300)),
    ThemePalette(R.string.palette_orange, Color(0xFFFB8C00)),
    ThemePalette(R.string.palette_deep_orange, Color(0xFFF4511E)),
    ThemePalette(R.string.palette_brown, Color(0xFF6D4C41)),
    ThemePalette(R.string.palette_grey, Color(0xFF757575)),
    ThemePalette(R.string.palette_blue_grey, Color(0xFF546E7A)),
)

@Composable
fun ThemeControls(
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System mode (AUTO)
                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.AUTO,
                        targetPureBlack = pureBlack,
                        onClick = {
                            onDarkModeChange(DarkMode.AUTO)
                        },
                        showIcon = true
                    )
                    
                    // Vertical divider to separate System from manual modes
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    
                    // Manual modes (Light, Dark, Pure Black)
                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.OFF,
                        targetPureBlack = false,
                        onClick = {
                            onDarkModeChange(DarkMode.OFF)
                            onPureBlackChange(false)
                        },
                        showIcon = false
                    )
                    
                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.ON,
                        targetPureBlack = false,
                        onClick = {
                            onDarkModeChange(DarkMode.ON)
                            onPureBlackChange(false)
                        },
                        showIcon = false
                    )
                    
                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.ON,
                        targetPureBlack = true,
                        onClick = {
                            onDarkModeChange(DarkMode.ON)
                            onPureBlackChange(true)
                        },
                        showIcon = false
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.color_palette),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // A colour the user mixed themselves matches no preset, so the row
                // needs its own swatch to show it as the active choice.
                val isCustomColor = selectedThemeColor != DefaultThemeColor &&
                    PaletteColors.none { it.seedColor == selectedThemeColor }
                var showColorPicker by rememberSaveable { mutableStateOf(false) }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(PaletteColors) { palette ->
                        val isDynamicPalette = palette.seedColor == Color.Transparent
                        val isSelected = if (isDynamicPalette) {
                            selectedThemeColor == DefaultThemeColor
                        } else {
                            selectedThemeColor == palette.seedColor
                        }

                        PaletteItem(
                            palette = palette,
                            isSelected = isSelected,
                            onClick = {
                                val colorToSave = if (isDynamicPalette) DefaultThemeColor else palette.seedColor
                                onSelectedThemeColorChange(colorToSave)
                            }
                        )
                    }
                    item {
                        CustomPaletteItem(
                            currentColor = selectedThemeColor,
                            isSelected = isCustomColor,
                            onClick = { showColorPicker = true },
                        )
                    }
                }

                if (showColorPicker) {
                    ColorPickerDialog(
                        initialColor = if (isCustomColor) selectedThemeColor else BlazeThemeColor,
                        onDismiss = { showColorPicker = false },
                        onConfirm = { picked ->
                            // DefaultThemeColor is the sentinel that switches theming
                            // back to dynamic, so a custom pick must never land on it.
                            val safe = if (picked == DefaultThemeColor) {
                                Color(picked.toArgb() xor 0x00000001)
                            } else {
                                picked
                            }
                            onSelectedThemeColorChange(safe)
                            showColorPicker = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ModeCircle(
    darkMode: DarkMode,
    pureBlack: Boolean,
    targetMode: DarkMode,
    targetPureBlack: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isSelected = darkMode == targetMode && pureBlack == targetPureBlack
    
    val effectiveDark = when (targetMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    
    // Use actual system colors for AUTO mode on Android 12+
    val modeColorScheme = if (targetMode == DarkMode.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = DefaultThemeColor,
            isDark = effectiveDark,
            style = PaletteStyle.TonalSpot
        )
    }
    
    val fillColor = when {
        targetPureBlack -> Color.Black
        effectiveDark -> modeColorScheme.surface
        else -> modeColorScheme.surface
    }
    
    // Animated border width
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderWidth"
    )
    
    // Animated scale for the entire circle
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    
    val contentDesc = when {
        targetPureBlack -> stringResource(R.string.cd_pure_black_mode)
        targetMode == DarkMode.OFF -> stringResource(R.string.cd_light_mode)
        targetMode == DarkMode.ON -> stringResource(R.string.cd_dark_mode)
        else -> stringResource(R.string.cd_system_mode)
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(fillColor)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            showIcon -> {
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null,
                    tint = modeColorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            isSelected -> {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.3f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                        targetScale = 0.3f,
                        animationSpec = tween(150)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inversePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PaletteItem(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val colorScheme = rememberDynamicColorScheme(
        seedColor = palette.seedColor,
        isDark = isSystemDark,
        style = PaletteStyle.TonalSpot
    )
    
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 48.dp * 0.25f else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderWidth"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    
    val paletteName = stringResource(palette.nameRes)
    val contentDesc = stringResource(R.string.cd_palette_item, paletteName)
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            }
    ) {
        if (palette.seedColor == Color.Transparent) {
            // Draw Dynamic/System icon using Material Design icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                drawRect(
                    color = colorScheme.onPrimary,
                    topLeft = Offset(0f, 0f),
                    size = Size(width, height / 2)
                )
                
                drawRect(
                    color = colorScheme.secondary,
                    topLeft = Offset(0f, height / 2),
                    size = Size(width / 2, height / 2)
                )
                
                drawRect(
                    color = colorScheme.tertiary,
                    topLeft = Offset(width / 2, height / 2),
                    size = Size(width / 2, height / 2)
                )
            }
        }
    }
}

/** Metallic phone bezel with a soft drop shadow — same look as the player-theme screen. */
@Composable
internal fun ThemePhoneFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val frameShape = RoundedCornerShape(38.dp)
    Box(
        modifier = modifier
            .aspectRatio(9f / 19.3f)
            .shadow(
                elevation = 24.dp,
                shape = frameShape,
                clip = false,
                ambientColor = Color.White.copy(alpha = 0.30f),
                spotColor = Color.White.copy(alpha = 0.50f),
            )
            .clip(frameShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF44454A), Color(0xFF26272B), Color(0xFF1A1B1E)),
                ),
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.28f),
                    ),
                ),
                shape = frameShape,
            )
            .padding(6.dp)
            .clip(RoundedCornerShape(32.dp)),
    ) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
        )
    }
}

private fun greetingLine(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good\nMorning 🌅"
        hour in 12..16 -> "Good\nAfternoon ☀️"
        hour in 17..20 -> "Good\nEvening 🌆"
        else -> "Good\nNight 🌙"
    }
}

/** A realistic mini Blazify home rendered with the chosen theme, so changes preview live. */
@Composable
internal fun ThemePhonePreview(
    darkMode: DarkMode,
    pureBlack: Boolean,
    themeColor: Color,
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (darkMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    // Reflect the chosen mini-player design + background in the preview.
    val (miniDesignId) = rememberPreference(MiniPlayerDesignKey, defaultValue = "")
    val (useNewMini) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val miniDesign = remember(miniDesignId, useNewMini) {
        if (miniDesignId.isBlank()) {
            if (useNewMini) MiniPlayerDesign.MODERN else MiniPlayerDesign.FLAT
        } else {
            MiniPlayerDesign.fromId(miniDesignId)
        }
    }
    val (miniBgStyle) = rememberEnumPreference(MiniPlayerBackgroundStyleKey, MiniPlayerBackgroundStyle.GRADIENT)
    // Reflect the home-layout settings too, so the preview updates when they change.
    val (defaultTab) = rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
    val (gridSize) = rememberEnumPreference(GridItemsSizeKey, GridItemSize.SMALL)
    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
    val (showGreetingCard) = rememberPreference(ShowHomeGreetingKey, defaultValue = true)
    val (showSearchPill) = rememberPreference(ShowHomeSearchBarKey, defaultValue = true)
    val (navStyle) = rememberEnumPreference(NavBarStyleKey, NavBarStyle.PILL)
    BlazifyTheme(darkTheme = useDark, pureBlack = pureBlack, themeColor = themeColor) {
        val cs = MaterialTheme.colorScheme
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(horizontal = 10.dp, vertical = 14.dp),
        ) {
            // Header: account · logo + wordmark · settings (real icons).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.person), null, tint = cs.onSurface.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Image(
                        painter = painterResource(if (useDark) R.drawable.blaze_logo_white else R.drawable.blaze_logo),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                    )
                    Text("Blazify", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                }
                Icon(painterResource(R.drawable.settings), null, tint = cs.onSurface.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(6.dp))
            // Greeting card: a CLIPPED gradient background with the hero image as an
            // UN-clipped sibling of an un-clipped outer box, so the hero spills out of
            // the card and a little over the wordmark — exactly like the real home.
            val onCard = cs.onPrimary
            if (showGreetingCard) Box(modifier = Modifier.fillMaxWidth().height(68.dp)) {
                // Card background (rounded, clipped).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(cs.primary, lerp(cs.primary, Color.Black, if (useDark) 0.30f else 0.20f)))),
                )
                // Hero: spills up out of the card (over the wordmark) but the offset
                // keeps its bottom a few dp INSIDE the card, so it never overflows below.
                // No clip so the transparent PNG blends like on the real home.
                Image(
                    painter = painterResource(if (useDark) R.drawable.blaze_home_dark else R.drawable.blaze_home_light),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .requiredWidth(78.dp)
                        .requiredHeight(92.dp)
                        // requiredHeight overflow is centred (12dp above AND below the
                        // 68dp card) — shift up by exactly that half so the bottom edge
                        // is flush inside the card and only the top spills out.
                        .offset(y = (-12).dp),
                )
                // Greeting text — centered-left, tight (so the greeting sits a touch
                // lower and 'Enjoy the music' a touch higher, with padding around it).
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.58f).padding(start = 11.dp),
                ) {
                    // Explicit lineHeights kill the inherited tall line-boxes, compressing
                    // the block: the greeting sits lower, 'Enjoy the music' higher, with
                    // even padding above and below.
                    Text(greetingLine(), color = onCard, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, lineHeight = 9.5.sp, maxLines = 2)
                    Text("Music Lover", color = onCard.copy(alpha = 0.95f), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, lineHeight = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Enjoy the music 🎵", color = onCard.copy(alpha = 0.85f), fontSize = 5.5.sp, fontWeight = FontWeight.Medium, lineHeight = 6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(9.dp))
            // Search pill (icon · placeholder · mic).
            val searchTint = if (useDark) Color.White.copy(alpha = 0.7f) else Color(0x8A000000)
            if (showSearchPill) Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (useDark) Color.White.copy(alpha = 0.10f) else Color(0xFFEEEEEE))
                    .padding(horizontal = 9.dp),
            ) {
                Icon(painterResource(R.drawable.search), null, tint = searchTint, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(5.dp))
                Text("Search songs, albums, artists...", fontSize = 7.sp, color = searchTint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.mic), null, tint = searchTint, modifier = Modifier.size(11.dp))
            }
            Spacer(Modifier.height(8.dp))
            // Mood chips with labels, like the real home — the rail runs past the screen
            // edge and the last chip gets cut off, exactly like the real chips row.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState(), enabled = false),
            ) {
                listOf("Energize", "Feel good", "Relax", "Workout", "Party").forEach { label ->
                    // Fixed thin height + explicit lineHeight — the inherited text
                    // line-box was inflating these pills no matter the padding.
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(cs.surfaceContainerHigh)
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, fontSize = 5.sp, lineHeight = 5.5.sp, color = cs.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            // "Quick picks" header + a small Play all pill.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Quick picks", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .border(0.6.dp, cs.primary.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Play all", fontSize = 4.5.sp, lineHeight = 5.sp, fontWeight = FontWeight.Medium, color = cs.primary)
                }
            }
            Spacer(Modifier.height(7.dp))
            // Quick-picks song rows (art · title · artist · ⋮). Art size reflects grid cell size.
            val rowArt = listOf(cs.secondaryContainer, cs.tertiaryContainer, cs.primaryContainer)
            val artSize = if (gridSize == GridItemSize.BIG) 28.dp else 23.dp
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(4) { i ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.size(artSize).clip(RoundedCornerShape(6.dp)).background(rowArt[i % rowArt.size]))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.fillMaxWidth(0.68f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(cs.onSurface.copy(alpha = 0.85f)))
                            Box(Modifier.fillMaxWidth(0.44f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(cs.onSurfaceVariant.copy(alpha = 0.6f)))
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                            repeat(3) { Box(Modifier.size(2.5.dp).clip(CircleShape).background(cs.onSurfaceVariant.copy(alpha = 0.6f))) }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // Mini-player — REAL song art + title; shape/background reflect the design.
            val pc = LocalPlayerConnection.current
            val meta by (pc?.mediaMetadata ?: remember { MutableStateFlow<MediaMetadata?>(null) }).collectAsState()
            val isFloating = miniDesign == MiniPlayerDesign.FLOATING
            val isFlat = miniDesign == MiniPlayerDesign.FLAT
            val miniShape = when {
                isFlat -> RoundedCornerShape(6.dp)
                isFloating -> RoundedCornerShape(12.dp)
                else -> RoundedCornerShape(50)
            }
            val miniArtShape = if (isFlat) RoundedCornerShape(5.dp) else CircleShape
            val onMini = when (miniBgStyle) {
                MiniPlayerBackgroundStyle.GRADIENT -> cs.onPrimary
                MiniPlayerBackgroundStyle.PURE_BLACK -> Color.White
                else -> cs.onSurface
            }
            val miniBackground: Modifier = when (miniBgStyle) {
                MiniPlayerBackgroundStyle.GRADIENT ->
                    Modifier.background(Brush.horizontalGradient(listOf(cs.primary, cs.primary.copy(alpha = 0.72f))))
                MiniPlayerBackgroundStyle.PURE_BLACK -> Modifier.background(Color.Black)
                MiniPlayerBackgroundStyle.BLUR -> Modifier.background(cs.surfaceVariant)
                else -> Modifier.background(cs.surfaceContainerHighest)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isFloating) 0.94f else 1f)
                        .then(if (isFloating) Modifier.shadow(4.dp, miniShape, clip = false) else Modifier)
                        .height(34.dp)
                        .clip(miniShape)
                        .then(miniBackground)
                        .then(if (isFlat) Modifier else Modifier.border(0.6.dp, onMini.copy(alpha = 0.25f), miniShape)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)) {
                        // The real mini-player wraps the round art in a progress ring;
                        // only the flat design keeps a plain square thumbnail.
                        val ringColor = if (miniBgStyle == MiniPlayerBackgroundStyle.GRADIENT) onMini else cs.primary
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(if (isFlat) 23.dp else 26.dp)
                                .then(
                                    if (isFlat) {
                                        Modifier
                                    } else {
                                        Modifier.drawWithContent {
                                            drawContent()
                                            val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                                            val inset = stroke.width / 2
                                            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                                            drawArc(onMini.copy(alpha = 0.2f), 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
                                            drawArc(ringColor, -90f, 360f * 0.35f, false, Offset(inset, inset), arcSize, style = stroke)
                                        }
                                    },
                                ),
                        ) {
                            Box(Modifier.size(if (isFlat) 23.dp else 21.dp).clip(miniArtShape)) {
                                val url = meta?.thumbnailUrl
                                if (url != null) {
                                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(Modifier.fillMaxSize().background(onMini.copy(alpha = 0.85f)))
                                }
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            // Explicit lineHeights: the inherited tall line-boxes pushed the
                            // artist line out of the 34dp bar's clip, so it never showed.
                            Text(meta?.title ?: "Song title", color = onMini, fontSize = 7.sp, fontWeight = FontWeight.Bold, lineHeight = 7.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                meta?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() } ?: "Artist",
                                color = onMini.copy(alpha = 0.7f), fontSize = 5.5.sp, lineHeight = 6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        when (miniDesign) {
                            MiniPlayerDesign.ROUNDED -> {
                                Icon(painterResource(R.drawable.skip_previous), null, tint = onMini, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(2.dp))
                                Box(Modifier.size(15.dp).clip(CircleShape).background(onMini), contentAlignment = Alignment.Center) {
                                    Icon(
                                        painterResource(R.drawable.play), null,
                                        tint = if (miniBgStyle == MiniPlayerBackgroundStyle.GRADIENT) cs.primary else cs.surface,
                                        modifier = Modifier.size(9.dp),
                                    )
                                }
                                Spacer(Modifier.width(2.dp))
                                Icon(painterResource(R.drawable.skip_next), null, tint = onMini, modifier = Modifier.size(10.dp))
                            }
                            MiniPlayerDesign.FLAT ->
                                Icon(painterResource(R.drawable.favorite_border), null, tint = onMini, modifier = Modifier.size(11.dp))
                            else -> {
                                // Follow artist · add to playlist · like, each in an outlined circle.
                                listOf(R.drawable.person, R.drawable.add, R.drawable.favorite_border).forEachIndexed { i, icon ->
                                    if (i > 0) Spacer(Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier.size(18.dp).border(0.6.dp, onMini.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(painterResource(icon), null, tint = onMini.copy(alpha = 0.75f), modifier = Modifier.size(9.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (slimNav) 7.dp else 9.dp))
            // Bottom nav — real tab icons; the default-open tab is highlighted; slim
            // hides the labels and tightens the row.
            val navItems = listOf(
                NavigationTab.HOME to R.drawable.home_filled,
                NavigationTab.SEARCH to R.drawable.search,
                null to R.drawable.grid_view,
                NavigationTab.LIBRARY to R.drawable.library_music_outlined,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                navItems.forEach { (tab, iconRes) ->
                    val active = tab != null && tab == defaultTab
                    val tint = if (active) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.55f)
                    // Mirror the chosen navigation bar style so the preview matches.
                    val activeShape = RoundedCornerShape(5.dp)
                    val highlight: Modifier = when {
                        !active -> Modifier
                        navStyle == NavBarStyle.PILL ->
                            Modifier.clip(RoundedCornerShape(50)).background(cs.primary.copy(alpha = 0.22f))
                        navStyle == NavBarStyle.GRADIENT ->
                            Modifier.clip(activeShape).background(
                                Brush.horizontalGradient(listOf(cs.primary, cs.tertiary)),
                            )
                        navStyle == NavBarStyle.OUTLINED ->
                            Modifier.clip(activeShape).background(cs.primary.copy(alpha = 0.14f))
                        else -> Modifier
                    }
                    val iconTint = if (active && navStyle == NavBarStyle.GRADIENT) cs.onPrimary else tint
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = highlight.padding(
                                horizontal = if (active && navStyle != NavBarStyle.UNDERLINE) 5.dp else 0.dp,
                                vertical = if (active && navStyle != NavBarStyle.UNDERLINE) 2.dp else 0.dp,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(if (slimNav) 11.dp else 13.dp),
                            )
                        }
                        if (!slimNav && navStyle != NavBarStyle.GRADIENT) {
                            Box(Modifier.width(12.dp).height(2.5.dp).clip(RoundedCornerShape(2.dp)).background(tint.copy(alpha = if (active) 1f else 0.5f)))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Trailing swatch that opens the free colour picker. Shows the user's own colour
 * once they have one, and a hue wheel as an invitation before that.
 */
@Composable
fun CustomPaletteItem(
    currentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 48.dp * 0.25f else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "customCorner",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "customBorder",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "customScale",
    )
    val shape = RoundedCornerShape(cornerRadius)
    val hueWheel = remember {
        Brush.sweepGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) })
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, MaterialTheme.colorScheme.inversePrimary, shape)
                } else {
                    Modifier
                }
            )
            .background(if (isSelected) SolidColor(currentColor) else hueWheel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.palette),
            contentDescription = stringResource(R.string.custom_color),
            tint = if (isSelected) {
                if (currentColor.luminance() > 0.5f) Color.Black else Color.White
            } else {
                Color.White
            },
            modifier = Modifier.size(22.dp),
        )
    }
}
