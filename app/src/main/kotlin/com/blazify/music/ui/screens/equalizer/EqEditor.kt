/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.equalizer

import android.media.audiofx.PresetReverb
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.R
import com.blazify.music.eq.AudioEffectsState
import com.blazify.music.eq.data.EqPresets
import com.blazify.music.eq.data.ParametricEQBand
import kotlin.math.roundToInt

/** Gain range of each band, in dB either side of flat. */
private const val GAIN_RANGE = 12f

/**
 * The equalizer proper: a live response curve, a preset row, one drag handle per
 * band, preamp, and the platform effects that sit after the EQ.
 */
@Composable
fun EqEditor(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    bandGains: List<Double>,
    onBandGainChange: (Int, Double) -> Unit,
    preamp: Double,
    onPreampChange: (Double) -> Unit,
    presetId: String?,
    onPresetSelected: (String) -> Unit,
    onReset: () -> Unit,
    effects: AudioEffectsState,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onReverbChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Master switch
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.eq_enable),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.eq_enable_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }

        // Live response curve for what the sliders currently say.
        val bands = EqPresets.FREQUENCIES.mapIndexed { index, frequency ->
            ParametricEQBand(frequency = frequency, gain = bandGains.getOrElse(index) { 0.0 })
        }
        EqFrequencyResponseGraph(bands = bands, preamp = preamp)

        // Presets
        SectionHeader(
            title = stringResource(R.string.eq_presets),
            trailing = {
                TextButton(onClick = onReset) { Text(stringResource(R.string.eq_reset)) }
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            EqPresets.ALL.forEach { preset ->
                PresetChip(
                    label = stringResource(preset.nameRes),
                    selected = preset.id == presetId,
                    onClick = { onPresetSelected(preset.id) },
                )
            }
            // Shown only once the user has dragged away from every preset.
            if (presetId == null) {
                PresetChip(
                    label = stringResource(R.string.eq_custom),
                    selected = true,
                    onClick = {},
                )
            }
        }

        // Band sliders
        SectionHeader(title = stringResource(R.string.eq_bands))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        ) {
            EqPresets.FREQUENCIES.forEachIndexed { index, frequency ->
                BandSlider(
                    gain = bandGains.getOrElse(index) { 0.0 }.toFloat(),
                    frequency = frequency,
                    onGainChange = { onBandGainChange(index, it.toDouble()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Preamp
        SectionHeader(
            title = stringResource(R.string.eq_preamp),
            trailing = {
                Text(
                    text = "%+.1f dB".format(preamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
        Slider(
            value = preamp.toFloat(),
            onValueChange = { onPreampChange(it.toDouble()) },
            valueRange = -GAIN_RANGE..GAIN_RANGE,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )

        // Platform effects, which sit after the EQ on the output session.
        SectionHeader(title = stringResource(R.string.eq_effects))
        EffectSlider(
            label = stringResource(R.string.eq_bass_boost),
            strength = effects.bassBoost,
            onChange = onBassBoostChange,
        )
        EffectSlider(
            label = stringResource(R.string.eq_virtualizer),
            strength = effects.virtualizer,
            onChange = onVirtualizerChange,
        )
        ReverbRow(preset = effects.reverbPreset, onChange = onReverbChange)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * One vertical band control. Drawn rather than using a rotated [Slider] because a
 * rotated slider reports its drags in the wrong axis and fights the parent list's
 * vertical scrolling.
 */
@Composable
private fun BandSlider(
    gain: Float,
    frequency: Double,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackHeight = 150.dp
    val density = LocalDensity.current
    val trackPx = with(density) { trackHeight.toPx() }
    val fraction = ((gain + GAIN_RANGE) / (GAIN_RANGE * 2)).coerceIn(0f, 1f)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = "%+d".format(gain.roundToInt()),
            style = MaterialTheme.typography.labelSmall,
            color = if (gain == 0f) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(trackHeight)
                .width(34.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val f = 1f - (change.position.y / trackPx).coerceIn(0f, 1f)
                            onGainChange(f * GAIN_RANGE * 2 - GAIN_RANGE)
                        },
                    )
                },
        ) {
            // Rail
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            // Fill runs from the centre line out to the handle, so cuts read as
            // clearly as boosts.
            val fillHeight = trackHeight * kotlin.math.abs(fraction - 0.5f)
            val fillBottom = trackHeight * minOf(fraction, 0.5f)
            Box(
                Modifier
                    .width(5.dp)
                    .height(fillHeight)
                    .align(Alignment.BottomCenter)
                    .offset(y = -fillBottom)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            // Handle
            Box(
                Modifier
                    .size(18.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 9.dp - (trackHeight * fraction))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = frequency.toLabel(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Double.toLabel(): String = when {
    this >= 1000 -> "${(this / 1000).roundToInt()}k"
    this >= 100 -> roundToInt().toString()
    else -> roundToInt().toString()
}

@Composable
private fun EffectSlider(label: String, strength: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = "${strength / 10}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = strength.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..1000f,
        )
    }
}

@Composable
private fun ReverbRow(preset: Int, onChange: (Int) -> Unit) {
    val presets = listOf(
        PresetReverb.PRESET_NONE to R.string.eq_reverb_none,
        PresetReverb.PRESET_SMALLROOM to R.string.eq_reverb_small_room,
        PresetReverb.PRESET_MEDIUMROOM to R.string.eq_reverb_medium_room,
        PresetReverb.PRESET_LARGEROOM to R.string.eq_reverb_large_room,
        PresetReverb.PRESET_MEDIUMHALL to R.string.eq_reverb_medium_hall,
        PresetReverb.PRESET_LARGEHALL to R.string.eq_reverb_large_hall,
        PresetReverb.PRESET_PLATE to R.string.eq_reverb_plate,
    )
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.eq_reverb),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            presets.forEach { (value, labelRes) ->
                PresetChip(
                    label = stringResource(labelRes),
                    selected = preset == value.toInt(),
                    onClick = { onChange(value.toInt()) },
                )
            }
        }
    }
}
