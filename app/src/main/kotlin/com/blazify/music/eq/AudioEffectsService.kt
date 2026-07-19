/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.eq

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The platform audio effects that sit alongside the parametric EQ: bass boost,
 * stereo widening and reverb.
 *
 * These are separate from [EqualizerService], which is a custom DSP processor in
 * ExoPlayer's chain. These attach to the output session instead, so they need
 * re-attaching whenever that session changes and releasing when it goes away.
 *
 * Every call is defensive: these effects are optional on Android and a device is
 * free to not implement any of them, in which case construction throws and the
 * control simply stays off.
 */
@Singleton
class AudioEffectsService @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("blazify_audio_effects", Context.MODE_PRIVATE)

    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var sessionId: Int = 0

    private val _state = MutableStateFlow(
        AudioEffectsState(
            bassBoost = prefs.getInt(KEY_BASS, 0),
            virtualizer = prefs.getInt(KEY_VIRTUAL, 0),
            reverbPreset = prefs.getInt(KEY_REVERB, PresetReverb.PRESET_NONE.toInt()),
        ),
    )
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    /** True when the device actually provides these effects. */
    val isSupported: Boolean
        get() = bassBoost != null || virtualizer != null || reverb != null

    /** Binds to a playback session; safe to call repeatedly with the same id. */
    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == sessionId) return
        release()
        sessionId = audioSessionId

        bassBoost = runCatching { BassBoost(EFFECT_PRIORITY, audioSessionId) }
            .onFailure { Timber.w(it, "BassBoost unavailable") }.getOrNull()
        virtualizer = runCatching { Virtualizer(EFFECT_PRIORITY, audioSessionId) }
            .onFailure { Timber.w(it, "Virtualizer unavailable") }.getOrNull()
        reverb = runCatching { PresetReverb(EFFECT_PRIORITY, audioSessionId) }
            .onFailure { Timber.w(it, "PresetReverb unavailable") }.getOrNull()

        applyAll()
    }

    @Synchronized
    fun release() {
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { reverb?.release() }
        bassBoost = null
        virtualizer = null
        reverb = null
        sessionId = 0
    }

    /** @param strength 0..1000, the platform's own scale. */
    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, MAX_STRENGTH)
        _state.value = _state.value.copy(bassBoost = clamped)
        prefs.edit { putInt(KEY_BASS, clamped) }
        applyBassBoost()
    }

    /** @param strength 0..1000, the platform's own scale. */
    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, MAX_STRENGTH)
        _state.value = _state.value.copy(virtualizer = clamped)
        prefs.edit { putInt(KEY_VIRTUAL, clamped) }
        applyVirtualizer()
    }

    /** @param preset one of the [PresetReverb] PRESET_* values. */
    fun setReverbPreset(preset: Int) {
        _state.value = _state.value.copy(reverbPreset = preset)
        prefs.edit { putInt(KEY_REVERB, preset) }
        applyReverb()
    }

    private fun applyAll() {
        applyBassBoost()
        applyVirtualizer()
        applyReverb()
    }

    private fun applyBassBoost() {
        val effect = bassBoost ?: return
        val strength = _state.value.bassBoost
        runCatching {
            // Some devices only support on/off; asking for a level they cannot do
            // throws rather than rounding, so treat failure as "leave it alone".
            effect.enabled = strength > 0
            if (effect.strengthSupported) {
                effect.setStrength(strength.toShort())
            }
        }.onFailure { Timber.w(it, "Could not apply bass boost") }
    }

    private fun applyVirtualizer() {
        val effect = virtualizer ?: return
        val strength = _state.value.virtualizer
        runCatching {
            effect.enabled = strength > 0
            if (effect.strengthSupported) {
                effect.setStrength(strength.toShort())
            }
        }.onFailure { Timber.w(it, "Could not apply virtualizer") }
    }

    private fun applyReverb() {
        val effect = reverb ?: return
        val preset = _state.value.reverbPreset
        runCatching {
            effect.enabled = preset != PresetReverb.PRESET_NONE.toInt()
            effect.preset = preset.toShort()
        }.onFailure { Timber.w(it, "Could not apply reverb") }
    }

    private companion object {
        // Above zero so we outrank a passive system effect, but not so high that
        // we fight a foreground equalizer app the user opened deliberately.
        const val EFFECT_PRIORITY = 1
        const val MAX_STRENGTH = 1000
        const val KEY_BASS = "bass_boost"
        const val KEY_VIRTUAL = "virtualizer"
        const val KEY_REVERB = "reverb_preset"
    }
}

data class AudioEffectsState(
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val reverbPreset: Int = 0,
)
