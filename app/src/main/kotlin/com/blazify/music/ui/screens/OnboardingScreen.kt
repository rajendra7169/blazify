/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * First-run onboarding: four pages that show what Blazify does, each with a pair
 * of phone frames illustrating the feature. Shown once, then never again.
 */

package com.blazify.music.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.R
import androidx.compose.ui.graphics.lerp
import com.blazify.music.ui.screens.settings.DarkMode
import com.blazify.music.ui.screens.settings.LyricsPosition
import com.blazify.music.ui.screens.settings.LyricsSampleInterior
import com.blazify.music.ui.screens.settings.ThemePhoneFrame
import com.blazify.music.ui.screens.settings.ThemePhonePreview
import com.blazify.music.ui.theme.BlazifyTheme
import com.blazify.music.ui.theme.BlazeGradientEnd
import com.blazify.music.ui.theme.BlazeThemeColor
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.launch

/** Which mock the phone frames show — one per page, matching what the copy claims. */
private enum class OnboardScreen { HOME, LYRICS, TOGETHER, THEME }

private data class OnboardPage(
    val titleRes: Int,
    val bodyRes: Int,
    val iconRes: Int,
    /** Front frame — the screen this page is actually about. */
    val front: OnboardScreen,
    /** Back frame — a second, different screen so the pair doesn't read as a duplicate. */
    val back: OnboardScreen,
)

/**
 * @param onFinish called when the user completes or skips onboarding.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardPage(R.string.onboard_1_title, R.string.onboard_1_body, R.drawable.play, OnboardScreen.HOME, OnboardScreen.LYRICS),
            OnboardPage(R.string.onboard_2_title, R.string.onboard_2_body, R.drawable.lyrics, OnboardScreen.LYRICS, OnboardScreen.HOME),
            OnboardPage(R.string.onboard_3_title, R.string.onboard_3_body, R.drawable.group_add, OnboardScreen.TOGETHER, OnboardScreen.HOME),
            OnboardPage(R.string.onboard_4_title, R.string.onboard_4_body, R.drawable.gradient, OnboardScreen.THEME, OnboardScreen.LYRICS),
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Fully opaque: an alpha stop here let the app behind show through.
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black,
                        lerp(Color.Black, BlazeThemeColor, 0.14f),
                        Color.Black,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // Skip
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.onboard_skip), color = Color.White.copy(alpha = 0.7f))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                OnboardPageContent(page = pages[page], index = page)
            }

            // Dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.indices.forEach { i ->
                    val active = i == pagerState.currentPage
                    val width by animateDpAsState(if (active) 22.dp else 7.dp, label = "dot")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(width)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) {
                                    Brush.horizontalGradient(listOf(BlazeThemeColor, BlazeGradientEnd))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.22f)),
                                    )
                                },
                            ),
                    )
                }
            }

            Button(
                onClick = {
                    if (isLast) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlazeThemeColor,
                    contentColor = Color.Black,
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(
                    text = stringResource(if (isLast) R.string.onboard_start else R.string.onboard_next),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun OnboardPageContent(page: OnboardPage, index: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // A pair of phone frames — the back one tilted behind, the front one holding
        // a simple mock of the feature being described.
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Back frame: a second, different screen, tilted away behind.
            ThemePhoneFrame(
                modifier = Modifier
                    .fillMaxHeight(0.66f)
                    .graphicsLayer {
                        rotationZ = -9f
                        translationX = -70f
                        alpha = 0.55f
                    },
            ) {
                OnboardInterior(screen = page.back)
            }
            // Front frame: the screen this page is about.
            ThemePhoneFrame(modifier = Modifier.fillMaxHeight(0.78f)) {
                OnboardInterior(screen = page.front)
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(page.titleRes),
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 31.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.bodyRes),
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 14.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

/**
 * Picks the mock shown inside a phone frame. HOME and LYRICS reuse the same
 * previews the Look & Feel hub renders; TOGETHER and THEME are drawn after the
 * real Listen Together and Look & Feel screens, card for card.
 */
@Composable
private fun OnboardInterior(screen: OnboardScreen) {
    // A fresh install opens in Blaze amber on pure black, so that's what the
    // phones show; a different tint per page made them look like another app.
    val accent = BlazeThemeColor
    val pureBlack = true
    when (screen) {
        OnboardScreen.HOME ->
            ThemePhonePreview(darkMode = DarkMode.ON, pureBlack = pureBlack, themeColor = accent)
        OnboardScreen.LYRICS ->
            LyricsSampleInterior(
                darkMode = DarkMode.ON,
                pureBlack = pureBlack,
                themeColor = accent,
                position = LyricsPosition.CENTER,
            )
        OnboardScreen.TOGETHER -> TogetherSampleInterior(accent, pureBlack)
        OnboardScreen.THEME -> LookAndFeelSampleInterior(accent, pureBlack)
    }
}

/**
 * Listen Together as it looks once you're hosting a room: the connection card,
 * the room code with its copy buttons, and who's listening. Same cards, colours
 * and labels as the real screen, just smaller.
 */
@Composable
private fun TogetherSampleInterior(accent: Color, pureBlack: Boolean) {
    BlazifyTheme(darkTheme = true, pureBlack = pureBlack, themeColor = accent) {
        val cs = MaterialTheme.colorScheme
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(horizontal = 9.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            // Top bar: back · logo · Blaze Together.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            ) {
                Icon(painterResource(R.drawable.arrow_back), null, tint = cs.onSurface, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(6.dp))
                Image(painterResource(R.drawable.blaze_logo), null, modifier = Modifier.size(9.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.blaze_together), color = cs.onSurface, fontSize = 9.sp, lineHeight = 10.sp)
            }

            // Connection card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.primaryContainer)
                    .padding(7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(cs.primary))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.listen_together_connected),
                        color = cs.primary, fontSize = 7.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    SamplePill(stringResource(R.string.disconnect), cs.primary, cs.onPrimary, Modifier.weight(1f))
                    SamplePill("Reconnect", cs.secondaryContainer, cs.onSecondaryContainer, Modifier.weight(1f))
                }
            }

            // Room code card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceContainerHighest)
                    .padding(horizontal = 7.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.room_code), color = cs.onSurfaceVariant, fontSize = 5.5.sp, lineHeight = 6.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    "K7M2QX9P",
                    color = cs.primary, fontSize = 13.sp, lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp, maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.listen_together_you_are_host), color = cs.onSurfaceVariant, fontSize = 5.5.sp, lineHeight = 6.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SamplePill(stringResource(R.string.copy_link), cs.secondaryContainer, cs.onSecondaryContainer, icon = R.drawable.link)
                    SamplePill(stringResource(R.string.copy_code), cs.secondaryContainer, cs.onSecondaryContainer, icon = R.drawable.content_copy)
                }
            }

            // Who's in the room — the host gets the crown.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.surfaceContainerHigh)
                    .padding(7.dp),
            ) {
                Text(
                    "${stringResource(R.string.connected_users)} (3)",
                    color = cs.primary, fontSize = 7.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SampleListener("Alex", host = true)
                    SampleListener("Maya", host = false)
                    SampleListener("Sam", host = false)
                }
            }
        }
    }
}

/** A small button shape from the room screen, optionally with a leading icon. */
@Composable
private fun SamplePill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: Int? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(container)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, tint = content, modifier = Modifier.size(6.dp))
            Spacer(Modifier.width(2.dp))
        }
        Text(text, color = content, fontSize = 5.5.sp, lineHeight = 6.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** One avatar from the connected users row: initial in a circle, name, and Host under the host. */
@Composable
private fun SampleListener(name: String, host: Boolean) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (host) cs.primary else cs.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1),
                    color = if (host) cs.onPrimary else cs.onSurfaceVariant,
                    fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold,
                )
            }
            if (host) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(cs.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.crown), null, tint = cs.onPrimary, modifier = Modifier.size(5.dp))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(name, color = if (host) cs.primary else cs.onSurface, fontSize = 5.5.sp, lineHeight = 6.sp, fontWeight = FontWeight.Medium)
        if (host) {
            Text(stringResource(R.string.host_label), color = cs.primary.copy(alpha = 0.8f), fontSize = 4.5.sp, lineHeight = 5.sp)
        }
    }
}

/** Size of the phone preview pinned at the top of the real Look & Feel hub. */
private val HubPreviewWidth = 198.dp
private val HubPreviewHeight = 425.dp

/**
 * The Look & Feel hub as it really is: the live phone preview on top, the
 * Theme / Player / Mini-player / Lyrics tabs, and the Theme tab's mode circles
 * and colour palette underneath.
 */
@Composable
private fun LookAndFeelSampleInterior(accent: Color, pureBlack: Boolean) {
    BlazifyTheme(darkTheme = true, pureBlack = pureBlack, themeColor = accent) {
        val cs = MaterialTheme.colorScheme
        BoxWithConstraints(Modifier.fillMaxSize().background(cs.background)) {
            // The hub's own preview, laid out at its real size and shrunk to fit,
            // so it is the same home mock the hub shows rather than a sketch of it.
            val previewHeight = maxHeight * 0.44f
            val scale = previewHeight / HubPreviewHeight
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top bar: back · Look & Feel.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Icon(painterResource(R.drawable.arrow_back), null, tint = cs.onSurface, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.look_and_feel), color = cs.onSurface, fontSize = 10.sp, lineHeight = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(HubPreviewWidth * scale, previewHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    ThemePhoneFrame(
                        modifier = Modifier
                            .requiredSize(HubPreviewWidth, HubPreviewHeight)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                    ) {
                        ThemePhonePreview(darkMode = DarkMode.ON, pureBlack = pureBlack, themeColor = accent)
                    }
                }
                Spacer(Modifier.height(9.dp))

                // Tab strip, Theme selected; it runs off the edge like the real one.
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState(), enabled = false),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(R.string.theme, R.string.player, R.string.mini_player, R.string.lyrics).forEachIndexed { i, label ->
                        val active = i == 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) cs.primary else cs.surfaceContainerHighest)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                stringResource(label),
                                color = if (active) cs.onPrimary else cs.onSurfaceVariant,
                                fontSize = 6.sp, lineHeight = 7.sp, maxLines = 1,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Theme tab controls.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(stringResource(R.string.theme_mode), color = cs.onSurface, fontSize = 7.5.sp, lineHeight = 8.sp)
                    // System (selected) · divider · light · dark · pure black.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.5.dp, cs.inversePrimary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.sync), null, tint = Color.White, modifier = Modifier.size(9.dp))
                        }
                        Box(Modifier.width(0.6.dp).height(14.dp).background(cs.outlineVariant))
                        listOf(Color(0xFFFFF8F5), Color(0xFF221A17), Color.Black).forEach { fill ->
                            Box(Modifier.size(20.dp).clip(CircleShape).background(fill))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.color_palette), color = cs.onSurface, fontSize = 7.5.sp, lineHeight = 8.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState(), enabled = false),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        // Dynamic colours first, then the presets with Blaze picked.
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(cs.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.palette), null, tint = cs.onSurfaceVariant, modifier = Modifier.size(10.dp))
                        }
                        listOf(
                            BlazeThemeColor, Color(0xFFEC5464), Color(0xFFD81B60), Color(0xFF8E24AA), Color(0xFF5E35B1),
                        ).forEach { seed ->
                            SamplePaletteSwatch(seed, selected = seed == BlazeThemeColor)
                        }
                    }
                }
            }
        }
    }
}

/** A palette swatch drawn the way the Theme tab draws it: a top half and two bottom quarters. */
@Composable
private fun SamplePaletteSwatch(seed: Color, selected: Boolean) {
    val scheme = rememberDynamicColorScheme(seedColor = seed, isDark = true, style = PaletteStyle.TonalSpot)
    val shape = if (selected) RoundedCornerShape(5.dp) else CircleShape
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(shape)
            .then(if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.inversePrimary, shape) else Modifier),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val half = Size(size.width / 2, size.height / 2)
            drawRect(scheme.onPrimary, size = Size(size.width, size.height / 2))
            drawRect(scheme.secondary, topLeft = Offset(0f, size.height / 2), size = half)
            drawRect(scheme.tertiary, topLeft = Offset(size.width / 2, size.height / 2), size = half)
        }
    }
}
