/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 *
 * How a festival looks: a warm glow and a lit diya on the Home logo, and, when the logo is
 * tapped, a few seconds of celebration over the whole screen. All of it drawn here, so it
 * adds no images, no animation files and no sound files to the app.
 */

package com.blazify.music.festival

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blazify.music.BuildConfig
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.R
import com.blazify.music.constants.ContentCountryKey
import com.blazify.music.constants.FestivalTouchesKey
import com.blazify.music.constants.SYSTEM_DEFAULT
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val Marigold = Color(0xFFFFA726)
private val Flame = Color(0xFFFFB300)
private val FlameCore = Color(0xFFFFF59D)
private val Clay = Color(0xFFB5532A)
private val ClayLight = Color(0xFFE07A3F)
private val ClayDark = Color(0xFF5A2614)
private val RangoliColors = listOf(Color(0xFFFFA726), Color(0xFFE91E63), Color(0xFF26A69A), Color(0xFFFFD54F))

/** Test copies built with -PforceFestival=all step through every festival with [FestivalTestButton]. */
private val testIndex = androidx.compose.runtime.mutableIntStateOf(0)

private val testingAll get() = BuildConfig.FORCE_FESTIVAL == "all"

/** Today's festival here, or null: none today, none in this country, or switched off. */
@Composable
fun rememberActiveFestival(): Festival? {
    val context = LocalContext.current
    val enabled by rememberPreference(FestivalTouchesKey, true)
    val chosenCountry by rememberPreference(ContentCountryKey, SYSTEM_DEFAULT)
    if (testingAll) return Festival.entries[testIndex.intValue % Festival.entries.size]
    if (!enabled) return null
    return remember(chosenCountry) {
        // Test copies can be built to pretend it is a festival day (-PforceFestival=tihar);
        // the copies people install are built without it.
        Festival.entries.firstOrNull { it.id == BuildConfig.FORCE_FESTIVAL }
            ?: Festival.on(LocalDate.now(), festivalCountry(context, chosenCountry.takeUnless { it == SYSTEM_DEFAULT }))
    }
}

/**
 * The Home logo in festival dress, moving, at exactly the logo's own size: the decoration lives
 * inside the logo's square, so nothing in the header moves on a festival day. Tapping it plays
 * the celebration.
 */
@Composable
fun FestivalLogo(
    festival: Festival,
    isDark: Boolean,
    logoSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    var celebrating by remember { mutableStateOf(false) }
    FestivalLogoArt(
        festival = festival,
        isDark = isDark,
        size = logoSize,
        modifier =
            modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { celebrating = true },
    )
    if (celebrating) FestivalCelebration(festival) { celebrating = false }
}

/** Only in test copies built with -PforceFestival=all: a button over Home to step to the next festival. */
@Composable
fun FestivalTestButton(modifier: Modifier = Modifier) {
    if (!testingAll) return
    val festival = Festival.entries[testIndex.intValue % Festival.entries.size]
    androidx.compose.material3.Surface(
        onClick = { testIndex.intValue = (testIndex.intValue + 1) % Festival.entries.size },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = Color(0xE6202020),
        contentColor = Color.White,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Text(
            text = "${festival.id}  ${festival.ordinal + 1}/${Festival.entries.size}  ›",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
