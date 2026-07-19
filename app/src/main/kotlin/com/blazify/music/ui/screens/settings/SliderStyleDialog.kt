/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blazify.music.R
import com.blazify.music.constants.PlayerBackgroundStyle
import com.blazify.music.constants.SliderStyle
import com.blazify.music.ui.component.CapsuleSeekBar
import com.blazify.music.ui.component.DefaultDialog
import com.blazify.music.ui.component.PlayerSliderTrack
import com.blazify.music.ui.component.SquigglySlider
import com.blazify.music.ui.component.WavySlider
import com.blazify.music.ui.theme.PlayerSliderColors

/**
 * The four slider styles as live tiles, each drawing the real component rather
 * than a picture of it. Shared by Appearance and Look & Feel so the two screens
 * can never drift apart.
 *
 * Squiggly is a variant of [SliderStyle.WAVY] rather than its own enum entry,
 * so selection is reported as a (style, squiggly) pair.
 */
@Composable
fun SliderStyleDialog(
    current: SliderStyle,
    squiggly: Boolean,
    onSelect: (SliderStyle, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        onDismiss = onDismiss,
    ) {
        val previewColors = PlayerSliderColors.getSliderColors(
            MaterialTheme.colorScheme.primary,
            PlayerBackgroundStyle.DEFAULT,
            isSystemInDarkTheme(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StyleTile(
                    label = stringResource(R.string.slider_style_capsule),
                    selected = current == SliderStyle.DEFAULT,
                    onClick = { onSelect(SliderStyle.DEFAULT, false) },
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CapsuleSeekBar(
                            position = 36_000L,
                            duration = 179_000L,
                            onSeek = { /* preview only */ },
                            colors = previewColors,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            enabled = false,
                            compact = true,
                        )
                    }
                }
                StyleTile(
                    label = stringResource(R.string.wavy),
                    selected = current == SliderStyle.WAVY && !squiggly,
                    onClick = { onSelect(SliderStyle.WAVY, false) },
                ) {
                    WavySlider(
                        value = 0.5f,
                        valueRange = 0f..1f,
                        onValueChange = { /* preview only */ },
                        colors = previewColors,
                        modifier = Modifier.weight(1f),
                        isPlaying = true,
                        enabled = false,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StyleTile(
                    label = stringResource(R.string.slim),
                    selected = current == SliderStyle.SLIM,
                    onClick = { onSelect(SliderStyle.SLIM, false) },
                ) {
                    Slider(
                        value = 0.65f,
                        valueRange = 0f..1f,
                        onValueChange = { /* preview only */ },
                        thumb = { Spacer(Modifier.size(0.dp)) },
                        track = { state ->
                            PlayerSliderTrack(sliderState = state, colors = previewColors)
                        },
                        colors = previewColors,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                StyleTile(
                    label = stringResource(R.string.squiggly),
                    selected = current == SliderStyle.WAVY && squiggly,
                    onClick = { onSelect(SliderStyle.WAVY, true) },
                ) {
                    SquigglySlider(
                        value = 0.5f,
                        valueRange = 0f..1f,
                        onValueChange = { /* preview only */ },
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        colors = previewColors,
                        isPlaying = true,
                    )
                }
            }
        }
    }
}

/** One square tile: the live slider above its name, outlined when it is the active style. */
@Composable
private fun RowScope.StyleTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .aspectRatio(1f)
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        content()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
