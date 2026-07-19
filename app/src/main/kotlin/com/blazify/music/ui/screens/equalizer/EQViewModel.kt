package com.blazify.music.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.music.eq.AudioEffectsService
import com.blazify.music.eq.EqualizerService
import com.blazify.music.eq.data.EQProfileRepository
import com.blazify.music.eq.data.EqPresets
import com.blazify.music.eq.data.ParametricEQParser
import com.blazify.music.eq.data.SavedEQProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

/**
 * ViewModel for EQ Screen
 * Manages EQ profiles and applies them to the EqualizerService
 */
@HiltViewModel
class EQViewModel @Inject constructor(
    private val eqProfileRepository: EQProfileRepository,
    private val equalizerService: EqualizerService,
    private val audioEffectsService: AudioEffectsService,
) : ViewModel() {

    /** Bass boost / surround / reverb, which live on the output session. */
    val effects = audioEffectsService.state

    fun setBassBoost(strength: Int) = audioEffectsService.setBassBoost(strength)

    fun setVirtualizer(strength: Int) = audioEffectsService.setVirtualizer(strength)

    fun setReverbPreset(preset: Int) = audioEffectsService.setReverbPreset(preset)

    private val _state = MutableStateFlow(EQState())
    val state: StateFlow<EQState> = _state.asStateFlow()

    init {
        loadProfiles()
        restoreBandEditor()
    }

    /**
     * The ten-band editor is stored as an ordinary profile so it survives restarts
     * and rides the same apply path as imported ones. Its id is fixed, which also
     * keeps it out of the saved-profile list (that list only shows custom imports).
     */
    private fun restoreBandEditor() {
        val existing = eqProfileRepository.getAllProfiles().find { it.id == BAND_PROFILE_ID }
        if (existing != null) {
            val gains = EqPresets.FREQUENCIES.map { frequency ->
                existing.bands.find { it.frequency == frequency }?.gain ?: 0.0
            }
            _state.update {
                it.copy(
                    bandGains = gains,
                    preamp = existing.preamp,
                    presetId = EqPresets.matching(gains)?.id,
                    eqEnabled = eqProfileRepository.getActiveProfile()?.id == BAND_PROFILE_ID,
                )
            }
        }
    }

    /** Turns the band editor on or off without discarding the gains. */
    fun setEqEnabled(enabled: Boolean) {
        _state.update { it.copy(eqEnabled = enabled) }
        viewModelScope.launch {
            if (enabled) {
                applyBandEditor()
            } else {
                equalizerService.disable()
                eqProfileRepository.setActiveProfile(null)
            }
        }
    }

    fun selectPreset(presetId: String) {
        val preset = EqPresets.ALL.find { it.id == presetId } ?: return
        _state.update {
            it.copy(
                bandGains = preset.gains,
                preamp = preset.suggestedPreamp(),
                presetId = preset.id,
                eqEnabled = true,
            )
        }
        viewModelScope.launch { applyBandEditor() }
    }

    fun setBandGain(index: Int, gain: Double) {
        val gains = _state.value.bandGains.toMutableList()
        if (index !in gains.indices) return
        gains[index] = gain
        _state.update {
            it.copy(
                bandGains = gains,
                presetId = EqPresets.matching(gains)?.id,
                eqEnabled = true,
            )
        }
        viewModelScope.launch { applyBandEditor() }
    }

    fun setPreamp(preamp: Double) {
        _state.update { it.copy(preamp = preamp, eqEnabled = true) }
        viewModelScope.launch { applyBandEditor() }
    }

    fun resetBands() = selectPreset(EqPresets.FLAT_ID)

    /** Persists the current gains and pushes them into the audio chain. */
    private suspend fun applyBandEditor() {
        val current = _state.value
        val preset = current.presetId?.let { id -> EqPresets.ALL.find { it.id == id } }
        val profile = SavedEQProfile(
            id = BAND_PROFILE_ID,
            name = preset?.let { "" } ?: "",
            deviceModel = "",
            bands = EqPresets.FREQUENCIES.mapIndexed { index, frequency ->
                com.blazify.music.eq.data.ParametricEQBand(
                    frequency = frequency,
                    gain = current.bandGains.getOrElse(index) { 0.0 },
                    q = 1.41,
                )
            },
            preamp = current.preamp,
            isCustom = false,
        )
        eqProfileRepository.saveProfile(profile)
        equalizerService.applyProfile(profile)
            .onSuccess { eqProfileRepository.setActiveProfile(BAND_PROFILE_ID) }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    private companion object {
        const val BAND_PROFILE_ID = "blazify_band_eq"
    }

    /**
     * Load all saved EQ profiles (sorted: AutoEQ first, then custom)
     */
    private fun loadProfiles() {
        // Observe profiles changes
        viewModelScope.launch {
            eqProfileRepository.profiles.collect { _ ->
                val sortedProfiles = eqProfileRepository.getSortedProfiles()
                _state.update {
                    it.copy(profiles = sortedProfiles)
                }
            }
        }

        // Observe active profile changes separately
        viewModelScope.launch {
            eqProfileRepository.activeProfile.collect { activeProfile ->
                _state.update {
                    it.copy(activeProfileId = activeProfile?.id)
                }
            }
        }
    }

    /**
     * Select and apply an EQ profile
     * Pass null to disable EQ
     */
    fun selectProfile(profileId: String?) {
        viewModelScope.launch {
            if (profileId == null) {
                // Disable EQ
                equalizerService.disable()
                eqProfileRepository.setActiveProfile(null)
                // Keep the band editor's switch honest — picking "no equalization"
                // or a saved profile means the editor is no longer driving output.
                _state.update { it.copy(eqEnabled = false) }
            } else {
                // Apply the selected profile
                val profile = _state.value.profiles.find { it.id == profileId }
                if (profile != null) {
                    val result = equalizerService.applyProfile(profile)
                    result.onSuccess {
                        eqProfileRepository.setActiveProfile(profileId)
                        _state.update { it.copy(eqEnabled = false) }
                    }.onFailure { e ->
                        _state.update { it.copy(error = e.message ?: "Unknown error") }
                    }
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Delete an EQ profile
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            eqProfileRepository.deleteProfile(profileId)
        }
    }

    /**
     * Import a custom EQ profile from a file
     */
    fun importCustomProfile(
        fileName: String,
        inputStream: InputStream,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Read the file content
                val content = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                // Parse the ParametricEQ format
                val parametricEQ = ParametricEQParser.parseText(content)

                // Validate the parsed EQ
                val validationErrors = ParametricEQParser.validate(parametricEQ)
                if (validationErrors.isNotEmpty()) {
                    onError(Exception("Invalid EQ file: ${validationErrors.first()}"))
                    return@launch
                }

                // Extract profile name from file name (remove .txt extension)
                val profileName = fileName.removeSuffix(".txt")

                // Import the profile
                eqProfileRepository.importCustomProfile(profileName, parametricEQ)

                _state.update { it.copy(importStatus = "Successfully imported $profileName") }
                onSuccess()
            } catch (e: Exception) {
                onError(Exception("Failed to import EQ profile: ${e.message}"))
            }
        }
    }
}