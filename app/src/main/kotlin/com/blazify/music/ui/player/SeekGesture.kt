/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.player

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.R
import com.blazify.music.constants.SeekAmountSecondsKey
import com.blazify.music.constants.SeekExtraSeconds
import com.blazify.music.playback.PlayerConnection
import com.blazify.music.utils.rememberPreference
import kotlinx.coroutines.delay

/**
 * Moves playback back or forward by the amount picked in Player settings, and
 * keeps the short message that tells you it happened.
 *
 * With progressive seek on, seeking again within a second adds another step,
 * so three quick taps at 10 seconds jump 30.
 */
@Stable
class PlayerSeeker internal constructor(
    private val playerConnection: PlayerConnection,
    private val context: Context,
) {
    internal var stepSeconds = 10
    internal var progressive = false
    private var steps = 1
    private var lastSeekAt = 0L

    /** "+10 seconds forward" and the like, or null when there is nothing to show. */
    var message by mutableStateOf<String?>(null)
        private set

    internal var messageId by mutableIntStateOf(0)
        private set

    fun seek(forward: Boolean) {
        val now = SystemClock.uptimeMillis()
        steps = if (progressive && now - lastSeekAt < 1000) steps + 1 else 1
        lastSeekAt = now

        val player = playerConnection.player
        val jumpMs = stepSeconds * 1000L * steps
        val position = player.currentPosition
        val duration = player.duration
        val target =
            if (forward) {
                if (duration > 0) (position + jumpMs).coerceAtMost(duration) else position + jumpMs
            } else {
                (position - jumpMs).coerceAtLeast(0)
            }
        player.seekTo(target)

        message = context.getString(
            if (forward) R.string.seek_forward_dynamic else R.string.seek_backward_dynamic,
            stepSeconds * steps,
        )
        messageId++
    }

    internal fun clearMessage() {
        message = null
    }
}

@Composable
fun rememberPlayerSeeker(playerConnection: PlayerConnection): PlayerSeeker {
    val context = LocalContext.current
    val stepSeconds by rememberPreference(SeekAmountSecondsKey, defaultValue = 10)
    val progressive by rememberPreference(SeekExtraSeconds, defaultValue = false)
    val seeker = remember(playerConnection) { PlayerSeeker(playerConnection, context.applicationContext) }
    SideEffect {
        seeker.stepSeconds = stepSeconds
        seeker.progressive = progressive
    }
    return seeker
}

/** Double-tap the left half to go back and the right half to go forward (mirrored for RTL). */
fun Modifier.doubleTapToSeek(seeker: PlayerSeeker, rtl: Boolean, enabled: Boolean = true): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(seeker, rtl) {
            detectTapGestures(
                onDoubleTap = { offset ->
                    val leftHalf = offset.x < size.width / 2
                    seeker.seek(forward = leftHalf == rtl)
                },
            )
        }
    }

/** The small "+10 seconds forward" note, gone a second after the last seek. */
@Composable
fun SeekMessage(seeker: PlayerSeeker, modifier: Modifier = Modifier) {
    val message = seeker.message
    var lastMessage by remember { mutableStateOf("") }
    SideEffect { if (message != null) lastMessage = message }

    LaunchedEffect(seeker.messageId) {
        if (seeker.message != null) {
            delay(1000)
            seeker.clearMessage()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Text(
            text = message ?: lastMessage,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(8.dp),
        )
    }
}
