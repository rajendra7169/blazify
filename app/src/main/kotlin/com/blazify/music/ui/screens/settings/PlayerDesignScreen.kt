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
import com.blazify.music.ui.player.CassetteTape
import com.blazify.music.ui.player.SeekableAlbumRing
import com.blazify.music.ui.player.VinylTurntable
import com.blazify.music.ui.theme.PlayerColorExtractor
import com.blazify.music.constants.SliderStyle
import com.blazify.music.constants.SliderStyleKey
import com.blazify.music.utils.rememberEnumPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import com.blazify.music.db.entities.LyricsEntity
import com.blazify.music.lyrics.LyricsUtils
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.blazify.music.constants.SquigglySliderKey
import com.blazify.music.ui.component.SquigglySlider
import com.blazify.music.ui.component.WavySlider
import com.blazify.music.utils.rememberPreference
import com.blazify.music.ui.component.CapsuleSeekBar
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.player.PlayerDesign
import com.blazify.music.ui.utils.backToMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.blazify.music.utils.safeDataStoreEdit
import com.blazify.music.utils.get
import com.blazify.music.utils.dataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDesignScreen(navController: NavController) {
    val designs = remember { PlayerDesign.entries.toList() }
    val playerConnection = LocalPlayerConnection.current
    val playerSheetState = LocalPlayerBottomSheetState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Read the saved design before the first frame. The usual preference state starts on the
    // default for a frame, which showed Classic and "Apply" before jumping to the real design.
    val storedId = remember { context.dataStore.get(PlayerDesignKey, PlayerDesign.CLASSIC.id) }
    val activeId by remember {
        context.dataStore.data
            .map { it[PlayerDesignKey] ?: PlayerDesign.CLASSIC.id }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(storedId)
    val setActiveId: (String) -> Unit = { id ->
        scope.launch { context.safeDataStoreEdit { it[PlayerDesignKey] = id } }
    }

    val pagerState = rememberPagerState(
        initialPage = designs.indexOfFirst { it.id == activeId }.coerceAtLeast(0),
        pageCount = { designs.size },
    )

    // Whenever the stored design changes, land on it. Deliberately NOT rememberSaveable:
    // a saved "already jumped" flag survived re-entry and left the pager on the
    // default page. Guarded on isScrollInProgress so it never fights a swipe.
    LaunchedEffect(activeId) {
        val index = designs.indexOfFirst { it.id == activeId }
        if (index >= 0 && index != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(index)
        }
    }

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
        val applyButton: @Composable () -> Unit = {
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)),
        ) {
            // Held sideways there is no height for a standing phone, so the carousel takes the
            // left half and the name, dots and button the right.
            val sideBySide = maxWidth > maxHeight

            // Sideways there is no room for a phone at its own size, and simply squeezing the
            // frame left the preview inside it cut off. So it is laid out at a full phone's size
            // and the whole thing scaled down to the height there is.
            val previewWidth = 250.dp
            val previewHeight = previewWidth * 19.3f / 9f
            val landscapeScale = ((maxHeight - 24.dp) / previewHeight).coerceIn(0.3f, 1f)

            val carousel: @Composable (Modifier) -> Unit = { carouselModifier ->
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = if (sideBySide) 24.dp else 62.dp),
                    pageSpacing = 16.dp,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = carouselModifier,
                ) { page ->
                    val design = designs[page]
                    val focused = page == pagerState.currentPage
                    Box(
                        modifier = Modifier.fillMaxSize().padding(vertical = if (sideBySide) 8.dp else 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sideBySide) {
                            val scale = landscapeScale * if (focused) 1f else 0.88f
                            Box(
                                modifier = Modifier.size(previewWidth * scale, previewHeight * scale),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredSize(previewWidth, previewHeight)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                ) {
                                    PhoneFrame(modifier = Modifier.fillMaxSize()) {
                                        LivePreview(design, playerConnection)
                                    }
                                }
                            }
                        } else {
                            PhoneFrame(
                                modifier = Modifier
                                    .fillMaxHeight(if (focused) 0.94f else 0.82f),
                            ) {
                                LivePreview(design, playerConnection)
                            }
                        }
                    }
                }
            }

            if (sideBySide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    carousel(Modifier.weight(1f).fillMaxHeight())
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp),
                    ) {
                        DesignName(designs[pagerState.currentPage].nameRes)
                        DesignDots(designs.size, pagerState.currentPage)
                        applyButton()
                    }
                }
                return@BoxWithConstraints
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                carousel(Modifier.weight(1f).fillMaxWidth())

                DesignName(designs[pagerState.currentPage].nameRes)
                DesignDots(designs.size, pagerState.currentPage)
                applyButton()
            }
        }
    }
}

@Composable
private fun DesignName(nameRes: Int) {
    Text(
        text = stringResource(nameRes),
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun DesignDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 10.dp),
    ) {
        repeat(count) { i ->
            val on = i == current
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

/** Stand-in length so the capsule shows a plausible readout with no song loaded. */
private const val PREVIEW_FALLBACK_DURATION = 179_000L

@Composable
internal fun LivePreview(design: PlayerDesign, pc: PlayerConnection?) {
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
            PlayerDesign.CASSETTE -> CassettePreview(meta, pc, textColor)
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
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SLIM)

    // The capsule style is distinctive enough that the preview has to show it,
    // otherwise picking it in Look & Feel appears to do nothing.
    if (sliderStyle == SliderStyle.DEFAULT) {
        CapsuleSeekBar(
            position = if (dur > 0) pos else (PREVIEW_FALLBACK_PROGRESS * PREVIEW_FALLBACK_DURATION).toLong(),
            duration = if (dur > 0) dur else PREVIEW_FALLBACK_DURATION,
            onSeek = { pc?.player?.seekTo(it) },
            colors = SliderDefaults.colors(
                activeTrackColor = activeColor,
                inactiveTrackColor = inactiveColor,
            ),
            contentColor = textColor,
            enabled = pc != null,
            // The phone frame is a fraction of real screen width, so the full-size
            // bar swallows the track here just as it did in the picker tiles.
            compact = true,
        )
        return
    }

    // Wavy and squiggly are just as distinctive, and the preview used to show a plain bar for
    // both, so picking either in Look & Feel looked like it did nothing.
    if (sliderStyle == SliderStyle.WAVY) {
        val squiggly by rememberPreference(SquigglySliderKey, defaultValue = false)
        val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else PREVIEW_FALLBACK_PROGRESS
        val colors = SliderDefaults.colors(
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor,
            thumbColor = activeColor,
        )
        val onValue: (Float) -> Unit = { f -> if (dur > 0) pc?.player?.seekTo((f * dur).toLong()) }
        if (squiggly) {
            SquigglySlider(
                value = frac,
                valueRange = 0f..1f,
                onValueChange = onValue,
                modifier = Modifier.fillMaxWidth(),
                enabled = pc != null,
                colors = colors,
                // Always waving, even with the music paused: this is a preview of the style.
                isPlaying = true,
            )
        } else {
            WavySlider(
                value = frac,
                valueRange = 0f..1f,
                onValueChange = onValue,
                colors = colors,
                modifier = Modifier.fillMaxWidth(),
                isPlaying = true,
                enabled = pc != null,
            )
        }
        return
    }

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
private fun PreviewTransport(pc: PlayerConnection?, onColor: Color, ringOrder: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val isPlaying by remember(pc) { pc?.isPlaying ?: MutableStateFlow(false) }.collectAsState()
    // Circle while paused, rounded square while playing, like the real play button.
    val roundness by animateDpAsState(
        targetValue = if (isPlaying) 15.dp else 23.dp,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "previewPlayRoundness",
    )
    // Ring puts repeat first and shuffle last; the other designs do the opposite.
    val first = if (ringOrder) R.drawable.repeat else R.drawable.shuffle
    val last = if (ringOrder) R.drawable.shuffle else R.drawable.repeat
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniIcon(first, onColor, 18)
        MiniIcon(R.drawable.skip_previous, onColor, 22) { pc?.seekToPrevious() }
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(roundness)).background(cs.primary).clickable { pc?.togglePlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
        }
        MiniIcon(R.drawable.skip_next, onColor, 22) { pc?.seekToNext() }
        MiniIcon(last, onColor, 18)
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

/** Heart · theme · more beside the title, the theme and more buttons as filled circles. */
@Composable
private fun PreviewTitleActions(pc: PlayerConnection?, textColor: Color) {
    val pillIcon = if (textColor == Color.White) Color.Black else MaterialTheme.colorScheme.surface
    PreviewFavorite(pc, textColor)
    Spacer(Modifier.width(10.dp))
    PreviewPillButton(R.drawable.palette, textColor, pillIcon)
    Spacer(Modifier.width(8.dp))
    PreviewPillButton(R.drawable.more_horiz, textColor, pillIcon)
}

/** "Now Playing" and the source in the middle, the collapse button on the left. */
@Composable
private fun PreviewHeader(meta: MediaMetadata?, textColor: Color, collapse: @Composable () -> Unit = { MiniIcon(R.drawable.expand_more, textColor, 20) }) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.align(Alignment.CenterStart)) { collapse() }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 30.dp)) {
            Text(stringResource(R.string.now_playing), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
            meta?.album?.title?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 9.sp, color = textColor.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private val PreviewBottomItems = listOf(
    R.drawable.queue_music to R.string.queue,
    R.drawable.cast to R.string.cast,
    R.drawable.bedtime to R.string.sleep_timer,
    R.drawable.lyrics to R.string.lyrics,
)

/** The player's bottom row: Queue · Cast · Sleep timer · Lyrics, each label under its icon. */
@Composable
private fun PreviewQueuePeek(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewBottomItems.forEach { (icon, label) -> PeekItem(icon, stringResource(label), color, Modifier.weight(1f)) }
    }
}

@Composable
private fun PeekItem(res: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painter = painterResource(res), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 8.sp, lineHeight = 9.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        PreviewHeader(meta, textColor)
        Spacer(Modifier.weight(0.4f))
        PreviewArt(meta?.thumbnailUrl, RoundedCornerShape(20.dp), Modifier.fillMaxWidth(0.82f).aspectRatio(1f))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PreviewTitle(meta, textColor) }
            PreviewTitleActions(pc, textColor)
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

/** Ring's lyric lines above the bottom row: the song's own lyrics, or nothing when it has none. */
@Composable
private fun PreviewRingLyrics(pc: PlayerConnection?, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    val lyrics by remember(pc) { pc?.currentLyrics ?: MutableStateFlow(null) }.collectAsState(initial = null)
    val (pos, _) = rememberLivePosition(pc)
    val entries = remember(lyrics) {
        val text = lyrics?.lyrics?.trim()
        if (text.isNullOrEmpty() || text == LyricsEntity.LYRICS_NOT_FOUND) emptyList() else LyricsUtils.parseLyrics(text)
    }
    val lines: Triple<String, String, String>? =
        when {
            // No song to read from: sample lines, so the design still shows its lyrics.
            pc == null -> Triple("In the stillness of the night", "I feel the weight, the empty sight", "Whispers in my mind, they call")
            entries.isEmpty() -> null
            else -> {
                val i = LyricsUtils.findCurrentLineIndex(entries, pos)
                if (i < 0) {
                    Triple("", "♪", entries.first().text)
                } else {
                    Triple(entries.getOrNull(i - 1)?.text.orEmpty(), entries[i].text, entries.getOrNull(i + 1)?.text.orEmpty())
                }
            }
        }
    if (lines == null) return
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(lines.first, fontSize = 9.sp, color = textColor.copy(alpha = 0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(lines.second, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(lines.third, fontSize = 9.sp, color = textColor.copy(alpha = 0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RingPreview(meta: MediaMetadata?, pc: PlayerConnection?, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    val (pos, dur) = rememberLivePosition(pc)
    val progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else PREVIEW_FALLBACK_PROGRESS
    val shownDur = if (dur > 0) dur else PREVIEW_FALLBACK_DURATION
    val shownPos = if (dur > 0) pos else (PREVIEW_FALLBACK_PROGRESS * PREVIEW_FALLBACK_DURATION).toLong()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PreviewHeader(meta, textColor)
        Spacer(Modifier.weight(0.4f))
        SeekableAlbumRing(
            thumbnailUrl = meta?.thumbnailUrl,
            progress = progress,
            ringColor = cs.primary,
            trackColor = textColor.copy(alpha = 0.20f),
            onSeek = { f -> if (dur > 0) pc?.player?.seekTo((f * dur).toLong()) },
            modifier = Modifier.fillMaxWidth(0.68f).aspectRatio(1f),
            ringStrokeDp = 4f,
            artPaddingDp = 8f,
            fallbackBrush = previewArtBrush(),
            thumbColor = cs.primary,
            // The times sit in a gap at the top of the ring, as in the real player.
            topLabel = {
                Text(
                    "${makeTimeString(shownPos)} — ${makeTimeString(shownDur)}",
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.85f),
                )
            },
        )
        Spacer(Modifier.weight(0.4f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PreviewTitle(meta, textColor) }
            PreviewTitleActions(pc, textColor)
        }
        Spacer(Modifier.height(12.dp))
        PreviewTransport(pc, textColor, ringOrder = true)
        Spacer(Modifier.weight(0.3f))
        PreviewRingLyrics(pc, textColor)
        PreviewQueuePeek(textColor)
    }
}

/* ---------- RECORD ---------- */

@Composable
private fun RecordPreview(meta: MediaMetadata?, pc: PlayerConnection?, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    val isPlaying by remember(pc) { pc?.isPlaying ?: MutableStateFlow(false) }.collectAsState()
    val (pos, dur) = rememberLivePosition(pc)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PreviewHeader(meta, textColor)
        Spacer(Modifier.weight(0.3f))
        VinylTurntable(
            thumbnailUrl = meta?.thumbnailUrl,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxWidth(0.84f).aspectRatio(1f),
            fallbackBrush = previewArtBrush(),
            progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0.35f,
        )
        Spacer(Modifier.weight(0.3f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PreviewTitle(meta, textColor) }
            PreviewTitleActions(pc, textColor)
        }
        Spacer(Modifier.height(12.dp))
        PreviewSlider(pc, cs.primary, textColor.copy(alpha = 0.22f), textColor)
        Spacer(Modifier.height(10.dp))
        PreviewTransport(pc, textColor)
        Spacer(Modifier.height(10.dp))
        PreviewQueuePeek(textColor)
    }
}

/* ---------- CASSETTE ---------- */

private val PreviewRetroCream = Color(0xFFF2E7D0)
private val PreviewRetroInk = Color(0xFF3A2F24)
private val PreviewRetroShellTop = Color(0xFF4A3D31)
private val PreviewRetroShellBottom = Color(0xFF262019)
private val PreviewRetroShellEdge = Color(0xFF6B5B49)

/** A small cream key, like the Cassette player's buttons. */
@Composable
private fun PreviewRetroKey(res: Int, tint: Color = PreviewRetroInk, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(width = 28.dp, height = 25.dp)
            .shadow(2.dp, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .background(PreviewRetroCream)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(res), null, tint = tint, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun CassettePreview(meta: MediaMetadata?, pc: PlayerConnection?, textColor: Color) {
    val cs = MaterialTheme.colorScheme
    val isPlaying by remember(pc) { pc?.isPlaying ?: MutableStateFlow(false) }.collectAsState()
    val song by remember(pc) { pc?.currentSong ?: MutableStateFlow(null) }.collectAsState()
    val liked = song?.song?.liked == true
    val (pos, dur) = rememberLivePosition(pc)
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else PREVIEW_FALLBACK_PROGRESS
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PreviewHeader(meta, textColor) { PreviewRetroKey(R.drawable.expand_more) }
        Spacer(Modifier.weight(0.4f))
        CassetteTape(
            isPlaying = isPlaying,
            progress = frac,
            modifier = Modifier.fillMaxWidth(0.92f),
            accent = cs.primary,
            thumbnailUrl = meta?.thumbnailUrl,
        )
        Spacer(Modifier.weight(0.4f))
        // Title on the left, the heart · theme · more keys on the right.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { PreviewTitle(meta, textColor) }
            PreviewRetroKey(
                if (liked) R.drawable.favorite else R.drawable.favorite_border,
                tint = if (liked) cs.error else PreviewRetroInk,
            ) { pc?.toggleLike() }
            Spacer(Modifier.width(5.dp))
            PreviewRetroKey(R.drawable.palette)
            Spacer(Modifier.width(5.dp))
            PreviewRetroKey(R.drawable.more_horiz)
        }
        Spacer(Modifier.height(8.dp))
        // Retro waveform card with the times, full width.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PreviewRetroCream)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val shownDur = if (dur > 0) dur else PREVIEW_FALLBACK_DURATION
                Text(makeTimeString((frac * shownDur).toLong()), fontSize = 7.sp, lineHeight = 8.sp, color = PreviewRetroInk)
                Text(makeTimeString(shownDur), fontSize = 7.sp, lineHeight = 8.sp, color = PreviewRetroInk)
            }
            Spacer(Modifier.height(3.dp))
            Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                val n = 30
                val gap = size.width / n
                val barW = gap * 0.55f
                for (i in 0 until n) {
                    val wave = kotlin.math.abs(kotlin.math.sin(i * 1.7) * 0.5 + kotlin.math.sin(i * 0.53 + 1.3) * 0.5)
                    val barH = size.height * (0.30f + 0.65f * wave.toFloat()).coerceIn(0.15f, 1f)
                    drawRoundRect(
                        color = if ((i + 0.5f) / n <= frac) cs.primary else PreviewRetroInk.copy(alpha = 0.25f),
                        topLeft = Offset(gap * i + (gap - barW) / 2f, (size.height - barH) / 2f),
                        size = Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // Shuffle · previous · play · next · repeat, the middle three as keys.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            MiniIcon(R.drawable.shuffle, textColor, 15)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(width = 40.dp, height = 32.dp).clip(RoundedCornerShape(9.dp)).background(PreviewRetroCream).clickable { pc?.seekToPrevious() }, contentAlignment = Alignment.Center) {
                MiniIcon(R.drawable.skip_previous, PreviewRetroInk, 17)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(width = 50.dp, height = 36.dp).clip(RoundedCornerShape(9.dp)).background(cs.primary).clickable { pc?.togglePlayPause() }, contentAlignment = Alignment.Center) {
                MiniIcon(if (isPlaying) R.drawable.pause else R.drawable.play, Color.White, 19)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(width = 40.dp, height = 32.dp).clip(RoundedCornerShape(9.dp)).background(PreviewRetroCream).clickable { pc?.seekToNext() }, contentAlignment = Alignment.Center) {
                MiniIcon(R.drawable.skip_next, PreviewRetroInk, 17)
            }
            Spacer(Modifier.width(8.dp))
            MiniIcon(R.drawable.repeat, textColor, 15)
        }
        Spacer(Modifier.weight(0.3f))
        // The dark tape-shell strip with Queue · Cast · Sleep timer · Lyrics.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(PreviewRetroShellTop, PreviewRetroShellBottom)))
                .border(1.dp, PreviewRetroShellEdge, RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp),
        ) {
            PreviewBottomItems.forEach { (icon, label) ->
                PeekItem(icon, stringResource(label), PreviewRetroCream, Modifier.weight(1f))
            }
        }
    }
}

/* ---------- FULL ART ---------- */

@Composable
private fun FullArtPreview(meta: MediaMetadata?, pc: PlayerConnection?) {
    Box(Modifier.fillMaxSize()) {
        PreviewArt(meta?.thumbnailUrl, RoundedCornerShape(0.dp), Modifier.fillMaxSize())
        FullArtScrim()
        // "Now Playing" + source centred at the top, the collapse button on the left.
        val fullArtShadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 2f), 6f)
        Box(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp)) {
            MiniIcon(R.drawable.expand_more, Color.White, 20)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 20.dp, end = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.now_playing),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = LocalTextStyle.current.copy(shadow = fullArtShadow),
            )
            meta?.album?.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = LocalTextStyle.current.copy(shadow = fullArtShadow),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { PreviewTitle(meta, Color.White, shadow = true) }
                PreviewTitleActions(pc, Color.White)
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
