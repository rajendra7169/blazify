/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Player-design gallery: swipe through player LAYOUTS in a phone frame, each a
 * live mini mock rendered with the current dynamic colours; Apply persists the
 * choice. Colours stay album-art dynamic — only the layout changes.
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.PlayerDesignKey
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.player.PlayerDesign
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDesignScreen(navController: NavController) {
    val designs = remember { PlayerDesign.entries.toList() }
    val (activeId, setActiveId) = rememberPreference(PlayerDesignKey, PlayerDesign.CLASSIC.id)

    val pagerState = rememberPagerState(
        initialPage = designs.indexOfFirst { it.id == activeId }.coerceAtLeast(0),
        pageCount = { designs.size },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.player_theme)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 54.dp),
                pageSpacing = 14.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                val design = designs[page]
                val focused = page == pagerState.currentPage
                PhoneFrame(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = if (focused) 8.dp else 26.dp),
                ) {
                    when (design) {
                        PlayerDesign.CLASSIC -> ClassicMock()
                        PlayerDesign.RING -> RingMock()
                        PlayerDesign.FULL_ART -> FullArtMock()
                    }
                }
            }

            Text(
                text = stringResource(designs[pagerState.currentPage].nameRes),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                designs.indices.forEach { i ->
                    val on = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (on) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (on) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            ),
                    )
                }
            }

            val currentId = designs[pagerState.currentPage].id
            val applied = currentId == activeId
            Button(
                onClick = { setActiveId(currentId) },
                enabled = !applied,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(52.dp),
            ) {
                if (applied) {
                    Icon(painterResource(R.drawable.check), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.using), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                } else {
                    Text(stringResource(R.string.apply), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PhoneFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(9f / 19.2f)
            .clip(RoundedCornerShape(34.dp))
            .background(Color.Black)
            .padding(5.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

/* ---------- shared mock pieces ---------- */

@Composable
private fun MockIcon(res: Int, tint: Color = MaterialTheme.colorScheme.onSurface, size: Int = 18) {
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
}

@Composable
private fun MockTitleLines() {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(0.7f).height(11.dp).clip(CircleShape).background(cs.onSurface.copy(alpha = 0.85f)))
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth(0.45f).height(8.dp).clip(CircleShape).background(cs.onSurfaceVariant.copy(alpha = 0.6f)))
}

@Composable
private fun MockSlider() {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(cs.onSurface.copy(alpha = 0.2f))) {
        Box(Modifier.fillMaxWidth(0.4f).height(4.dp).clip(CircleShape).background(cs.primary))
    }
}

@Composable
private fun MockTransport(onColor: Color = MaterialTheme.colorScheme.onSurface) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MockIcon(R.drawable.shuffle, onColor)
        MockIcon(R.drawable.skip_previous, onColor)
        Box(Modifier.size(46.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.pause), null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
        }
        MockIcon(R.drawable.skip_next, onColor)
        MockIcon(R.drawable.repeat, onColor)
    }
}

@Composable
private fun MockArtBrush(): Brush {
    val cs = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(cs.primary, cs.tertiary))
}

/* ---------- CLASSIC ---------- */

@Composable
private fun ClassicMock() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MockIcon(R.drawable.expand_more)
            MockIcon(R.drawable.more_horiz)
        }
        Spacer(Modifier.weight(0.4f))
        Box(Modifier.fillMaxWidth(0.82f).aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(MockArtBrush()))
        Spacer(Modifier.height(18.dp))
        MockTitleLines()
        Spacer(Modifier.height(16.dp))
        MockSlider()
        Spacer(Modifier.weight(0.5f))
        MockTransport()
    }
}

/* ---------- RING ---------- */

@Composable
private fun RingMock() {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MockIcon(R.drawable.expand_more)
            MockIcon(R.drawable.more_horiz)
        }
        Spacer(Modifier.weight(0.5f))
        Box(contentAlignment = Alignment.Center) {
            // circular art
            Box(Modifier.fillMaxWidth(0.62f).aspectRatio(1f).clip(CircleShape).background(MockArtBrush()))
            // progress ring around it
            Canvas(Modifier.fillMaxWidth(0.72f).aspectRatio(1f)) {
                val stroke = 6.dp.toPx()
                drawArc(
                    color = cs.onSurface.copy(alpha = 0.18f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = cs.primary,
                    startAngle = -90f, sweepAngle = 250f, useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        MockTitleLines()
        Spacer(Modifier.weight(0.6f))
        MockTransport()
    }
}

/* ---------- FULL ART ---------- */

@Composable
private fun FullArtMock() {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        // art fills the whole frame
        Box(Modifier.fillMaxSize().background(MockArtBrush()))
        // bottom scrim
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.72f),
                ),
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MockIcon(R.drawable.expand_more, Color.White)
            MockIcon(R.drawable.more_horiz, Color.White)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(0.7f).height(11.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.45f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.6f)))
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth(0.4f).height(4.dp).clip(CircleShape).background(cs.primary))
            }
            Spacer(Modifier.height(16.dp))
            MockTransport(onColor = Color.White)
        }
    }
}
