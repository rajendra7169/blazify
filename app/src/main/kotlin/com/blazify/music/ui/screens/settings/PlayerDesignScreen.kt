/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Player-design gallery: swipe through player LAYOUTS inside a phone frame. Each
 * page is a LIVE preview of the real player rendered with the currently-playing
 * song — real album art, working transport buttons and a real (seekable on Ring)
 * progress bar. Apply persists the choice. Colours stay album-art dynamic; only
 * the layout changes. Reached from the full player's top-right theme icon.
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
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
import com.blazify.music.LocalPlayerBottomSheetState
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.PlayerDesignKey
import com.blazify.music.models.MediaMetadata
import com.blazify.music.playback.PlayerConnection
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.player.PlayerDesign
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDesignScreen(navController: NavController) {
    val designs = remember { PlayerDesign.entries.toList() }
    val playerConnection = LocalPlayerConnection.current
    val playerSheetState = LocalPlayerBottomSheetState.current
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
                contentPadding = PaddingValues(horizontal = 62.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                val design = designs[page]
                val focused = page == pagerState.currentPage
                Box(
                    modifier = Modifier.fillMaxSize().padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PhoneFrame(
                        modifier = Modifier
                            .fillMaxHeight(if (focused) 0.94f else 0.82f),
                    ) {
                        LivePreview(design, playerConnection)
                    }
                }
            }

            Text(
                text = stringResource(designs[pagerState.currentPage].nameRes),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
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
                onClick = {
                    setActiveId(currentId)
                    // Close the preview and reopen the full player with the applied design.
                    navController.navigateUp()
                    playerSheetState?.expandSoft()
                },
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
    val frameShape = RoundedCornerShape(40.dp)
    Box(
        modifier = modifier
            .aspectRatio(9f / 19.3f)
            // Drop shadow / glow so the frame pops off the dark background.
            .shadow(
                elevation = 26.dp,
                shape = frameShape,
                clip = false,
                ambientColor = Color.White.copy(alpha = 0.35f),
                spotColor = Color.White.copy(alpha = 0.55f),
            )
            .clip(frameShape)
            // Metallic bezel (lighter than pure black so it's visible in the dark).
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF44454A),
                        Color(0xFF26272B),
                        Color(0xFF1A1B1E),
                    ),
                ),
            )
            // Bright edge highlight to define the frame outline.
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
            .padding(7.dp)
            .clip(RoundedCornerShape(33.dp))
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
        // Top speaker slit for a realistic look.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 7.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
        )
    }
}


/* ---------- static design previews (faithful, non-interactive) ---------- */

private const val PREVIEW_PROGRESS = 0.35f

@Composable
private fun LivePreview(design: PlayerDesign, pc: PlayerConnection?) {
    val meta by remember(pc) { pc?.mediaMetadata ?: MutableStateFlow(null) }.collectAsState()
    when (design) {
        PlayerDesign.CLASSIC -> ClassicPreview(meta)
        PlayerDesign.RING -> RingPreview(meta)
        PlayerDesign.FULL_ART -> FullArtPreview(meta)
    }
}

@Composable
private fun previewArtBrush(): Brush {
    val cs = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(cs.primary, cs.tertiary))
}

@Composable
private fun PreviewArt(url: String?, shape: Shape, modifier: Modifier = Modifier) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(modifier.clip(shape).background(previewArtBrush()))
    }
}

@Composable
private fun PreviewTitle(meta: MediaMetadata?, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = meta?.title ?: "Song title",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color,
    )
    Spacer(Modifier.height(3.dp))
    Text(
        text = meta?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() } ?: "Artist",
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color.copy(alpha = 0.6f),
    )
}

/** Apple-Music-style slim bar matching the app's default slider look. */
@Composable
private fun PreviewSlider(activeColor: Color, inactiveColor: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(inactiveColor)) {
        Box(Modifier.fillMaxWidth(PREVIEW_PROGRESS).height(6.dp).clip(CircleShape).background(activeColor))
    }
}

@Composable
private fun PreviewTimes(color: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("1:02", fontSize = 9.sp, color = color)
        Text("2:59", fontSize = 9.sp, color = color)
    }
}

@Composable
private fun MiniIcon(res: Int, tint: Color, size: Int = 18) {
    Icon(painter = painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
}

@Composable
private fun PreviewTransport(onColor: Color, big: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniIcon(R.drawable.shuffle, onColor, 18)
        MiniIcon(R.drawable.skip_previous, onColor, 22)
        Box(
            Modifier.size(if (big) 52.dp else 46.dp).clip(CircleShape).background(cs.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.pause), null, tint = cs.onPrimary, modifier = Modifier.size(if (big) 24.dp else 22.dp))
        }
        MiniIcon(R.drawable.skip_next, onColor, 22)
        MiniIcon(R.drawable.repeat, onColor, 18)
    }
}

/** Collapsed queue peek bar (Queue · Sleep timer · Lyrics) shown at the bottom of the real player. */
@Composable
private fun PreviewQueuePeek(color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeekItem(R.drawable.queue_music, stringResource(R.string.queue), color)
        PeekItem(R.drawable.bedtime, stringResource(R.string.sleep_timer), color)
        PeekItem(R.drawable.lyrics, stringResource(R.string.lyrics), color)
    }
}

@Composable
private fun PeekItem(res: Int, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(res), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ---------- CLASSIC ---------- */

@Composable
private fun ClassicPreview(meta: MediaMetadata?) {
    val cs = MaterialTheme.colorScheme
    val onColor = cs.onSurface
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // "Now Playing" header (centred), like the real classic ThumbnailHeader.
        Text(
            text = stringResource(R.string.now_playing),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = onColor,
        )
        Spacer(Modifier.weight(0.4f))
        PreviewArt(meta?.thumbnailUrl, RoundedCornerShape(20.dp), Modifier.fillMaxWidth(0.82f).aspectRatio(1f))
        Spacer(Modifier.height(16.dp))
        // title/artist + favourite + theme + more (matches the real classic title row).
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PreviewTitle(meta) }
            MiniIcon(R.drawable.favorite_border, onColor, 18)
            Spacer(Modifier.width(10.dp))
            PreviewPillButton(R.drawable.palette, onColor.copy(alpha = 0.12f), onColor)
            Spacer(Modifier.width(8.dp))
            PreviewPillButton(R.drawable.more_horiz, onColor.copy(alpha = 0.12f), onColor)
        }
        Spacer(Modifier.height(14.dp))
        PreviewSlider(cs.primary, onColor.copy(alpha = 0.22f))
        PreviewTimes(onColor.copy(alpha = 0.7f))
        Spacer(Modifier.weight(0.5f))
        PreviewTransport(onColor)
        Spacer(Modifier.weight(0.4f))
        PreviewQueuePeek(onColor)
    }
}

/** Small filled pill button matching the real player's theme / more buttons. */
@Composable
private fun PreviewPillButton(res: Int, bg: Color, tint: Color) {
    Box(
        modifier = Modifier.size(26.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
    }
}

/* ---------- RING ---------- */

@Composable
private fun RingPreview(meta: MediaMetadata?) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MiniIcon(R.drawable.expand_more, cs.onSurface, 22)
            Text(
                text = stringResource(R.string.now_playing),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            MiniIcon(R.drawable.palette, cs.onSurface, 20)
        }
        Spacer(Modifier.weight(0.4f))
        StaticRing(meta?.thumbnailUrl, Modifier.fillMaxWidth(0.66f).aspectRatio(1f))
        Spacer(Modifier.weight(0.4f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniIcon(R.drawable.queue_music, cs.onSurface, 20)
            MiniIcon(R.drawable.favorite, cs.error, 20)
        }
        Spacer(Modifier.height(6.dp))
        PreviewSlider(cs.primary, cs.onSurface.copy(alpha = 0.22f))
        PreviewTimes(cs.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        PreviewTransport(cs.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            MiniIcon(R.drawable.bedtime, cs.onSurface, 18)
            MiniIcon(R.drawable.more_horiz, cs.onSurface, 18)
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.show_lyrics),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f),
                )
                MiniIcon(R.drawable.expand_less, cs.onSurface, 16)
            }
            Spacer(Modifier.height(6.dp))
            Text("In the stillness of the night", fontSize = 9.sp, color = cs.onSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            Text("I feel the weight, the empty sight", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            Text("Whispers in my mind, they call", fontSize = 9.sp, color = cs.onSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Static (non-interactive) circular art + progress ring + thumb for the RING preview. */
@Composable
private fun StaticRing(thumbnailUrl: String?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val trackColor = cs.onSurface.copy(alpha = 0.18f)
    val ringColor = cs.primary
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(9.dp).clip(CircleShape),
            )
        } else {
            Box(Modifier.fillMaxSize().padding(9.dp).clip(CircleShape).background(previewArtBrush()))
        }
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = Size(d, d), style = Stroke(width = stroke, cap = StrokeCap.Round))
            drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * PREVIEW_PROGRESS, useCenter = false, topLeft = topLeft, size = Size(d, d), style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

/* ---------- FULL ART ---------- */

@Composable
private fun FullArtPreview(meta: MediaMetadata?) {
    Box(Modifier.fillMaxSize()) {
        PreviewArt(meta?.thumbnailUrl, RoundedCornerShape(0.dp), Modifier.fillMaxSize())
        FullArtScrim()
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniIcon(R.drawable.expand_more, Color.White)
            MiniIcon(R.drawable.more_horiz, Color.White)
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { PreviewTitle(meta, Color.White) }
                MiniIcon(R.drawable.favorite_border, Color.White, 18)
                Spacer(Modifier.width(10.dp))
                PreviewPillButton(R.drawable.palette, Color.White.copy(alpha = 0.18f), Color.White)
                Spacer(Modifier.width(8.dp))
                PreviewPillButton(R.drawable.more_horiz, Color.White.copy(alpha = 0.18f), Color.White)
            }
            Spacer(Modifier.height(12.dp))
            PreviewSlider(MaterialTheme.colorScheme.primary, Color.White.copy(alpha = 0.25f))
            PreviewTimes(Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(12.dp))
            PreviewTransport(Color.White)
            Spacer(Modifier.height(14.dp))
            PreviewQueuePeek(Color.White)
        }
    }
}

@Composable
private fun FullArtScrim() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0.30f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.78f),
            ),
        ),
    )
}
