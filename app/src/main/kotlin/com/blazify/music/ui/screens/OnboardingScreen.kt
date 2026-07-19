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
import com.blazify.music.ui.screens.settings.ThemePhoneFrame
import com.blazify.music.ui.screens.settings.ThemePhonePreview
import com.blazify.music.ui.theme.BlazeGradientEnd
import com.blazify.music.ui.theme.BlazeThemeColor
import kotlinx.coroutines.launch

private data class OnboardPage(
    val titleRes: Int,
    val bodyRes: Int,
    val iconRes: Int,
    val accent: Color,
)

/**
 * @param onFinish called when the user completes or skips onboarding.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardPage(R.string.onboard_1_title, R.string.onboard_1_body, R.drawable.play, BlazeThemeColor),
            OnboardPage(R.string.onboard_2_title, R.string.onboard_2_body, R.drawable.lyrics, Color(0xFF00ACC1)),
            OnboardPage(R.string.onboard_3_title, R.string.onboard_3_body, R.drawable.group_add, Color(0xFF8E24AA)),
            OnboardPage(R.string.onboard_4_title, R.string.onboard_4_body, R.drawable.gradient, Color(0xFF43A047)),
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
            // Back frame: the real app preview in a different accent, tilted away.
            ThemePhoneFrame(
                modifier = Modifier
                    .fillMaxHeight(0.66f)
                    .graphicsLayer {
                        rotationZ = -9f
                        translationX = -70f
                        alpha = 0.55f
                    },
            ) {
                ThemePhonePreview(
                    darkMode = DarkMode.ON,
                    pureBlack = true,
                    themeColor = page.accent,
                )
            }
            // Front frame: the real app preview in this page's accent.
            ThemePhoneFrame(modifier = Modifier.fillMaxHeight(0.78f)) {
                ThemePhonePreview(
                    darkMode = DarkMode.ON,
                    pureBlack = false,
                    themeColor = page.accent,
                )
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
