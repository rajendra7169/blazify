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
import com.blazify.music.ui.theme.BlazeGradientEnd
import com.blazify.music.ui.theme.BlazeThemeColor
import kotlinx.coroutines.launch

private data class OnboardPage(
    val titleRes: Int,
    val bodyRes: Int,
    val iconRes: Int,
)

/**
 * @param onFinish called when the user completes or skips onboarding.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardPage(R.string.onboard_1_title, R.string.onboard_1_body, R.drawable.play),
            OnboardPage(R.string.onboard_2_title, R.string.onboard_2_body, R.drawable.lyrics),
            OnboardPage(R.string.onboard_3_title, R.string.onboard_3_body, R.drawable.group_add),
            OnboardPage(R.string.onboard_4_title, R.string.onboard_4_body, R.drawable.gradient),
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black,
                        BlazeThemeColor.copy(alpha = 0.10f),
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
            OnboardPhone(
                modifier = Modifier
                    .fillMaxHeight(0.66f)
                    .graphicsLayer {
                        rotationZ = -9f
                        translationX = -70f
                        scaleX = 0.86f
                        scaleY = 0.86f
                        alpha = 0.55f
                    },
                accent = BlazeGradientEnd,
                variant = index + 1,
            )
            OnboardPhone(
                modifier = Modifier.fillMaxHeight(0.78f),
                accent = BlazeThemeColor,
                variant = index,
                iconRes = page.iconRes,
            )
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

/** A small stylised phone frame holding a simple mock of the feature. */
@Composable
private fun OnboardPhone(
    modifier: Modifier = Modifier,
    accent: Color,
    variant: Int,
    iconRes: Int? = null,
) {
    val frameShape = RoundedCornerShape(26.dp)
    val transition = rememberInfiniteTransition(label = "onboardPhone")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .aspectRatio(9f / 19f)
            .clip(frameShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF3A3B40), Color(0xFF1A1B1E))))
            .border(1.dp, Color.White.copy(alpha = 0.16f), frameShape)
            .padding(6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(21.dp))
                .background(Color(0xFF0B0B0D))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            // Hero block, tinted by the page accent.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.55f)))),
                contentAlignment = Alignment.Center,
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                            },
                    )
                }
            }
            // A few content rows so it reads as a real screen.
            repeat(4) { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = if (row == 0) 0.22f else 0.12f)),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (row % 2 == 0) 0.72f else 0.58f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.20f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.11f)),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // Mini-player strip at the bottom.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.30f)),
            )
        }
    }
}
