/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.eq.data

import com.blazify.music.R

/**
 * Built-in equalizer presets.
 *
 * The engine underneath is fully parametric, but presets are expressed on the
 * familiar ten-band graphic layout so the UI can show one slider per band and
 * users get the curve they expect from other players.
 */
object EqPresets {

    /** ISO centre frequencies, the same ten every graphic EQ uses. */
    val FREQUENCIES = listOf(31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)

    /** Widened slightly from the default so neighbouring bands overlap smoothly. */
    private const val BAND_Q = 1.41

    const val FLAT_ID = "flat"

    data class EqPreset(
        val id: String,
        val nameRes: Int,
        /** One gain in dB per entry in [FREQUENCIES]. */
        val gains: List<Double>,
    ) {
        fun toBands(): List<ParametricEQBand> = FREQUENCIES.mapIndexed { index, frequency ->
            ParametricEQBand(
                frequency = frequency,
                gain = gains.getOrElse(index) { 0.0 },
                q = BAND_Q,
                filterType = FilterType.PK,
            )
        }

        /**
         * Headroom so boosted bands cannot clip. Only positive gain needs
         * compensating; a preset that only cuts leaves the preamp alone.
         */
        fun suggestedPreamp(): Double = -maxOf(0.0, gains.maxOrNull() ?: 0.0)
    }

    val ALL = listOf(
        EqPreset(FLAT_ID, R.string.eq_preset_flat, List(10) { 0.0 }),
        EqPreset("bass_boost", R.string.eq_preset_bass_boost, listOf(7.0, 6.0, 4.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
        EqPreset("treble_boost", R.string.eq_preset_treble_boost, listOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 3.0, 5.0, 6.0, 7.0)),
        EqPreset("vocal", R.string.eq_preset_vocal, listOf(-2.0, -1.0, 0.0, 2.0, 4.0, 4.0, 3.0, 2.0, 0.0, -1.0)),
        EqPreset("rock", R.string.eq_preset_rock, listOf(5.0, 4.0, 3.0, 1.0, -1.0, -1.0, 2.0, 3.0, 4.0, 4.0)),
        EqPreset("pop", R.string.eq_preset_pop, listOf(-1.0, 0.0, 2.0, 4.0, 4.0, 2.0, 0.0, -1.0, -1.0, -1.0)),
        EqPreset("hip_hop", R.string.eq_preset_hip_hop, listOf(6.0, 5.0, 2.0, 3.0, -1.0, -1.0, 1.0, 2.0, 3.0, 4.0)),
        EqPreset("electronic", R.string.eq_preset_electronic, listOf(5.0, 4.0, 1.0, 0.0, -2.0, 2.0, 1.0, 1.0, 4.0, 5.0)),
        EqPreset("jazz", R.string.eq_preset_jazz, listOf(4.0, 3.0, 1.0, 2.0, -1.0, -1.0, 0.0, 1.0, 3.0, 4.0)),
        EqPreset("classical", R.string.eq_preset_classical, listOf(5.0, 4.0, 3.0, 2.0, -1.0, -1.0, 0.0, 2.0, 3.0, 4.0)),
        EqPreset("acoustic", R.string.eq_preset_acoustic, listOf(4.0, 3.0, 2.0, 1.0, 1.0, 1.0, 2.0, 3.0, 3.0, 2.0)),
        EqPreset("loudness", R.string.eq_preset_loudness, listOf(6.0, 4.0, 1.0, 0.0, -1.0, 0.0, 1.0, 3.0, 5.0, 6.0)),
        EqPreset("podcast", R.string.eq_preset_podcast, listOf(-4.0, -3.0, -1.0, 2.0, 4.0, 4.0, 3.0, 1.0, -1.0, -3.0)),
    )

    /**
     * Whichever preset these gains match, or null once the user has edited away
     * from all of them. Compared loosely because gains round-trip through the
     * slider as floats.
     */
    fun matching(gains: List<Double>): EqPreset? = ALL.firstOrNull { preset ->
        preset.gains.size == gains.size &&
            preset.gains.indices.all { kotlin.math.abs(preset.gains[it] - gains[it]) < 0.05 }
    }
}
