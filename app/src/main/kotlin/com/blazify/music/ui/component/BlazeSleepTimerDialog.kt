/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.blazify.music.R
import com.blazify.music.utils.makeTimeString
import kotlin.math.roundToInt

/**
 * Blazify-style sleep timer dialog: big time readout, 15/30/45/60m preset chips,
 * amber CANCEL/START pill buttons, an end-of-song option, and a live countdown with
 * END/RESET while a timer is running.
 */
@Composable
fun BlazeSleepTimerDialog(
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    pauseWhenSongEnd: Boolean,
    initialMinutes: Float,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
    onStartEndOfSong: () -> Unit,
    onClear: () -> Unit,
) {
    var sleepTimerValue by remember { mutableFloatStateOf(initialMinutes) }
    val accent = MaterialTheme.colorScheme.primary

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.bedtime),
                contentDescription = null,
                tint = accent,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.sleep_timer_stop_music_after),
                fontWeight = FontWeight.Bold,
            )
        },
        confirmButton = {},
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!sleepTimerEnabled) {
                    val selectedMinutes = sleepTimerValue.roundToInt()
                    Text(
                        text = String.format("%02d:%02d:00", selectedMinutes / 60, selectedMinutes % 60),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        ),
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(15, 30, 45, 60).forEach { minutes ->
                            val selected = selectedMinutes == minutes
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (selected) accent
                                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        )
                                        .clickable { sleepTimerValue = minutes.toFloat() }
                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = "${minutes}m",
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (selected) Color.White
                                        else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onStartEndOfSong,
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(2.dp, accent),
                    ) {
                        Text(
                            text = stringResource(R.string.end_of_song),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(2.dp, accent),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                        ) {
                            Text(
                                text = stringResource(android.R.string.cancel).uppercase(),
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(Modifier.width(24.dp))

                        Button(
                            onClick = { onStart(sleepTimerValue.roundToInt()) },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color.White,
                            ),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer_start),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    Text(
                        text =
                            if (pauseWhenSongEnd) {
                                stringResource(R.string.end_of_song)
                            } else {
                                makeTimeString(sleepTimerTimeLeft)
                            },
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onClear()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(2.dp, accent),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer_end),
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(Modifier.width(24.dp))

                        Button(
                            onClick = {
                                // Reset: cancel the running timer and return to the picker
                                onClear()
                                sleepTimerValue = initialMinutes
                            },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color.White,
                            ),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer_reset),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
    )
}
