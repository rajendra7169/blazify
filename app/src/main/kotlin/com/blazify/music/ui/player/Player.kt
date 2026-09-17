/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.player

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.blazify.music.ui.component.PlayerBottomButton
import com.blazify.music.ui.component.CastButton
import com.blazify.music.ui.component.BlazeLoader
import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import androidx.palette.graphics.Palette
import com.blazify.music.LocalNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.blazify.music.LocalDatabase
import com.blazify.music.LocalDownloadUtil
import com.blazify.music.LocalListenTogetherManager
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.CropAlbumArtKey
import com.blazify.music.constants.DarkModeKey
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.unit.Dp
import com.blazify.music.constants.HidePlayerThumbnailKey
import com.blazify.music.constants.PlayerDesignKey
import com.blazify.music.constants.HideStatusBarOnFullscreenKey
import com.blazify.music.constants.KeepScreenOn
import com.blazify.music.constants.PlayerBackgroundStyle
import com.blazify.music.constants.PlayerBackgroundStyleKey
import com.blazify.music.constants.PlayerButtonsStyle
import com.blazify.music.constants.PlayerButtonsStyleKey
import com.blazify.music.constants.PlayerHorizontalPadding
import com.blazify.music.constants.QueuePeekHeight
import com.blazify.music.constants.SleepTimerDefaultKey
import com.blazify.music.constants.SleepTimerFadeOutKey
import com.blazify.music.constants.SleepTimerStopAfterCurrentSongKey
import com.blazify.music.constants.SliderStyle
import com.blazify.music.constants.SliderStyleKey
import com.blazify.music.constants.SquigglySliderKey
import com.blazify.music.constants.ThumbnailCornerRadius
import com.blazify.music.constants.UseNewPlayerDesignKey
import com.blazify.music.db.entities.LyricsEntity
import com.blazify.music.lyrics.LyricsUtils
import com.blazify.music.lyrics.lyricsTextLooksSynced
import com.blazify.music.extensions.metadata
import com.blazify.music.extensions.togglePlayPause
import com.blazify.music.extensions.toggleRepeatMode
import com.blazify.music.listentogether.RoomRole
import com.blazify.music.models.MediaMetadata
import com.blazify.music.ui.component.BottomSheet
import com.blazify.music.ui.component.BlazeSleepTimerDialog
import com.blazify.music.ui.component.BottomSheetState
import com.blazify.music.ui.component.CapsuleSeekBar
import com.blazify.music.ui.component.LocalBottomSheetPageState
import com.blazify.music.ui.component.LocalMenuState
import com.blazify.music.ui.component.Lyrics
import com.blazify.music.ui.component.LiveBadge
import com.blazify.music.ui.component.PlayerSliderTrack
import com.blazify.music.ui.component.ResizableIconButton
import com.blazify.music.ui.component.SquigglySlider
import com.blazify.music.ui.component.WavySlider
import com.blazify.music.ui.component.rememberBottomSheetState
import com.blazify.music.ui.menu.PlayerMenu
import com.blazify.music.ui.screens.settings.DarkMode
import com.blazify.music.ui.theme.BlazeThemeColor
import com.blazify.music.ui.theme.PlayerColorExtractor
import com.blazify.music.ui.theme.PlayerSliderColors
import com.blazify.music.ui.utils.ShowMediaInfo
import com.blazify.music.ui.utils.ShowOffsetDialog
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.makeTimeString
import com.blazify.music.utils.rememberEnumPreference
import com.blazify.music.utils.rememberPreference
import com.blazify.music.utils.safeDataStoreEdit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.blazify.music.ui.component.Icon as MIcon
import com.blazify.music.constants.SleepTimerDefaultKey
import com.blazify.music.constants.SleepTimerFadeOutKey
import com.blazify.music.constants.SleepTimerStopAfterCurrentSongKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    // Back from the design gallery lands on the page it was opened from; bring the player back up.
    // Opening it, the gallery appears at once under the player (no page slide), and once it has
    // been drawn the player fades away over it and is put down without the slide: sliding the
    // player over the gallery redrew both on every frame and stuttered, and sliding it before the
    // gallery was there showed the page underneath.
    val playerFade = remember { Animatable(1f) }
    // The sheet state is rebuilt when the mini player's resting place changes (the gallery hides
    // the navigation bar), so always act on the newest one.
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            if (entry.savedStateHandle.remove<Boolean>(ReopenPlayerKey) == true) {
                playerFade.snapTo(1f)
                latestState.expandSoft()
            }
            if (entry.savedStateHandle.remove<Boolean>(CollapsePlayerKey) == true) {
                try {
                    // Two frames: the gallery is composed in the first and drawn in the second.
                    withFrameNanos { }
                    withFrameNanos { }
                    playerFade.animateTo(0f, tween(durationMillis = 180, easing = LinearEasing))
                    latestState.collapse(snap())
                    withFrameNanos { }
                } finally {
                    playerFade.snapTo(1f)
                }
            }
        }
    }

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = false,
        )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val (playerDesignId) = rememberPreference(PlayerDesignKey, PlayerDesign.CLASSIC.id)
    val playerDesign = remember(playerDesignId) { PlayerDesign.fromId(playerDesignId) }
    // Height reserved at the bottom of the RING layout for its overlay: the button row, plus the
    // synced lines when the song has any. A song without lyrics gets only the row, and the rest of
    // the layout settles into the room instead of leaving a blank or a "no lyrics" note.
    val ringNavBottomInset = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val ringLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val ringHasLyrics =
        remember(ringLyrics) {
            val text = ringLyrics?.lyrics?.trim()
            !text.isNullOrEmpty() && text != LyricsEntity.LYRICS_NOT_FOUND
        }
    val ringBottomOverlayHeight by animateDpAsState(
        targetValue = (if (ringHasLyrics) 208.dp else 96.dp) + ringNavBottomInset,
        label = "ringBottomOverlayHeight",
    )

    var showInlineLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var isFullScreen by rememberSaveable {
        mutableStateOf(false)
    }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT,
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT,
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    val shouldUseDarkButtonColors =
        remember(playerBackground, useDarkTheme) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> true
                PlayerBackgroundStyle.DEFAULT -> useDarkTheme
            }
        }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn, isFullScreen, hideStatusBarOnFullscreen) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                    insetsController.isAppearanceLightStatusBars = false
                }

                PlayerBackgroundStyle.DEFAULT -> {
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                }
            }

            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }

            if (keepScreenOn && state.isExpanded) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Back steps out one layer at a time: full screen first, then lyrics, and only then does the
    // player collapse. While lyrics or full screen are open, the sheet's own collapse handler and
    // the one below are switched off, so this doesn't depend on which handler registered last
    // (that order differed between phones). The queue sheet adds its handler when the queue
    // opens, so Back with the queue open keeps closing just the queue.
    val playerHasInnerLayer = isFullScreen || showInlineLyrics
    BackHandler(enabled = state.isExpanded && !playerHasInnerLayer) {
        state.collapseSoft()
    }

    if (state.isExpanded && playerHasInnerLayer) {
        BackHandler {
            if (isFullScreen) {
                isFullScreen = false
            } else {
                showInlineLyrics = false
            }
        }
    }

    val onBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurface
        }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    // A broadcast is wherever it is right now: it gets a LIVE mark instead of a time it is at.
    val liveBroadcasts by playerConnection.liveBroadcasts.collectAsState()
    val isLive = mediaMetadata?.id in liveBroadcasts
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SLIM)
    val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)

    // Listen Together state (reactive)
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST
    val playerSeeker = rememberPlayerSeeker(playerConnection)
    val isRtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if focus request fails
            }
        }
    }

    // Use Cast state when casting, otherwise local player
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    // Use State objects for position/duration to pass to MiniPlayer without causing recomposition
    // These states persist across playback state changes to ensure continuous progress updates.
    // Seed from the player's current values so re-entering composition on resume shows the real
    // time immediately instead of flashing 0:00 until the first poll fires. runCatching guards the
    // player-not-ready race; the poll loop corrects duration if it isn't known yet.
    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf(
            (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L),
        )
    }

    // Convenience accessors for local use
    var position by positionState
    var duration by durationState

    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) {
                castPosition
            } else {
                position
            }
        }
    }

    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    // Track when we last manually set position to avoid Cast overwriting it
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(currentMetadata.thumbnailUrl)
                            .size(100, 100)
                            .allowHardware(false)
                            .memoryCacheKey("gradient_${currentMetadata.id}")
                            .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette =
                                withContext(Dispatchers.Default) {
                                    Palette
                                        .from(bitmap)
                                        .maximumColorCount(8)
                                        .resizeBitmapArea(100 * 100)
                                        .generate()
                                }
                            val extractedColors =
                                PlayerColorExtractor.extractGradientColors(
                                    palette = palette,
                                    fallbackColor = fallbackColor,
                                )
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val TextBackgroundColor by animateColorAsState(
        targetValue =
            when (playerBackground) {
                PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
                PlayerBackgroundStyle.BLUR -> Color.White
                PlayerBackgroundStyle.GRADIENT -> Color.White
            },
        label = "TextBackgroundColor",
    )

    val icBackgroundColor by animateColorAsState(
        targetValue =
            when (playerBackground) {
                PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
                PlayerBackgroundStyle.BLUR -> Color.Black
                PlayerBackgroundStyle.GRADIENT -> Color.Black
            },
        label = "icBackgroundColor",
    )

    val (textButtonColor, iconButtonColor) =
        when {
            playerBackground == PlayerBackgroundStyle.BLUR ||
                playerBackground == PlayerBackgroundStyle.GRADIENT -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(Color.White, Color.Black)
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }

            else -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        if (useDarkTheme) {
                            Pair(Color.White, Color.Black)
                        } else {
                            Pair(Color.Black, Color.White)
                        }
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }
        }

    // The play/pause button always carries the colour taken from the artwork, as Ring's does,
    // whatever colour the other player buttons use: it is the one control people look for.
    val playButtonColor = MaterialTheme.colorScheme.primary
    val playIconColor = MaterialTheme.colorScheme.onPrimary

    // Separate colors for Previous/Next buttons in PRIMARY/TERTIARY modes
    val (sideButtonContainerColor, sideButtonContentColor) =
        when {
            playerBackground == PlayerBackgroundStyle.BLUR ||
                playerBackground == PlayerBackgroundStyle.GRADIENT -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(
                            Color.White.copy(alpha = 0.2f),
                            Color.White,
                        )
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            else -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }

    val download by LocalDownloadUtil.current
        .getDownload(mediaMetadata?.id ?: "")
        .collectAsStateWithLifecycle(initialValue = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer?.triggerTime,
            playerConnection.service.sleepTimer?.pauseWhenSongEnd,
            playerConnection.service.sleepTimer?.songsLeft,
        ) {
            playerConnection.service.sleepTimer?.isActive ?: false
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    val scope = rememberCoroutineScope()
    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember {
        mutableFloatStateOf(sleepTimerDefault)
    }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)


    if (showSleepTimerDialog) {
        BlazeSleepTimerDialog(
            sleepTimerEnabled = sleepTimerEnabled,
            sleepTimerTimeLeft = sleepTimerTimeLeft,
            pauseWhenSongEnd = playerConnection.service.sleepTimer?.pauseWhenSongEnd == true,
            sleepTimerSongsLeft = playerConnection.service.sleepTimer?.songsLeft ?: 0,
            initialMinutes = sleepTimerDefault,
            onDismiss = { showSleepTimerDialog = false },
            onStart = { minutes ->
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer?.start(
                    minute = minutes,
                    stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                    fadeOut = sleepTimerFadeOut,
                )
            },
            onStartEndOfSong = {
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer?.start(minute = -1)
            },
            onStartAfterSongs = { count ->
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer?.startAfterSongs(count)
            },
            onClear = { playerConnection.service.sleepTimer?.clear() },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Position update - only for local playback
    // When casting, we use castPosition directly to avoid sync issues
    // Use isPlaying instead of playbackState to ensure continuous updates during playback
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100) // Update more frequently for smoother progress bar
                if (sliderPosition == null) { // Only update if user isn't dragging
                    position = playerConnection.player.currentPosition
                    // Don't clobber a valid (metadata-derived) duration with 0/UNSET mid-resolve.
                    playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }
        }
    }

    // Also update position when playback state changes (e.g., song change, seek)
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            // Prefer the song's known duration (from metadata, available instantly from the restored
            // queue) so the slider range is right even when restored paused / before the stream
            // resolves; fall back to the player's duration once it is known.
            duration = (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: playerConnection.player.duration
        }
    }

    // Auto-switch from repeat one to repeat all when song ends naturally
    var previousMediaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        val currentId = mediaMetadata?.id

        // Only switch from REPEAT_ONE to REPEAT_ALL when playback naturally ended
        // (i.e., the player transitioned to ENDED state and then moved to next track).
        // Do NOT switch on manual skips.
        if (currentId != null &&
            currentId != previousMediaId &&
            previousMediaId != null &&
            playbackState == Player.STATE_ENDED &&
            repeatMode == Player.REPEAT_MODE_ONE &&
            !isListenTogetherGuest) {
            playerConnection.player.setRepeatMode(Player.REPEAT_MODE_ALL)
        }

        previousMediaId = currentId
    }

    // When casting, use Cast position/duration directly
    // But wait a bit after manual seeks to let Cast catch up
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                // Only update from Cast if we haven't manually seeked recently
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = dismissedBound,
            expandedBound = state.expandedBound,
            collapsedBound = dismissedBound + 1.dp,
            initialAnchor = 1,
        )

    val bottomSheetBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                MaterialTheme.colorScheme.surfaceContainer
            }

            else -> {
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            }
        }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    BottomSheet(
        state = state,
        modifier = modifier.graphicsLayer { alpha = playerFade.value },
        collapseOnBack = !playerHasInnerLayer,
        background = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(bottomSheetBackgroundColor),
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = mediaMetadata?.thumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground",
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(context)
                                                .data(thumbnailUrl)
                                                .size(100, 100)
                                                .allowHardware(false)
                                                .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .blur(if (useDarkTheme) 150.dp else 100.dp),
                                    )
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                    )
                                }
                            }
                        }
                    }

                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground",
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops =
                                    if (colors.size >= 3) {
                                        arrayOf(
                                            0.0f to colors[0],
                                            0.5f to colors[1],
                                            1.0f to colors[2],
                                        )
                                    } else {
                                        arrayOf(
                                            0.0f to colors[0],
                                            0.6f to colors[0].copy(alpha = 0.7f),
                                            1.0f to Color.Black,
                                        )
                                    }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                )
                            }
                        }
                    }

                    else -> {
                        PlayerBackgroundStyle.DEFAULT
                    }
                }
            }
        },
        onDismiss =
            if (!isListenTogetherGuest) {
                {
                    playerConnection.service.clearAutomix()
                    playerConnection.player.stop()
                    playerConnection.player.clearMediaItems()
                }
            } else {
                null
            },
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                onClick = { state.expandSoft() },
            )
        },
    ) {
        // Shared music progress bar honouring the Settings slider style (Default / Wavy / Slim).
        // Used by both the classic controls and the RING design so they stay consistent.
        val playerSeekBar: @Composable (Modifier, SliderColors) -> Unit = { seekModifier, seekColors ->
            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
                    CapsuleSeekBar(
                        position = sliderPosition ?: effectivePosition,
                        duration = if (duration == C.TIME_UNSET) 0L else duration,
                        onSeek = {
                            if (!isListenTogetherGuest) {
                                if (isCasting) {
                                    castHandler?.seekTo(it)
                                    lastManualSeekTime = System.currentTimeMillis()
                                } else {
                                    playerConnection.player.seekTo(it)
                                }
                                position = it
                                sliderPosition = null
                            }
                        },
                        colors = seekColors,
                        contentColor = onBackgroundColor,
                        enabled = !isListenTogetherGuest,
                        modifier = seekModifier,
                    )
                }

                SliderStyle.WAVY -> {
                    if (squigglySlider) {
                        SquigglySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    if (isCasting) {
                                        castHandler?.seekTo(it)
                                        lastManualSeekTime = System.currentTimeMillis()
                                    } else {
                                        playerConnection.player.seekTo(it)
                                    }
                                    position = it
                                }
                                sliderPosition = null
                            },
                            modifier = seekModifier,
                            colors = seekColors,
                            isPlaying = effectiveIsPlaying,
                        )
                    } else {
                        WavySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    if (isCasting) {
                                        castHandler?.seekTo(it)
                                        lastManualSeekTime = System.currentTimeMillis()
                                    } else {
                                        playerConnection.player.seekTo(it)
                                    }
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = seekColors,
                            modifier = seekModifier,
                            isPlaying = effectiveIsPlaying,
                        )
                    }
                }

                SliderStyle.SLIM -> {
                    Slider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                sliderPosition = it.toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let {
                                    if (isCasting) {
                                        castHandler?.seekTo(it)
                                        lastManualSeekTime = System.currentTimeMillis()
                                    } else {
                                        playerConnection.player.seekTo(it)
                                    }
                                    position = it
                                }
                                sliderPosition = null
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = seekColors,
                                // Apple-Music-style slim bar
                                trackHeight = 8.dp,
                            )
                        },
                        modifier = seekModifier,
                    )
                }
            }
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                AnimatedContent(
                    targetState = showInlineLyrics,
                    label = "ThumbnailAnimation",
                ) { showLyrics ->
                    if (showLyrics) {
                        Row {
                            if (hidePlayerThumbnail) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.small_icon),
                                        contentDescription = null,
                                        modifier =
                                            Modifier
                                                .size(32.dp)
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = mediaMetadata.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                    modifier =
                                        Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    AnimatedContent(
                        targetState = mediaMetadata.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor,
                            modifier =
                                Modifier
                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                    .combinedClickable(
                                        enabled = true,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            val albumId = mediaMetadata.album?.id
                                                ?: currentSong?.album?.id
                                                ?: currentSong?.song?.albumId
                                            if (albumId != null) {
                                                navController.navigate("album/$albumId")
                                                state.collapseSoft()
                                            }
                                        },
                                        onLongClick = {
                                            val clip = ClipData.newPlainText(copiedTitleStr, title)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast
                                                .makeText(context, copiedTitleStr, Toast.LENGTH_SHORT)
                                                .show()
                                        },
                                    ),
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (mediaMetadata.explicit) MIcon.Explicit()

                        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                            val annotatedString =
                                buildAnnotatedString {
                                    mediaMetadata.artists.forEachIndexed { index, artist ->
                                        val tag = "artist_${artist.id.orEmpty()}"
                                        pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                        withStyle(SpanStyle(color = TextBackgroundColor.copy(alpha = 0.7f), fontSize = 16.sp)) {
                                            append(artist.name)
                                        }
                                        pop()
                                        if (index != mediaMetadata.artists.lastIndex) append(", ")
                                    }
                                }

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                        .padding(end = 12.dp),
                            ) {
                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor.copy(alpha = 0.7f)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { layoutResult = it },
                                    modifier =
                                        Modifier
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val tapPosition = event.changes.firstOrNull()?.position
                                                        if (tapPosition != null) {
                                                            clickOffset = tapPosition
                                                        }
                                                    }
                                                }
                                            }.combinedClickable(
                                                enabled = true,
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = {
                                                    val tapPosition = clickOffset
                                                    val layout = layoutResult
                                                    if (tapPosition != null && layout != null) {
                                                        val offset = layout.getOffsetForPosition(tapPosition)
                                                        annotatedString
                                                            .getStringAnnotations(offset, offset)
                                                            .firstOrNull()
                                                            ?.let { ann ->
                                                                val artistId = ann.item
                                                                if (artistId.isNotBlank()) {
                                                                    navController.navigate("artist/$artistId")
                                                                    state.collapseSoft()
                                                                }
                                                            }
                                                    }
                                                },
                                                onLongClick = {
                                                    val clip =
                                                        ClipData.newPlainText(
                                                            copiedArtistStr,
                                                            annotatedString,
                                                        )
                                                    clipboardManager.setPrimaryClip(clip)
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            copiedArtistStr,
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                },
                                            ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (useNewPlayerDesign) {
                    val shareShape =
                        RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 3.dp,
                            bottomEnd = 3.dp,
                        )

                    val favShape =
                        RoundedCornerShape(
                            topStart = 3.dp,
                            bottomStart = 3.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp,
                        )

                    val middleShape = RoundedCornerShape(3.dp)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedContent(targetState = showInlineLyrics, label = "ShareButton") { showLyrics ->
                            if (showLyrics) {
                                FilledIconButton(
                                    onClick = { isFullScreen = !isFullScreen },
                                    shape = shareShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.fullscreen),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        val intent =
                                            Intent().apply {
                                                action = Intent.ACTION_SEND
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                                                )
                                            }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    shape = shareShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.share),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }

                        AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                            if (showLyrics) {
                                val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                FilledIconButton(
                                    onClick = {
                                        menuState.show {
                                            com.blazify.music.ui.menu.LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
                                                mediaMetadataProvider = { mediaMetadata },
                                                onDismiss = menuState::dismiss,
                                                onShowOffsetDialog = {
                                                    bottomSheetPageState.show {
                                                        ShowOffsetDialog(
                                                            songProvider = { currentSong?.song },
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    },
                                    shape = favShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_horiz),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            } else {
                                // For episodes, show saved state (inLibrary); for songs, show liked state
                                val isEpisode = currentSong?.song?.isEpisode == true
                                val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                FilledIconButton(
                                    onClick = playerConnection::toggleLike,
                                    // A broadcast is not a song to keep: there is nothing of it
                                    // to come back to later.
                                    enabled = !isLive,
                                    shape = favShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                            disabledContainerColor = textButtonColor.copy(alpha = 0.3f),
                                            disabledContentColor = iconButtonColor.copy(alpha = 0.4f),
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isFavorite) {
                                                    R.drawable.favorite
                                                } else {
                                                    R.drawable.favorite_border
                                                },
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(targetState = showInlineLyrics, label = "ShareButton") { showLyrics ->
                        if (showLyrics) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(textButtonColor)
                                        .clickable { isFullScreen = !isFullScreen },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.fullscreen),
                                    contentDescription = null,
                                    tint = iconButtonColor,
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp),
                                )
                            }
                        } else {
                            // Blazify layout: favorite heart next to the title (share lives in the more menu)
                            val isEpisode = currentSong?.song?.isEpisode == true
                            val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable(enabled = !isLive) { playerConnection.toggleLike() },
                            ) {
                                Icon(
                                    painter =
                                        painterResource(
                                            if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                                        ),
                                    contentDescription = null,
                                    tint =
                                        when {
                                            isLive -> TextBackgroundColor.copy(alpha = 0.3f)
                                            isFavorite -> MaterialTheme.colorScheme.error
                                            else -> TextBackgroundColor
                                        },
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(26.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))
                    PlayerThemeButton(
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        state = state,
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                        if (showLyrics) {
                            val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(textButtonColor)
                                        .clickable {
                                            menuState.show {
                                                com.blazify.music.ui.menu.LyricsMenu(
                                                    lyricsProvider = { currentLyrics },
                                                    songProvider = { currentSong?.song },
                                                    mediaMetadataProvider = { mediaMetadata },
                                                    onDismiss = menuState::dismiss,
                                                    onShowOffsetDialog = {
                                                        bottomSheetPageState.show {
                                                            ShowOffsetDialog(
                                                                songProvider = { currentSong?.song },
                                                            )
                                                        }
                                                    },
                                                )
                                            }
                                        },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_horiz),
                                    contentDescription = null,
                                    tint = iconButtonColor,
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp),
                                )
                            }
                        } else {
                            PlayerMoreMenuButton(
                                mediaMetadata = mediaMetadata,
                                state = state,
                                textButtonColor = textButtonColor,
                                iconButtonColor = iconButtonColor,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            playerSeekBar(
                Modifier.padding(horizontal = PlayerHorizontalPadding),
                // Dynamic album-colour progress bar for every design; style stays from Settings.
                PlayerSliderColors.getSliderColors(
                    activeColor = textButtonColor,
                    playerBackground = playerBackground,
                    useDarkTheme = useDarkTheme,
                    activeOverride = MaterialTheme.colorScheme.primary,
                ),
            )

            // The capsule style already carries "elapsed / total" in its thumb, so
            // the separate end labels would just repeat it.
            if (sliderStyle != SliderStyle.DEFAULT) {
                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PlayerHorizontalPadding + 4.dp),
                ) {
                    if (isLive) {
                        LiveBadge()
                    } else {
                        Text(
                            text = makeTimeString(sliderPosition ?: effectivePosition),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextBackgroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextBackgroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Column {
                    if (useNewPlayerDesign) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PlayerHorizontalPadding),
                        ) {
                            val backInteractionSource = remember { MutableInteractionSource() }
                            val nextInteractionSource = remember { MutableInteractionSource() }
                            val playPauseInteractionSource = remember { MutableInteractionSource() }

                            val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                            val isBackPressed by backInteractionSource.collectIsPressedAsState()
                            val isNextPressed by nextInteractionSource.collectIsPressedAsState()

                            val playPauseWeight by animateFloatAsState(
                                targetValue =
                                    if (isPlayPausePressed) {
                                        1.9f
                                    } else if (isBackPressed || isNextPressed) {
                                        1.1f
                                    } else {
                                        1.3f
                                    },
                                animationSpec =
                                    spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 500f,
                                    ),
                                label = "playPauseWeight",
                            )

                            val backButtonWeight by animateFloatAsState(
                                targetValue =
                                    if (isBackPressed) {
                                        0.65f
                                    } else if (isPlayPausePressed) {
                                        0.35f
                                    } else {
                                        0.45f
                                    },
                                animationSpec =
                                    spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 500f,
                                    ),
                                label = "backButtonWeight",
                            )

                            val nextButtonWeight by animateFloatAsState(
                                targetValue =
                                    if (isNextPressed) {
                                        0.65f
                                    } else if (isPlayPausePressed) {
                                        0.35f
                                    } else {
                                        0.45f
                                    },
                                animationSpec =
                                    spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 500f,
                                    ),
                                label = "nextButtonWeight",
                            )

                            FilledIconButton(
                                onClick = playerConnection::seekToPrevious,
                                enabled = canSkipPrevious && !isListenTogetherGuest,
                                shape = RoundedCornerShape(50),
                                interactionSource = backInteractionSource,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = sideButtonContainerColor,
                                        contentColor = sideButtonContentColor,
                                    ),
                                modifier =
                                    Modifier
                                        .height(68.dp)
                                        .weight(backButtonWeight),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FilledIconButton(
                                onClick = {
                                    if (isListenTogetherGuest) {
                                        playerConnection.toggleMute()
                                        return@FilledIconButton
                                    }
                                    if (isCasting) {
                                        if (castIsPlaying) {
                                            castHandler?.pause()
                                        } else {
                                            castHandler?.play()
                                        }
                                    } else if (playbackState == STATE_ENDED) {
                                        playerConnection.player.seekTo(0, 0)
                                        playerConnection.player.playWhenReady = true
                                    } else {
                                        playerConnection.togglePlayPause()
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                interactionSource = playPauseInteractionSource,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = playButtonColor,
                                        contentColor = playIconColor,
                                    ),
                                modifier =
                                    Modifier
                                        .height(68.dp)
                                        .weight(playPauseWeight)
                                        .focusRequester(focusRequester),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isListenTogetherGuest) {
                                                    if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                                } else {
                                                    if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                                                },
                                            ),
                                        contentDescription =
                                            if (isListenTogetherGuest) {
                                                if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                            } else {
                                                if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                                            },
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text =
                                            if (isListenTogetherGuest) {
                                                if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                            } else {
                                                if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                                            },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FilledIconButton(
                                onClick = playerConnection::seekToNext,
                                enabled = canSkipNext && !isListenTogetherGuest,
                                shape = RoundedCornerShape(50),
                                interactionSource = nextInteractionSource,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = sideButtonContainerColor,
                                        contentColor = sideButtonContentColor,
                                    ),
                                modifier =
                                    Modifier
                                        .height(68.dp)
                                        .weight(nextButtonWeight),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PlayerHorizontalPadding),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                // Blazify layout: shuffle on the far left, accent-tinted when on
                                val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
                                ResizableIconButton(
                                    icon = R.drawable.shuffle,
                                    color = if (shuffleModeEnabled) BlazeThemeColor else TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .align(Alignment.Center)
                                            .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    enabled = !isListenTogetherGuest,
                                    onClick = {
                                        playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                                    },
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.skip_previous,
                                    enabled = canSkipPrevious && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .align(Alignment.Center)
                                            .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToPrevious,
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(playPauseRoundness))
                                        .background(playButtonColor)
                                        .clickable {
                                            if (isListenTogetherGuest) {
                                                playerConnection.toggleMute()
                                                return@clickable
                                            }
                                            if (isCasting) {
                                                if (castIsPlaying) {
                                                    castHandler?.pause()
                                                } else {
                                                    castHandler?.play()
                                                }
                                            } else if (playbackState == STATE_ENDED) {
                                                playerConnection.player.seekTo(0, 0)
                                                playerConnection.player.playWhenReady = true
                                            } else {
                                                playerConnection.player.togglePlayPause()
                                            }
                                        }
                                        .focusRequester(focusRequester),
                            ) {
                                Image(
                                    painter =
                                        painterResource(
                                            if (isListenTogetherGuest) {
                                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                            } else if (playbackState ==
                                                STATE_ENDED
                                            ) {
                                                R.drawable.replay
                                            } else if (effectiveIsPlaying) {
                                                R.drawable.pause
                                            } else {
                                                R.drawable.play
                                            },
                                        ),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(playIconColor),
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp),
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.skip_next,
                                    enabled = canSkipNext && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .align(Alignment.Center)
                                            .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToNext,
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                // Blazify layout: repeat on the far right, accent-tinted when active
                                ResizableIconButton(
                                    icon =
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> throw IllegalStateException()
                                        },
                                    color = if (repeatMode != Player.REPEAT_MODE_OFF) BlazeThemeColor else TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .align(Alignment.Center)
                                            .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    enabled = !isListenTogetherGuest,
                                    onClick = {
                                        playerConnection.player.toggleRepeatMode()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // Calculate vertical padding like OuterTune
                val density = LocalDensity.current
                val verticalPadding =
                    max(
                        WindowInsets.systemBars.getTop(density),
                        WindowInsets.systemBars.getBottom(density),
                    )
                val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = verticalPaddingDp)

                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets),
                            ).padding(bottom = 24.dp)
                            .fillMaxSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .weight(1f)
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                    ) {
                        // Remember lambdas to prevent unnecessary recomposition
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition },
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.animateContentSize(),
                                    isPlayerExpanded = isExpandedProvider,
                                    isLandscape = true,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(if (showInlineLyrics) 0.65f else 1f, false)
                                .animateContentSize()
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                val bottomPadding by animateDpAsState(
                    targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound,
                    label = "bottomPadding",
                )
                // Cassette's title, progress and transport. Used on its front page and, so that the
                // controls do not turn into another design's, on its lyrics page too.
                val cassetteLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                val cassetteControls: @Composable () -> Unit = {
                    // Title / artist on the left, with the like, theme and menu keys beside
                    // them — the same arrangement as the other designs, in this one's style.
                    val cassetteIsEpisode = currentSong?.song?.isEpisode == true
                    val cassetteIsFavorite =
                        if (cassetteIsEpisode) {
                            currentSong?.song?.inLibrary != null
                        } else {
                            currentSong?.song?.liked == true
                        }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                    ) {
                        if (showInlineLyrics) {
                            // The tape is out of sight on the lyrics page, so the artwork sits in
                            // front of the title, small, as on the other designs' lyrics pages.
                            AsyncImage(
                                model = mediaMetadata?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(6.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RetroCream),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = mediaMetadata?.title.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = TextBackgroundColor,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                            )
                            Text(
                                text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = TextBackgroundColor.copy(alpha = 0.7f),
                            )
                        }
                        mediaMetadata?.let {
                            CassetteTitleKeys(
                                mediaMetadata = it,
                                state = state,
                                isFavorite = cassetteIsFavorite,
                                isLive = isLive,
                                onToggleLike = { playerConnection.toggleLike() },
                                lyricsPage = showInlineLyrics,
                                onToggleFullScreen = { isFullScreen = !isFullScreen },
                                onLyricsMenu = {
                                    menuState.show {
                                        com.blazify.music.ui.menu.LyricsMenu(
                                            lyricsProvider = { cassetteLyrics },
                                            songProvider = { currentSong?.song },
                                            mediaMetadataProvider = { it },
                                            onDismiss = menuState::dismiss,
                                            onShowOffsetDialog = {
                                                bottomSheetPageState.show {
                                                    ShowOffsetDialog(songProvider = { currentSong?.song })
                                                }
                                            },
                                        )
                                    }
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Retro waveform progress card (times + seekable bars).
                    RetroWaveformCard(
                        position = sliderPosition ?: effectivePosition,
                        duration = duration,
                        isLive = isLive,
                        accent = MaterialTheme.colorScheme.primary,
                        onSeek = { pos ->
                            playerConnection.player.seekTo(pos)
                            position = pos
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                    )

                    // Chunky 3D retro transport. Full-screen lyrics keep only the lines and what
                    // is above them, down to the progress card, as on the other designs.
                    val cassetteShuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
                    AnimatedVisibility(
                        visible = !(showInlineLyrics && isFullScreen),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(CassetteSectionGap))
                            RetroTransportRow(
                        isPlaying = effectiveIsPlaying,
                        shuffleOn = cassetteShuffle,
                        repeatMode = repeatMode,
                        accent = MaterialTheme.colorScheme.primary,
                        flatColor = TextBackgroundColor,
                        onToggleShuffle = {
                            playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                        },
                        onPrevious = { playerConnection.seekToPrevious() },
                        onTogglePlay = { playerConnection.togglePlayPause() },
                        onNext = { playerConnection.seekToNext() },
                        onToggleRepeat = { playerConnection.player.toggleRepeatMode() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                    )
                        }
                    }
                }
                if (playerDesign == PlayerDesign.FULL_ART && !showInlineLyrics) {
                    // FULL_ART: album art fills the WHOLE screen (even behind the bottom
                    // queue peek) for a seamless look; controls float over the bottom scrim.
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .animateContentSize()
                                .doubleTapToSeek(playerSeeker, isRtlLayout, enabled = !isListenTogetherGuest),
                    ) {
                        FullArtBackground(
                            thumbnailUrl = mediaMetadata?.thumbnailUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                        SeekMessage(playerSeeker, Modifier.align(Alignment.Center))
                        // "Now Playing" + source header centred at the top, over the artwork
                        // (same as the classic ThumbnailHeader, with shadows for readability).
                        val fullArtQueueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
                        val fullArtShadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), 6f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                                .padding(top = 14.dp, start = 48.dp, end = 48.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium.copy(shadow = fullArtShadow),
                                fontWeight = FontWeight.Bold,
                                color = TextBackgroundColor,
                            )
                            val fullArtPlayingFrom = fullArtQueueTitle ?: mediaMetadata?.album?.title
                            if (!fullArtPlayingFrom.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = fullArtPlayingFrom,
                                    style = MaterialTheme.typography.titleMedium.copy(shadow = fullArtShadow),
                                    color = TextBackgroundColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(),
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                    .padding(bottom = bottomPadding),
                        ) {
                            mediaMetadata?.let {
                                controlsContent(it)
                            }
                            Spacer(Modifier.height(30.dp))
                        }
                    }
                } else if (playerDesign == PlayerDesign.RING && !showInlineLyrics) {
                    val ringShuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
                    val ringQueueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
                    val ringIsEpisode = currentSong?.song?.isEpisode == true
                    val ringIsFavorite =
                        if (ringIsEpisode) {
                            currentSong?.song?.inLibrary != null
                        } else {
                            currentSong?.song?.liked == true
                        }
                    RingPlayerLayout(
                        mediaMetadata = mediaMetadata,
                        isLive = isLive,
                        nowPlayingFrom = ringQueueTitle ?: mediaMetadata?.album?.title,
                        isFavorite = ringIsFavorite,
                        position = sliderPosition ?: effectivePosition,
                        duration = duration,
                        isPlaying = effectiveIsPlaying,
                        repeatMode = repeatMode,
                        shuffleOn = ringShuffle,
                        textColor = TextBackgroundColor,
                        buttonBgColor = textButtonColor,
                        buttonFgColor = iconButtonColor,
                        bottomReserve = ringBottomOverlayHeight,
                        onSeek = { pos ->
                            playerConnection.player.seekTo(pos)
                            position = pos
                        },
                        onTogglePlay = { playerConnection.togglePlayPause() },
                        onNext = { playerConnection.seekToNext() },
                        onPrevious = { playerConnection.seekToPrevious() },
                        onToggleRepeat = { playerConnection.player.toggleRepeatMode() },
                        onToggleLike = { playerConnection.toggleLike() },
                        onShowLyrics = { showInlineLyrics = true },
                        onCollapse = { state.collapseSoft() },
                        titleActions = {
                            // Theme and the song menu sit beside the title, as on the other designs.
                            Spacer(Modifier.size(12.dp))
                            PlayerThemeButton(
                                textButtonColor = textButtonColor,
                                iconButtonColor = iconButtonColor,
                                state = state,
                            )
                            Spacer(Modifier.size(12.dp))
                            mediaMetadata?.let {
                                PlayerMoreMenuButton(
                                    mediaMetadata = it,
                                    state = state,
                                    textButtonColor = textButtonColor,
                                    iconButtonColor = iconButtonColor,
                                )
                            }
                        },
                        onToggleShuffle = {
                            playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                        },
                        seeker = if (isListenTogetherGuest) null else playerSeeker,
                        rtl = isRtlLayout,
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
                    )
                } else if (playerDesign == PlayerDesign.RECORD && !showInlineLyrics) {
                    // RECORD: spinning vinyl + tonearm as the artwork; classic controls below.
                    val recordQueueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = bottomPadding)
                                .animateContentSize(),
                    ) {
                        // "Now Playing" + source header, like the classic ThumbnailHeader.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                                    .padding(top = 8.dp, start = 48.dp, end = 48.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextBackgroundColor,
                            )
                            val recordPlayingFrom = recordQueueTitle ?: mediaMetadata?.album?.title
                            if (!recordPlayingFrom.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = recordPlayingFrom,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextBackgroundColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(),
                                )
                            }
                        }

                        // Push the turntable a little lower.
                        Spacer(Modifier.height(14.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        ) {
                            VinylTurntable(
                                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                                isPlaying = effectiveIsPlaying,
                                onTurn = if (isListenTogetherGuest) null else { forward -> playerSeeker.seek(forward) },
                                modifier = Modifier.fillMaxSize().padding(horizontal = PlayerHorizontalPadding),
                                fallbackBrush = Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                ),
                                progress =
                                    if (duration > 0) {
                                        ((sliderPosition ?: effectivePosition).toFloat() / duration).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    },
                            )
                            SeekMessage(playerSeeker)
                        }

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.height(30.dp))
                    }
                } else if (playerDesign == PlayerDesign.CASSETTE && !showInlineLyrics) {
                    // CASSETTE: retro 3D tape as the artwork; classic controls below.
                    val cassetteQueueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = bottomPadding)
                                .animateContentSize(),
                    ) {
                        // "Now Playing" + source header, like the classic ThumbnailHeader.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                                    .padding(top = 8.dp, start = 72.dp, end = 72.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextBackgroundColor,
                            )
                            val cassettePlayingFrom = cassetteQueueTitle ?: mediaMetadata?.album?.title
                            if (!cassettePlayingFrom.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = cassettePlayingFrom,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextBackgroundColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(),
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .doubleTapToSeek(playerSeeker, isRtlLayout, enabled = !isListenTogetherGuest),
                        ) {
                            CassetteTape(
                                isPlaying = effectiveIsPlaying,
                                progress =
                                    if (duration > 0) {
                                        ((sliderPosition ?: effectivePosition).toFloat() / duration).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                                accent = MaterialTheme.colorScheme.primary,
                                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                            )
                            SeekMessage(playerSeeker)
                        }

                        cassetteControls()

                        // The retro bottom row is drawn further down, over the strip the hidden
                        // queue peek keeps free; this leaves the same gap under the transport
                        // as above it.
                        Spacer(Modifier.height(CassetteSpacerUnderTransport))
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = bottomPadding)
                                .animateContentSize(),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            // Remember lambdas to prevent unnecessary recomposition
                            val currentSliderPosition by rememberUpdatedState(sliderPosition)
                            val sliderPositionProvider = remember { { currentSliderPosition } }
                            val isExpandedProvider = remember(state) { { state.isExpanded } }
                            AnimatedContent(
                                targetState = showInlineLyrics,
                                label = "Lyrics",
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                            ) { showLyrics ->
                                if (showLyrics) {
                                    InlineLyricsView(
                                        mediaMetadata = mediaMetadata,
                                        showLyrics = showLyrics,
                                        positionProvider = { effectivePosition },
                                    )
                                } else {
                                    Thumbnail(
                                        sliderPositionProvider = sliderPositionProvider,
                                        modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                        isPlayerExpanded = isExpandedProvider,
                                        isListenTogetherGuest = isListenTogetherGuest,
                                    )
                                }
                            }
                        }

                        if (playerDesign == PlayerDesign.CASSETTE) {
                            cassetteControls()
                            // Leaves room for the retro bottom row, as on the front page; full screen
                            // has no row below.
                            Spacer(Modifier.height(if (isFullScreen) 24.dp else CassetteSpacerUnderTransport))
                        } else {
                            mediaMetadata?.let {
                                controlsContent(it)
                            }

                            Spacer(Modifier.height(30.dp))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                background =
                    if (useBlackBackground) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                onBackgroundColor = onBackgroundColor,
                TextBackgroundColor = TextBackgroundColor,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                pureBlack = pureBlack,
                showInlineLyrics = showInlineLyrics,
                playerBackground = playerBackground,
                onToggleLyrics = {
                    showInlineLyrics = !showInlineLyrics
                },
                // Hide the collapsed queue peek on the RING front player (its lyrics card
                // owns the bottom) and on CASSETTE (its retro bottom row replaces it).
                // The full lyrics page keeps the peek as usual.
                showCollapsedContent = !(
                    (playerDesign == PlayerDesign.RING && !showInlineLyrics) ||
                        playerDesign == PlayerDesign.CASSETTE
                ),
            )
        }

        // A way down from the full player at the top left, where Ring always had one. Ring draws
        // its own inside its top bar and Cassette one in its own style; these three share this.
        if (!isFullScreen && !showInlineLyrics && queueSheetState.progress < 0.999f &&
            playerDesign in setOf(PlayerDesign.CLASSIC, PlayerDesign.FULL_ART, PlayerDesign.RECORD)
        ) {
            RingIconButton(
                res = R.drawable.expand_more,
                tint = TextBackgroundColor,
                size = 28,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                        .padding(start = 6.dp, top = 8.dp)
                        .alpha((1f - queueSheetState.progress).coerceIn(0f, 1f)),
                onClick = { state.collapseSoft() },
            )
        }

        // Cassette's way down: the same place, as one of its own raised keys.
        if (!isFullScreen && !showInlineLyrics && queueSheetState.progress < 0.999f &&
            playerDesign == PlayerDesign.CASSETTE
        ) {
            RetroIconKey(
                iconRes = R.drawable.expand_more,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                        .padding(start = PlayerHorizontalPadding, top = 10.dp)
                        .alpha((1f - queueSheetState.progress).coerceIn(0f, 1f)),
                onClick = { state.collapseSoft() },
            )
        }

        // RING design: bottom overlay, drawn over the queue peek: the synced lines, fading in from
        // above and out below, then the standard button row at the very bottom, where the other
        // designs keep it. Fades out as the queue is dragged open so it doesn't cover the queue.
        if (playerDesign == PlayerDesign.RING && !isFullScreen && !showInlineLyrics &&
            queueSheetState.progress < 0.999f
        ) {
            val overlayLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
            FetchMissingLyrics(mediaMetadata, overlayLyrics)
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .alpha((1f - queueSheetState.progress).coerceIn(0f, 1f))
                        // A darkening that builds towards the bottom keeps the lines and buttons
                        // readable on light artwork without drawing a shape.
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.35f),
                            ),
                        )
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
                horizontalAlignment = Alignment.Start,
            ) {
                AnimatedVisibility(
                    visible = ringHasLyrics,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                ) {
                    RingLyricsCard(
                        lyricsText = overlayLyrics?.lyrics,
                        isLoading = overlayLyrics == null,
                        position = sliderPosition ?: effectivePosition,
                        textColor = TextBackgroundColor,
                        onClick = { showInlineLyrics = true },
                    )
                }
                // The same four controls as every other design, in the same place at the very bottom.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                ) {
                    PlayerBottomButton(
                        icon = R.drawable.queue_music,
                        label = stringResource(R.string.queue),
                        active = false,
                        tint = TextBackgroundColor,
                        activeTint = BlazeThemeColor,
                        modifier = Modifier.weight(1f),
                        onClick = { queueSheetState.expandSoft() },
                    )
                    CastButton(
                        modifier = Modifier.weight(1f),
                        tintColor = TextBackgroundColor,
                        label = stringResource(R.string.cast),
                        activeTint = BlazeThemeColor,
                    )
                    PlayerBottomButton(
                        icon = R.drawable.bedtime,
                        label =
                            if (sleepTimerEnabled) {
                                makeTimeString(sleepTimerTimeLeft)
                            } else {
                                stringResource(R.string.sleep_timer)
                            },
                        active = sleepTimerEnabled,
                        tint = TextBackgroundColor,
                        activeTint = BlazeThemeColor,
                        enabled = !isListenTogetherGuest,
                        modifier = Modifier.weight(1f),
                        onClick = { showSleepTimerDialog = true },
                    )
                    PlayerBottomButton(
                        icon = R.drawable.lyrics,
                        label = stringResource(R.string.lyrics),
                        active = false,
                        tint = TextBackgroundColor,
                        activeTint = BlazeThemeColor,
                        modifier = Modifier.weight(1f),
                        onClick = { showInlineLyrics = true },
                    )
                }
            }
        }

        // CASSETTE design: the retro bottom row sits low, in the strip at the bottom that the
        // hidden queue peek still takes up (78dp above the navigation bar). The queue sheet
        // keeps handling taps and drags there, so the row is drawn over it, the same way as
        // RING's overlay, and fades out as the queue is dragged open.
        if (playerDesign == PlayerDesign.CASSETTE && !isFullScreen &&
            queueSheetState.progress < 0.999f
        ) {
            mediaMetadata?.let { meta ->
                RetroBottomRow(
                    lyricsOpen = showInlineLyrics,
                    sleepTimerEnabled = sleepTimerEnabled,
                    sleepTimerTimeLeft = sleepTimerTimeLeft,
                    sleepEnabled = !isListenTogetherGuest,
                    accent = MaterialTheme.colorScheme.primary,
                    onLyrics = { showInlineLyrics = !showInlineLyrics },
                    onQueue = { queueSheetState.expandSoft() },
                    onSleep = { showSleepTimerDialog = true },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .alpha((1f - queueSheetState.progress).coerceIn(0f, 1f))
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                            ).padding(
                                start = PlayerHorizontalPadding,
                                end = PlayerHorizontalPadding,
                                bottom = CassetteRowBottomMargin,
                            ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/**
 * Looks up the playing song's lyrics when there are none stored yet.
 *
 * Only the lyrics page used to do this, so anything else that shows lyrics — the Ring card —
 * waited on a row that nobody was fetching and showed its loading bars for as long as the song
 * played, whenever the song had not been through a track change first (after a restart, say).
 */
@Composable
private fun FetchMissingLyrics(
    mediaMetadata: MediaMetadata?,
    currentLyrics: LyricsEntity?,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            // Tiny debounce only (rapid skips); the old 500ms delay made lyrics feel slow.
            delay(100)
            // Resolve a usable duration first — fetching with an unknown duration lets
            // the fuzzy providers cache WRONG lyrics. Fall back to the player's duration;
            // if neither is known yet, skip (the service preload will fetch once known).
            val resolvedDuration =
                if (mediaMetadata.duration > 0) {
                    mediaMetadata.duration
                } else {
                    val playerMs = playerConnection.player.duration
                    if (playerMs != C.TIME_UNSET && playerMs > 0) (playerMs / 1000).toInt() else -1
                }
            if (resolvedDuration <= 0) return@LaunchedEffect
            val resolvedMetadata = mediaMetadata.copy(duration = resolvedDuration)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint =
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.blazify.music.di.LyricsHelperEntryPoint::class.java,
                        )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(resolvedMetadata)
                    database.query {
                        upsert(LyricsEntity(resolvedMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

}

@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var appInForeground by remember {
        mutableStateOf(
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
    }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer =
            LifecycleEventObserver { _, _ ->
                appInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val nextMetadata =
        remember(queueWindows, currentWindowIndex) {
            if (currentWindowIndex >= 0 && currentWindowIndex + 1 < queueWindows.size) {
                queueWindows[currentWindowIndex + 1].mediaItem.metadata
            } else {
                null
            }
        }

    FetchMissingLyrics(mediaMetadata, currentLyrics)

    // Prefetch lyrics for the next queue item only while the lyrics pane is visible, the app is in the
    // foreground, and the current track's lyrics row has finished loading (avoids competing with the
    // active fetch).
    LaunchedEffect(
        nextMetadata?.id,
        showLyrics,
        appInForeground,
        mediaMetadata?.id,
        currentLyrics,
    ) {
        if (!showLyrics || !appInForeground || nextMetadata == null) return@LaunchedEffect
        val loadedForCurrent =
            currentLyrics?.let { lyrics ->
                mediaMetadata == null || lyrics.id == mediaMetadata.id
            } == true
        if (mediaMetadata != null && !loadedForCurrent) return@LaunchedEffect
        val nextId = nextMetadata.id
        delay(400)
        if (!showLyrics || !appInForeground || !isActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val existing = database.lyrics(nextId).first()
                if (existing != null) return@withContext
                // Never prefetch without a known duration — fuzzy providers would
                // cache wrong lyrics permanently.
                if (nextMetadata.duration <= 0) return@withContext
                val entryPoint =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.blazify.music.di.LyricsHelperEntryPoint::class.java,
                    )
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(nextMetadata)
                database.query {
                    upsert(LyricsEntity(nextId, fetched.lyrics, fetched.provider))
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lyrics == null -> {
                BlazeLoader()
            }

            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics,
                    )
                }
                ProvideTextStyle(
                    value =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                ) {
                    lyricsContent()
                }
            }
        }
    }
}

@Composable
fun MoreActionsButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(textButtonColor)
                .clickable {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = {
                                mediaMetadata.id.let {
                                    bottomSheetPageState.show {
                                        ShowMediaInfo(it)
                                    }
                                }
                            },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}

/**
 * RING design — a distinct full-player layout (see the "Remedy Vibe" reference):
 * top bar (minimize · "Now Playing" · theme), circular art wrapped by a seekable
 * progress ring, a queue+favourite row, an image-style progress bar with times,
 * transport, a left-aligned sleep-timer icon, and a partial synced-lyrics card
 * that expands to full lyrics on tap. Colours stay album-art dynamic.
 */
@Composable
private fun RingPlayerLayout(
    mediaMetadata: MediaMetadata?,
    isLive: Boolean,
    nowPlayingFrom: String?,
    isFavorite: Boolean,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleOn: Boolean,
    textColor: Color,
    buttonBgColor: Color,
    buttonFgColor: Color,
    bottomReserve: Dp,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onShowLyrics: () -> Unit,
    onCollapse: () -> Unit,
    onToggleShuffle: () -> Unit,
    titleActions: @Composable RowScope.() -> Unit = {},
    seeker: PlayerSeeker? = null,
    rtl: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- top bar: minimize · "Now Playing" · theme (both icons plain, near the edges) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RingIconButton(R.drawable.expand_more, textColor, size = 28, onClick = onCollapse)
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                )
                if (!nowPlayingFrom.isNullOrBlank()) {
                    Text(
                        text = nowPlayingFrom,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }
            // Balances the minimize button so the heading stays centred; the theme button now
            // sits beside the song title.
            Spacer(Modifier.size(46.dp))
        }

        // --- ring ---
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight) * 0.92f
                SeekableAlbumRing(
                    thumbnailUrl = mediaMetadata?.thumbnailUrl,
                    progress = progress,
                    ringColor = MaterialTheme.colorScheme.primary,
                    trackColor = textColor.copy(alpha = 0.16f),
                    // A broadcast has nowhere to seek to.
                    onSeek = { f -> if (duration > 0 && !isLive) onSeek((f * duration).toLong()) },
                    modifier = Modifier.size(side),
                    ringStrokeDp = 7f,
                    artPaddingDp = 18f,
                    thumbColor = MaterialTheme.colorScheme.primary,
                    onDoubleTapArt = seeker?.let { s -> { forward: Boolean -> s.seek(forward) } },
                    rtl = rtl,
                    // The ring is this design's progress bar, so the times sit in a gap at its top.
                    topLabel = {
                        if (isLive) {
                            LiveBadge()
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = makeTimeString(position),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "  —  ",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textColor.copy(alpha = 0.5f),
                                )
                                Text(
                                    text = if (duration != C.TIME_UNSET && duration > 0) makeTimeString(duration) else "--:--",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textColor.copy(alpha = 0.7f),
                                )
                            }
                        }
                    },
                )
                if (seeker != null) SeekMessage(seeker)
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- title/artist on the left · favourite · theme · menu, as on the other designs ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = mediaMetadata?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = textColor,
                    modifier = Modifier.basicMarquee(),
                )
                val artistText = mediaMetadata?.artists?.joinToString { it.name }.orEmpty()
                if (artistText.isNotBlank()) {
                    Text(
                        text = artistText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }
            RingIconButton(
                res = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                // A broadcast is not a song to keep, so the heart is there but out of reach.
                tint =
                    when {
                        isLive -> textColor.copy(alpha = 0.3f)
                        isFavorite -> MaterialTheme.colorScheme.error
                        else -> textColor
                    },
                size = 26,
                enabled = !isLive,
                onClick = onToggleLike,
            )
            titleActions()
        }

        Spacer(Modifier.height(6.dp))

        Spacer(Modifier.height(8.dp))

        Spacer(Modifier.height(10.dp))

        // --- transport ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RingIconButton(
                res = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) BlazeThemeColor else textColor,
                size = 24,
                onClick = onToggleRepeat,
            )
            RingIconButton(R.drawable.skip_previous, textColor, size = 34, onClick = onPrevious)
            // Same morph as the other designs: a circle when paused, a rounded square while playing.
            val playRoundness by animateDpAsState(
                targetValue = if (isPlaying) 22.dp else 33.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "ringPlayRoundness",
            )
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(playRoundness))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
            RingIconButton(R.drawable.skip_next, textColor, size = 34, onClick = onNext)
            RingIconButton(
                res = R.drawable.shuffle,
                tint = if (shuffleOn) BlazeThemeColor else textColor,
                size = 24,
                onClick = onToggleShuffle,
            )
        }

        // Reserve room for the bottom overlay (sleep+more row + lyrics card),
        // which is drawn separately so it can sit above the shared queue peek.
        Spacer(Modifier.height(bottomReserve))
    }
}

/**
 * Bottom lyrics panel: a full-width, top-rounded card anchored to the screen
 * bottom (covers the shared queue peek). Shows a skeleton while lyrics load,
 * then up to three synced partial lines (current highlighted). Tap to expand.
 */
@Composable
private fun RingLyricsCard(
    lyricsText: String?,
    isLoading: Boolean,
    position: Long,
    textColor: Color,
    onClick: () -> Unit,
) {
    val entries = remember(lyricsText) {
        if (lyricsText.isNullOrBlank()) emptyList() else LyricsUtils.parseLyrics(lyricsText)
    }
    val currentIndex = remember(entries, position) {
        if (entries.isEmpty()) -1 else LyricsUtils.findCurrentLineIndex(entries, position)
    }
    val currentOnClick by rememberUpdatedState(onClick)
    // A swipe up opens the lyrics page, as a tap does: the lines read as the top of that page.
    val swipeThreshold = with(LocalDensity.current) { 40.dp.toPx() }
    // No box: the lines sit on the player itself, which keeps the album colours running to the
    // bottom edge.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -swipeThreshold) currentOnClick() },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                    },
                )
            }
            .padding(horizontal = 18.dp, vertical = 12.dp)
            // One height whatever it holds — loading bars, one line or a line that wraps — so the
            // buttons above it stay where they are instead of being pushed into the transport.
            .height(RingLyricsAreaHeight),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                // The line just sung fades in from above and the next one fades out below, so the
                // lines melt into the controls on either side.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Black,
                            0.7f to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            if (isLoading) {
                LyricsSkeleton(textColor)
            } else {
                // The card used to show only its heading until the first line was sung, and stayed
                // that way for songs with no timed lyrics at all: an empty strip across the bottom of
                // the player. It always shows three lines now — what is coming up during the intro,
                // the opening of lyrics that have no timing, or a plain note when there are none.
                val plainLines = remember(lyricsText, entries) {
                    if (entries.isNotEmpty() || lyricsText.isNullOrBlank() || lyricsText == LyricsEntity.LYRICS_NOT_FOUND) {
                        emptyList()
                    } else {
                        lyricsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    }
                }
                when {
                    entries.isNotEmpty() && currentIndex >= 0 -> RingLyricsLines(
                        previous = entries.getOrNull(currentIndex - 1)?.text?.takeIf { it.isNotBlank() },
                        current = entries.getOrNull(currentIndex)?.text?.takeIf { it.isNotBlank() } ?: "♪",
                        next = entries.getOrNull(currentIndex + 1)?.text?.takeIf { it.isNotBlank() },
                        textColor = textColor,
                    )
                    entries.isNotEmpty() -> RingLyricsLines(
                        previous = null,
                        current = "♪",
                        next = entries.firstOrNull { it.text.isNotBlank() }?.text,
                        textColor = textColor,
                    )
                    plainLines.isNotEmpty() -> RingLyricsLines(
                        previous = null,
                        current = plainLines[0],
                        next = plainLines.getOrNull(1),
                        textColor = textColor,
                    )
                    else -> RingLyricsLines(
                        previous = null,
                        current = stringResource(R.string.lyrics_not_found),
                        next = null,
                        textColor = textColor,
                        highlight = false,
                    )
                }
            }
        }
    }
}

/** Room for the card's lines: one above, up to two for the current line, one below. */
private val RingLyricsAreaHeight = 88.dp

/** Three centred lines: the one just sung, the one being sung, and the one coming up. */
@Composable
private fun RingLyricsLines(
    previous: String?,
    current: String,
    next: String?,
    textColor: Color,
    highlight: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = previous ?: " ",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = current,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = next ?: " ",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shimmering placeholder bars shown while lyrics are still loading. */
@Composable
private fun LyricsSkeleton(textColor: Color) {
    val transition = rememberInfiniteTransition(label = "lyricsSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(0.55f, 0.8f, 0.5f).forEachIndexed { i, frac ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(textColor.copy(alpha = if (i == 1) alpha else alpha * 0.7f)),
            )
            if (i < 2) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RingIconButton(
    res: Int,
    tint: Color,
    size: Int = 26,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size((size + 18).dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(res),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size.dp),
        )
    }
}

/** FULL_ART design: album art fills the stage behind a bottom scrim. */
@Composable
private fun FullArtBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.40f),
                                0.35f to Color.Transparent,
                                0.60f to Color.Black.copy(alpha = 0.55f),
                                0.80f to Color.Black.copy(alpha = 0.80f),
                                1.0f to Color.Black.copy(alpha = 0.95f),
                            ),
                        ),
                    ),
        )
    }
}

/* ---------- CASSETTE retro controls (chunky 3D keys, cream + ink, dynamic accent) ---------- */

private val RetroCream = Color(0xFFF2E7D0)
private val RetroInk = Color(0xFF3A2F24)
private val RetroShellTop = Color(0xFF4A3D31)
private val RetroShellBottom = Color(0xFF262019)
private val RetroShellEdge = Color(0xFF6B5B49)
// The cassette card is cream, so the broadcast mark goes darker to stay readable on it.
private val RetroLiveRed = Color(0xFFC62828)

/** Space between the waveform card, the transport row and the bottom row. */
private val CassetteSectionGap = 30.dp

/** How far the bottom row sits above the navigation bar. */
private val CassetteRowBottomMargin = 28.dp

private val CassetteRowHeight = 62.dp

/**
 * The player column already stops [QueuePeekHeight] + 1dp above the navigation bar (the queue
 * sheet's collapsed height), and the bottom row is drawn inside or above that strip. This is the
 * spacer under the transport row that leaves exactly [CassetteSectionGap] above the bottom row.
 */
private val CassetteSpacerUnderTransport =
    CassetteSectionGap - (QueuePeekHeight + 1.dp - CassetteRowHeight - CassetteRowBottomMargin)

/** Retro waveform progress card: times on top, seekable bars. */
@Composable
private fun RetroWaveformCard(
    position: Long,
    duration: Long,
    isLive: Boolean,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(RetroCream)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (isLive) {
                    LiveBadge(color = RetroLiveRed)
                } else {
                    Text(makeTimeString(position), style = MaterialTheme.typography.labelSmall, color = RetroInk)
                    Text(
                        text = if (duration != C.TIME_UNSET && duration > 0) makeTimeString(duration) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = RetroInk,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            val frac = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .pointerInput(duration) {
                        detectTapGestures { off ->
                            if (duration > 0) {
                                onSeek((off.x / size.width * duration).toLong().coerceIn(0L, duration))
                            }
                        }
                    }
                    .pointerInput(duration) {
                        detectHorizontalDragGestures { change, _ ->
                            if (duration > 0) {
                                onSeek((change.position.x / size.width * duration).toLong().coerceIn(0L, duration))
                            }
                        }
                    },
            ) {
                val n = 36
                val gap = size.width / n
                val barW = gap * 0.55f
                for (i in 0 until n) {
                    // Deterministic pseudo-random bar heights (retro EQ look).
                    val wave = kotlin.math.abs(kotlin.math.sin(i * 1.7) * 0.5 + kotlin.math.sin(i * 0.53 + 1.3) * 0.5)
                    val barH = size.height * (0.30f + 0.65f * wave.toFloat()).coerceIn(0.15f, 1f)
                    val x = gap * i + (gap - barW) / 2f
                    drawRoundRect(
                        color = if ((i + 0.5f) / n <= frac) accent else RetroInk.copy(alpha = 0.25f),
                        topLeft = Offset(x, (size.height - barH) / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                    )
                }
            }
        }
    }
}

/** A small raised retro key holding one icon, for the Cassette title row and top bar. */
@Composable
private fun RetroIconKey(
    iconRes: Int,
    tint: Color = RetroInk,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 44.dp, height = 40.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(RetroCream)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Like, theme and song menu as retro keys, beside the Cassette title. */
@Composable
private fun CassetteTitleKeys(
    mediaMetadata: MediaMetadata,
    state: BottomSheetState,
    isFavorite: Boolean,
    isLive: Boolean,
    onToggleLike: () -> Unit,
    lyricsPage: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    onLyricsMenu: () -> Unit = {},
) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (lyricsPage) {
            // On the lyrics page this key goes full screen, as the heart's place does elsewhere.
            RetroIconKey(iconRes = R.drawable.fullscreen, onClick = onToggleFullScreen)
        } else {
            RetroIconKey(
                iconRes = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                // A broadcast is not a song to keep, so the heart is there but out of reach.
                tint =
                    when {
                        isLive -> RetroInk.copy(alpha = 0.3f)
                        isFavorite -> MaterialTheme.colorScheme.error
                        else -> RetroInk
                    },
                enabled = !isLive,
                onClick = onToggleLike,
            )
        }
        RetroIconKey(iconRes = R.drawable.palette) {
            openPlayerDesignGallery(navController, state)
        }
        RetroIconKey(iconRes = R.drawable.more_horiz) {
            if (lyricsPage) {
                onLyricsMenu()
                return@RetroIconKey
            }
            menuState.show {
                PlayerMenu(
                    mediaMetadata = mediaMetadata,
                    playerBottomSheetState = state,
                    onShowDetailsDialog = {
                        mediaMetadata.id.let {
                            bottomSheetPageState.show {
                                ShowMediaInfo(it)
                            }
                        }
                    },
                    onDismiss = menuState::dismiss,
                )
            }
        }
    }
}

/** Raised 3D retro key (cream or accent pill with drop shadow). */
@Composable
private fun RetroKey(
    width: Dp,
    height: Dp,
    bg: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = width, height = height)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) { content() }
}

@Composable
private fun RetroTransportRow(
    isPlaying: Boolean,
    shuffleOn: Boolean,
    repeatMode: Int,
    accent: Color,
    flatColor: Color,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        RingIconButton(
            res = R.drawable.shuffle,
            tint = if (shuffleOn) accent else flatColor,
            size = 22,
            onClick = onToggleShuffle,
        )
        Spacer(Modifier.width(8.dp))
        RetroKey(width = 62.dp, height = 50.dp, bg = RetroCream, onClick = onPrevious) {
            Icon(painterResource(R.drawable.skip_previous), null, tint = RetroInk, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        RetroKey(width = 76.dp, height = 56.dp, bg = accent, onClick = onTogglePlay) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        RetroKey(width = 62.dp, height = 50.dp, bg = RetroCream, onClick = onNext) {
            Icon(painterResource(R.drawable.skip_next), null, tint = RetroInk, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(8.dp))
        RingIconButton(
            res = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            tint = if (repeatMode != Player.REPEAT_MODE_OFF) accent else flatColor,
            size = 22,
            onClick = onToggleRepeat,
        )
    }
}

/**
 * Retro segmented bottom row: queue · cast · sleep timer · lyrics.
 *
 * The same four controls, in the same order, as the bottom row of every other design, drawn
 * as a cream strip matching this design's other keys. Cast is left out where it is unavailable, as it is
 * elsewhere. Lyrics lights up while the lyrics page is open.
 */
@Composable
private fun RetroBottomRow(
    lyricsOpen: Boolean,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    sleepEnabled: Boolean,
    accent: Color,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            // Dark brown like the cassette shell, so the strip reads as part of the tape deck.
            .background(Brush.verticalGradient(listOf(RetroShellTop, RetroShellBottom)))
            .border(1.dp, RetroShellEdge, RoundedCornerShape(18.dp)),
    ) {
        val key = Modifier
            .weight(1f)
            .height(CassetteRowHeight)
            .padding(top = 6.dp)
        PlayerBottomButton(
            icon = R.drawable.queue_music,
            label = stringResource(R.string.queue),
            active = false,
            tint = RetroCream,
            activeTint = accent,
            modifier = key,
            onClick = onQueue,
        )
        CastButton(
            modifier = key,
            tintColor = RetroCream,
            label = stringResource(R.string.cast),
            activeTint = accent,
        )
        PlayerBottomButton(
            icon = R.drawable.bedtime,
            label = if (sleepTimerEnabled) makeTimeString(sleepTimerTimeLeft) else stringResource(R.string.sleep_timer),
            active = sleepTimerEnabled,
            tint = RetroCream,
            activeTint = accent,
            enabled = sleepEnabled,
            modifier = key,
            onClick = onSleep,
        )
        PlayerBottomButton(
            icon = R.drawable.lyrics,
            label = stringResource(R.string.lyrics),
            active = lyricsOpen,
            tint = RetroCream,
            activeTint = accent,
            modifier = key,
            onClick = onLyrics,
        )
    }
}

private const val ReopenPlayerKey = "reopen_player"
private const val CollapsePlayerKey = "collapse_player"
const val PLAYER_DESIGN_GALLERY_ROUTE = "settings/appearance/player_design"

/**
 * Opens the design gallery and then moves the player out of its way, and marks the page
 * underneath so the full player comes back when the user returns from the gallery.
 */
private fun openPlayerDesignGallery(navController: NavController, state: BottomSheetState) {
    // The button stays tappable while the player slides down, so a quick second tap would open
    // the gallery twice and Back would need pressing twice.
    val current = navController.currentBackStackEntry ?: return
    if (current.destination.route == PLAYER_DESIGN_GALLERY_ROUTE) return
    current.savedStateHandle[ReopenPlayerKey] = true
    navController.navigate(PLAYER_DESIGN_GALLERY_ROUTE) { launchSingleTop = true }
    // The player slides down once the gallery has finished opening behind it (see
    // BottomSheetPlayer); collapsing first showed the page underneath for a moment.
    navController.currentBackStackEntry?.savedStateHandle?.set(CollapsePlayerKey, true)
}

@Composable
private fun PlayerThemeButton(
    textButtonColor: Color,
    iconButtonColor: Color,
    state: BottomSheetState,
) {
    val navController = LocalNavController.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(textButtonColor)
                .clickable {
                    openPlayerDesignGallery(navController, state)
                },
    ) {
        Image(
            painter = painterResource(R.drawable.palette),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}

@Composable
private fun PlayerMoreMenuButton(
    mediaMetadata: MediaMetadata,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(textButtonColor)
                .clickable {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = {
                                mediaMetadata.id.let {
                                    bottomSheetPageState.show {
                                        ShowMediaInfo(it)
                                    }
                                }
                            },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}
