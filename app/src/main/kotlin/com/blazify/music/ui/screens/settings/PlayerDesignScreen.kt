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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.PlayerDesignKey
import com.blazify.music.models.MediaMetadata
import com.blazify.music.playback.PlayerConnection
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.player.PlayerDesign
import com.blazify.music.ui.player.SeekableAlbumRing
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDesignScreen(navController: NavController) {
    val designs = remember { PlayerDesign.entries.toList() }
    val playerConnection = LocalPlayerConnection.current
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

/* ---------- live preview (real song, real controls) ---------- */

@Composable
private fun LivePreview(design: PlayerDesign, pc: PlayerConnection?) {
    when (design) {
        PlayerDesign.CLASSIC -> LiveClassic(pc)
        PlayerDesign.RING -> LiveRing(pc)
        PlayerDesign.FULL_ART -> LiveFullArt(pc)
    }
}

/** Poll real playback position → 0..1 fraction. */
@Composable
private fun rememberLiveProgress(pc: PlayerConnection?): Float {
    var frac by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(pc) {
        while (pc != null) {
            val d = pc.player.duration
            frac = if (d > 0) (pc.player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(500)
        }
    }
    return frac
}

@Composable
private fun mockArtBrush(): Brush {
    val cs = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(cs.primary, cs.tertiary))
}

@Composable
private fun LiveArt(url: String?, shape: Shape, modifier: Modifier = Modifier) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(modifier.clip(shape).background(mockArtBrush()))
    }
}

@Composable
private fun LiveTitle(meta: MediaMetadata?, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = meta?.title ?: "—",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color,
    )
    Spacer(Modifier.height(3.dp))
    Text(
        text = meta?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() } ?: "",
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color.copy(alpha = 0.6f),
    )
}

@Composable
private fun LiveProgressBar(progress: Float, trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)) {
    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(trackColor)) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun MiniIcon(res: Int, tint: Color, size: Int = 18, onClick: (() -> Unit)? = null) {
    val base = Modifier.size(size.dp)
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = tint,
        modifier = if (onClick != null) base.clip(CircleShape).clickable(onClick = onClick) else base,
    )
}

@Composable
private fun LiveTransport(pc: PlayerConnection?, onColor: Color = MaterialTheme.colorScheme.onSurface) {
    val cs = MaterialTheme.colorScheme
    val isPlaying by (pc?.isPlaying ?: return StaticTransport(onColor)).collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniIcon(R.drawable.shuffle, onColor)
        MiniIcon(R.drawable.skip_previous, onColor, 22) { pc?.player?.seekToPreviousMediaItem() }
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(cs.primary).clickable { pc?.togglePlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = cs.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        MiniIcon(R.drawable.skip_next, onColor, 22) { pc?.player?.seekToNext() }
        MiniIcon(R.drawable.repeat, onColor)
    }
}

/** Non-interactive transport when there is no playing song. */
@Composable
private fun StaticTransport(onColor: Color) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniIcon(R.drawable.shuffle, onColor)
        MiniIcon(R.drawable.skip_previous, onColor, 22)
        Box(Modifier.size(46.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.play), null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
        }
        MiniIcon(R.drawable.skip_next, onColor, 22)
        MiniIcon(R.drawable.repeat, onColor)
    }
}

/* ---------- CLASSIC ---------- */

@Composable
private fun LiveClassic(pc: PlayerConnection?) {
    val meta by (pc?.mediaMetadata ?: return ClassicFallback()).collectAsState()
    val progress = rememberLiveProgress(pc)
    val onColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniIcon(R.drawable.expand_more, onColor)
            MiniIcon(R.drawable.more_horiz, onColor)
        }
        Spacer(Modifier.weight(0.4f))
        LiveArt(meta?.thumbnailUrl, RoundedCornerShape(20.dp), Modifier.fillMaxWidth(0.82f).aspectRatio(1f))
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) { LiveTitle(meta) }
        Spacer(Modifier.height(14.dp))
        LiveProgressBar(progress)
        Spacer(Modifier.weight(0.5f))
        LiveTransport(pc, onColor)
    }
}

@Composable
private fun ClassicFallback() {
    val onColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniIcon(R.drawable.expand_more, onColor)
            MiniIcon(R.drawable.more_horiz, onColor)
        }
        Spacer(Modifier.weight(0.4f))
        LiveArt(null, RoundedCornerShape(20.dp), Modifier.fillMaxWidth(0.82f).aspectRatio(1f))
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth()) { LiveTitle(null) }
        Spacer(Modifier.height(14.dp))
        LiveProgressBar(0.35f)
        Spacer(Modifier.weight(0.5f))
        StaticTransport(onColor)
    }
}

/* ---------- RING ---------- */

@Composable
private fun LiveRing(pc: PlayerConnection?) {
    val meta by (pc?.mediaMetadata ?: return RingBody(null, 0.35f, null)).collectAsState()
    val progress = rememberLiveProgress(pc)
    RingBody(meta, progress, pc)
}

@Composable
private fun RingFallback() = RingBody(null, 0.35f, null)

@Composable
private fun RingBody(meta: MediaMetadata?, progress: Float, pc: PlayerConnection?) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // top bar: minimize / Now Playing / theme
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
        SeekableAlbumRing(
            thumbnailUrl = meta?.thumbnailUrl,
            progress = progress,
            ringColor = cs.primary,
            trackColor = cs.onSurface.copy(alpha = 0.18f),
            onSeek = { f ->
                val d = pc?.player?.duration ?: 0L
                if (d > 0) pc?.player?.seekTo((f * d).toLong())
            },
            modifier = Modifier.fillMaxWidth(0.66f).aspectRatio(1f),
            ringStrokeDp = 5f,
            artPaddingDp = 9f,
            fallbackBrush = mockArtBrush(),
            thumbColor = cs.primary,
        )
        Spacer(Modifier.weight(0.4f))
        // queue + favourite
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniIcon(R.drawable.queue_music, cs.onSurface, 20)
            MiniIcon(R.drawable.favorite, MaterialTheme.colorScheme.error, 20)
        }
        Spacer(Modifier.height(6.dp))
        LiveProgressBar(progress)
        Spacer(Modifier.height(10.dp))
        if (pc != null) LiveTransport(pc, cs.onSurface) else StaticTransport(cs.onSurface)
        Spacer(Modifier.height(8.dp))
        // sleep timer (left, above the lyrics card)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
            MiniIcon(R.drawable.bedtime, cs.onSurface, 18)
        }
        Spacer(Modifier.height(6.dp))
        // lyrics card: header + partial lines (current highlighted)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.30f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
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

/* ---------- FULL ART ---------- */

@Composable
private fun LiveFullArt(pc: PlayerConnection?) {
    val meta by (pc?.mediaMetadata ?: return FullArtFallback()).collectAsState()
    val progress = rememberLiveProgress(pc)
    Box(Modifier.fillMaxSize()) {
        LiveArt(meta?.thumbnailUrl, RoundedCornerShape(0.dp), Modifier.fillMaxSize())
        FullArtScrim()
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniIcon(R.drawable.expand_more, Color.White)
            MiniIcon(R.drawable.more_horiz, Color.White)
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.fillMaxWidth()) { LiveTitle(meta, Color.White) }
            Spacer(Modifier.height(12.dp))
            LiveProgressBar(progress, trackColor = Color.White.copy(alpha = 0.25f))
            Spacer(Modifier.height(14.dp))
            LiveTransport(pc, Color.White)
        }
    }
}

@Composable
private fun FullArtFallback() {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(mockArtBrush()))
        FullArtScrim()
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.fillMaxWidth()) { LiveTitle(null, Color.White) }
            Spacer(Modifier.height(12.dp))
            LiveProgressBar(0.35f, trackColor = Color.White.copy(alpha = 0.25f))
            Spacer(Modifier.height(14.dp))
            StaticTransport(Color.White)
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
