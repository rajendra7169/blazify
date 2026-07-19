package com.blazify.music.ui.screens.equalizer

import com.blazify.music.eq.data.SavedEQProfile

/**
 * UI State for EQ Screen
 */
data class EQState(
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val importStatus: String? = null,
    val error: String? = null,
    /** Gains of the ten-band editor, in dB, one per EqPresets.FREQUENCIES entry. */
    val bandGains: List<Double> = List(10) { 0.0 },
    val preamp: Double = 0.0,
    /** Which built-in preset the current gains match, or null once edited away. */
    val presetId: String? = com.blazify.music.eq.data.EqPresets.FLAT_ID,
    /** Whether the ten-band editor is driving playback. */
    val eqEnabled: Boolean = false,
)