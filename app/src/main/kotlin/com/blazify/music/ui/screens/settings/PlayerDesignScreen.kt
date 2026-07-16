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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.pointer.pointerInput
import com.blazify.music.utils.makeTimeString
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.blazify.music.constants.PlayerBackgroundStyle
import com.blazify.music.constants.PlayerBackgroundStyleKey
import com.blazify.music.ui.player.SeekableAlbumRing
import com.blazify.music.ui.player.VinylTurntable
import com.blazify.music.ui.theme.PlayerColorExtractor
import com.blazify.music.utils.rememberEnumPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Shadow
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



/* ---------- interactive previews with the real dynamic player background ---------- */

private const val PREVIEW_FALLBACK_PROGRESS = 0.35f

@Composable
private fun LivePreview(design: PlayerDesign, pc: PlayerConnection?) {
    val meta by remember(pc) { pc?.mediaMetadata ?: MutableStateFlow(null) }.collectAsState()
    val bgStyle by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.GRADIENT)
    val gradient = rememberPreviewGradient(meta, bgStyle)
    val textColor = when (bgStyle) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onSurface
        else -> Color.White
    }
    Box(Modifier.fillMaxSize()) {
        if (design != PlayerDesign.FULL_ART) {
            PreviewBackground(bgStyle, meta?.thumbnailUrl, gradient)
        }
        when (design) {
            PlayerDesign.CLASSIC -> ClassicPreview(meta, pc, textColor)
            PlayerDesign.RING -> RingPreview(meta, pc, textColor)
            PlayerDesign.FULL_ART -> FullArtPreview(meta, pc)
            PlayerDesign.RECORD -> RecordPreview(meta, pc, textColor)
        }
    }
}

/** Extract the same album-art gradient colours the real player uses (GRADIENT style). */
@Composable
private fun rememberPreviewGradient(meta: MediaMetadata?, bgStyle: PlayerBackgroundStyle): List<Color> {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.surface.toArgb()
    var colors by remember { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(meta?.id, bgStyle) {
        if (bgStyle != PlayerBackgroundStyle.GRADIENT || meta?.thumbnailUrl == null) {
            colors = emptyList()
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(meta.thumbnailUrl)
                .size(100, 100)
                .allowHardware(false)
                .build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            val bitmap = result?.image?.toBitmap()
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate()
                }
                val extracted = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallback)
                withContext(Dispatchers.Main) { colors = extracted }
            }
        }
    }
    return colors
}

@Composable
private fun PreviewBackground(bgStyle: PlayerBackgroundStyle, thumbnailUrl: String?, gradient: List<Color>) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
        when (bgStyle) {
            PlayerBackgroundStyle.BLUR -> {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(thumbnailUrl).size(100, 100).allowHardware(false).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(45.dp),
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
                }
            }
            PlayerBackgroundStyle.GRADIENT -> {
                if (gradient.size >= 3) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(colorStops = arrayOf(0.0f to gradient[0], 0.5f to gradient[1], 1.0f to gradient[2])),
                        ),
                    )
                } else if (gradient.isNotEmpty()) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(0.0f to gradient[0], 0.6f to gradient[0].copy(alpha = 0.7f), 1.0f to Color.Black),
                        ),
                    )
                }
            }
            PlayerBackgroundStyle.DEFAULT -> { /* theme surface already painted */ }
        }
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
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.clip(shape))
    } else {
        Box(modifier.clip(shape).background(previewArtBrush()))
    }
}

@Composable
private fun PreviewTitle(meta: MediaMetadata?, color: Color, shadow: Boolean = false) {
    val sh = if (shadow) Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 2f), 6f) else null
    Text(
        meta?.title ?: "Song title",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color,
        style = LocalTextStyle.current.copy(shadow = sh),
    )
    Spacer(Modifier.height(3.dp))
    Text(
        meta?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() } ?: "Artist",
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color.copy(alpha = 0.75f),
        style = LocalTextStyle.current.copy(shadow = sh),
    )
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
private fun PreviewPillButton(res: Int, bg: Color, tint: Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier.size(26.dp).clip(CircleShape).background(bg).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
    }
}

/** Live playback position (ms) + duration (ms), polled. */
@Composable
private fun rememberLivePosition(pc: PlayerConnection?): Pair<Long, Long> {
    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }
    LaunchedEffect(pc) {
        while (pc != null) {
            pos = pc.player.currentPosition
            dur = pc.player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }
    return pos to dur
}

/** Slim, seekable progress bar (tap/drag) matching the app's default slider look. */
@Composable
private fun PreviewSlider(pc: PlayerConnection?, activeColor: Color, inactiveColor: Color, textColor: Color) {
    val (pos, dur) = rememberLivePosition(pc)
    var dragFrac by remember { mutableStateOf<Float?>(null) }
    val frac = dragFrac ?: if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else PREVIEW_FALLBACK_PROGRESS
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .pointerInput(dur) {
                detectTapGestures { off -> if (dur > 0) pc?.player?.seekTo((off.x / size.width * dur).toLong()) }
            }
            .pointerInput(dur) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> dragFrac = (off.x / size.width).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, _ -> dragFrac = (change.position.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragFrac?.let { if (dur > 0) pc?.player?.seekTo((it * dur).toLong()) }; dragFrac = null },
                    onDragCancel = { dragFrac = null },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(inactiveColor)) {
            Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(CircleShape).background(activeColor))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(makeTimeString((frac * dur).toLong()), fontSize = 9.sp, color = textColor.copy(alpha = 0.8f))
        Text(if (dur > 0) makeTimeString(dur) else "0:00", fontSize = 9.sp, color = textColor.copy(alpha = 0.8f))
    }
}

@Composable
private fun PreviewTransport(pc: PlayerConnection?, onColor: Color) {
    val cs = MaterialTheme.colorScheme
    val isPlaying by remember(pc) { pc?.isPlaying ?: MutableStateFlow(false) }.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniIcon(R.drawable.shuffle, onColor, 18)
        MiniIcon(R.drawable.skip_previous, onColor, 22) { pc?.seekToPrevious() }
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(cs.primary).clickable { pc?.togglePlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
        }
        MiniIcon(R.drawable.skip_next, onColor, 22) { pc?.seekToNext() }
        MiniIcon(R.drawable.repeat, onColor, 18)
    }
}

@Composable
private fun PreviewFavorite(pc: PlayerConnection?, color: Color) {
    val song by remember(pc) { pc?.currentSong ?: MutableStateFlow(null) }.collectAsState()
    val liked = song?.song?.liked == true
    MiniIcon(
        if (liked) R.drawable.favorite else R.drawable.favorite_border,
        if (liked) MaterialTheme.colorScheme.error else color,
        18,
    ) { pc?.toggleLike() }
}

/** Collapsed queue peek bar (Queue - Sleep timer - Lyrics) shown at the bottom of the real player. */
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
private fun ClassicPreview(meta: MediaMetadata?, pc: PlayerConnection?, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.now_playing), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.weight(0.4f))
        PreviewArt(meta?.thumbnailUrl, RoundedCornerShape(20.dp), Modifier.fillMaxWidth(0.82f).aspectRatio(1f))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PreviewTitle(meta, textColor) }
            PreviewFavorite(pc, textColor)
            Spacer(Modifier.width(10.dp))
            PreviewPillButton(R.drawable.palette, textColor.copy(alpha = 0.14f), textColor)
            Spacer(Modifier.width(8.dp))
            PreviewPillButton(R.drawable.more_horiz, textColor.copy(alpha = 0.14f), textColor)
        }
        Spacer(Modifier.height(12.dp))
        PreviewSlider(pc, cs.primary, textColor.copy(alpha = 0.22f), textColor)
        Spacer(Modifier.weight(0.5f))
        PreviewTransport(pc, textColor)
        Spacer(Modifier.weight(0.4f))
        PreviewQueuePeek(textColor)
    }
}

/* ---------- RING ---------- */

@Composable
private fun RingPreview(meta: MediaMetadata?, pc: PlayerConnection?, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    val (pos, dur) = rememberLivePosition(pc)
    val progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else PREVIEW_FALLBACK_PROGRESS
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MiniIcon(R.drawable.expand_more, textColor, 22)
            Text(
                text = stringResource(R.string.now_playing),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            MiniIcon(R.drawable.palette, textColor, 20)
        }
        Spacer(Modifier.weight(0.4f))
        SeekableAlbumRing(
            thumbnailUrl = meta?.thumbnailUrl,
            progress = progress,
            ringColor = cs.primary,
            trackColor = textColor.copy(alpha = 0.20f),
            onSeek = { f -> if (dur > 0) pc?.player?.seekTo((f * dur).toLong()) },
            modifier = Modifier.fillMaxWidth(0.66f).aspectRatio(1f),
            ringStrokeDp = 5f,
            artPaddingDp = 9f,
            fallbackBrush = previewArtBrush(),
            thumbColor = cs.primary,
        )
        Spacer(Modifier.weight(0.4f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniIcon(R.drawable.queue_music, textColor, 20)
            PreviewFavorite(pc, textColor)
        }
        Spacer(Modifier.height(6.dp))
        PreviewSlider(pc, cs.primary, textColor.copy(alpha = 0.22f), textColor)
        Spacer(Modifier.height(8.dp))
        PreviewTransport(pc, textColor)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            MiniIcon(R.drawable.bedtime, textColor, 18)
            MiniIcon(R.drawable.more_horiz, textColor, 18)
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.show_lyrics), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f))
                MiniIcon(R.drawable.expand_less, textColor, 16)
            }
            Spacer(Modifier.height(6.dp))
            Text("In the stillness of the night", fontSize = 9.sp, color = textColor.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            Text("I feel the weight, the empty sight", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            Text("Whispers in my mind, they call", fontSize = 9.sp, color = textColor.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/* ---------- RECORD ---------- */

@Composable
private fun RecordPreview(meta: MediaMetadata?, pc: PlayerConnection?, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    val isPlaying by remember(pc) { pc?.isPlaying ?: MutableStateFlow(false) }.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.now_playing), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.weight(0.3f))
        VinylTurntable(
            thumbnailUrl = meta?.thumbnailUrl,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxWidth(0.94f).aspectRatio(1f),
            fallbackBrush = previewArtBrush(),
        )
        Spacer(Modifier.weight(0.3f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PreviewTitle(meta, textColor) }
            PreviewFavorite(pc, textColor)
            Spacer(Modifier.width(10.dp))
            PreviewPillButton(R.drawable.palette, textColor.copy(alpha = 0.14f), textColor)
            Spacer(Modifier.width(8.dp))
            PreviewPillButton(R.drawable.more_horiz, textColor.copy(alpha = 0.14f), textColor)
        }
        Spacer(Modifier.height(12.dp))
        PreviewSlider(pc, cs.primary, textColor.copy(alpha = 0.22f), textColor)
        Spacer(Modifier.height(10.dp))
        PreviewTransport(pc, textColor)
        Spacer(Modifier.height(10.dp))
        PreviewQueuePeek(textColor)
    }
}

/* ---------- FULL ART ---------- */

@Composable
private fun FullArtPreview(meta: MediaMetadata?, pc: PlayerConnection?) {
    Box(Modifier.fillMaxSize()) {
        PreviewArt(meta?.thumbnailUrl, RoundedCornerShape(0.dp), Modifier.fillMaxSize())
        FullArtScrim()
        // "Now Playing" centred at the top (no minimize / more icons here).
        Text(
            text = stringResource(R.string.now_playing),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = LocalTextStyle.current.copy(shadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 2f), 6f)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { PreviewTitle(meta, Color.White, shadow = true) }
                PreviewFavorite(pc, Color.White)
                Spacer(Modifier.width(10.dp))
                PreviewPillButton(R.drawable.palette, Color.White.copy(alpha = 0.18f), Color.White)
                Spacer(Modifier.width(8.dp))
                PreviewPillButton(R.drawable.more_horiz, Color.White.copy(alpha = 0.18f), Color.White)
            }
            Spacer(Modifier.height(10.dp))
            PreviewSlider(pc, MaterialTheme.colorScheme.primary, Color.White.copy(alpha = 0.25f), Color.White)
            Spacer(Modifier.height(10.dp))
            PreviewTransport(pc, Color.White)
            Spacer(Modifier.height(12.dp))
            PreviewQueuePeek(Color.White)
        }
    }
}

@Composable
private fun FullArtScrim() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.30f),
                0.35f to Color.Transparent,
                0.65f to Color.Black.copy(alpha = 0.55f),
                1.0f to Color.Black.copy(alpha = 0.92f),
            ),
        ),
    )
}
