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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.launch

/** Which mock the phone frames show — one per page, matching what the copy claims. */
private enum class OnboardScreen { HOME, LYRICS, TOGETHER, THEME }

private data class OnboardPage(
    val titleRes: Int,
    val bodyRes: Int,
    val iconRes: Int,
    val accent: Color,
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
            OnboardPage(R.string.onboard_1_title, R.string.onboard_1_body, R.drawable.play, BlazeThemeColor, OnboardScreen.HOME, OnboardScreen.LYRICS),
            OnboardPage(R.string.onboard_2_title, R.string.onboard_2_body, R.drawable.lyrics, Color(0xFF00ACC1), OnboardScreen.LYRICS, OnboardScreen.HOME),
            OnboardPage(R.string.onboard_3_title, R.string.onboard_3_body, R.drawable.group_add, Color(0xFF8E24AA), OnboardScreen.TOGETHER, OnboardScreen.HOME),
            OnboardPage(R.string.onboard_4_title, R.string.onboard_4_body, R.drawable.gradient, Color(0xFF43A047), OnboardScreen.THEME, OnboardScreen.LYRICS),
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
                OnboardInterior(screen = page.back, accent = page.accent, pureBlack = true)
            }
            // Front frame: the screen this page is about.
            ThemePhoneFrame(modifier = Modifier.fillMaxHeight(0.78f)) {
                OnboardInterior(screen = page.front, accent = page.accent, pureBlack = false)
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
 * previews the Look & Feel hub renders, so onboarding shows the real thing;
 * TOGETHER and THEME are simple stand-ins for screens that have no preview yet.
 */
@Composable
private fun OnboardInterior(screen: OnboardScreen, accent: Color, pureBlack: Boolean) {
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
        OnboardScreen.THEME -> ThemePickerSampleInterior(accent, pureBlack)
    }
}

/** Listen Together: room code, the shared song, and who else is in the room. */
@Composable
private fun TogetherSampleInterior(accent: Color, pureBlack: Boolean) {
    BlazifyTheme(darkTheme = true, pureBlack = pureBlack, themeColor = accent) {
        val cs = MaterialTheme.colorScheme
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(lerp(cs.primary, Color.Black, 0.55f), cs.background)))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Text("Listen Together", color = Color.White, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            // Room code pill with a copy affordance.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("BLZ-4K92", color = Color.White, fontSize = 6.5.sp, lineHeight = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.width(4.dp))
                Icon(painterResource(R.drawable.content_copy), null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(7.dp))
            }
            Spacer(Modifier.height(12.dp))
            // The song everyone is hearing.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(cs.primary, lerp(cs.primary, Color.Black, 0.45f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.music_note), null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Playing in sync", color = Color.White, fontSize = 7.5.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("4 listeners", color = Color.White.copy(alpha = 0.65f), fontSize = 5.5.sp, lineHeight = 6.sp)
            Spacer(Modifier.height(10.dp))
            // Listener avatars — the host tinted, the rest neutral.
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (i == 0) cs.primary else Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.person), null, tint = Color.White, modifier = Modifier.size(9.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth(0.55f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(cs.primary))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** Customisation: the accent swatches plus a couple of the style toggles. */
@Composable
private fun ThemePickerSampleInterior(accent: Color, pureBlack: Boolean) {
    BlazifyTheme(darkTheme = true, pureBlack = pureBlack, themeColor = accent) {
        val cs = MaterialTheme.colorScheme
        val swatches = listOf(
            BlazeThemeColor, Color(0xFF00ACC1), Color(0xFF8E24AA),
            Color(0xFF43A047), Color(0xFFE53935), Color(0xFF3949AB),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Text("Theme", color = cs.onSurface, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Accent colour", color = cs.onSurfaceVariant, fontSize = 5.5.sp, lineHeight = 6.sp)
            Spacer(Modifier.height(5.dp))
            // Two rows of swatches; the page accent reads as selected.
            swatches.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    row.forEach { c ->
                        val selected = c == accent
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (selected) Modifier.border(1.5.dp, cs.onSurface, CircleShape)
                                    else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(painterResource(R.drawable.check), null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // A few style rows with switch-shaped pills.
            listOf("Dynamic colour" to true, "Pure black" to true, "Player style" to false).forEach { (label, on) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.5.dp),
                ) {
                    Text(label, color = cs.onSurface, fontSize = 6.sp, lineHeight = 6.5.sp, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (on) cs.primary else cs.onSurface.copy(alpha = 0.18f)),
                        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(Modifier.padding(horizontal = 1.5.dp).size(6.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // Live-preview strip, echoing the real hub.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(cs.primary, lerp(cs.primary, Color.Black, 0.4f)))),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 5.dp)) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(5.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Live preview", color = Color.White, fontSize = 6.sp, lineHeight = 6.5.sp, fontWeight = FontWeight.Bold)
                        Text("Updates as you tweak", color = Color.White.copy(alpha = 0.7f), fontSize = 5.sp, lineHeight = 5.5.sp)
                    }
                    Icon(painterResource(R.drawable.play), null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
