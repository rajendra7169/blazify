package com.blazify.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.blazify.music.eq.data.ParametricEQ
import com.blazify.music.eq.data.ParametricEQBand
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * Custom audio processor for ExoPlayer that applies parametric EQ using biquad filters
 * Uses ParametricEQ format from AutoEQ project
 */
@UnstableApi
@SuppressWarnings("Deprecated")
class CustomEqualizerAudioProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false
    private var equalizerEnabled = false

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    // Read on the audio thread, replaced from the UI thread. Volatile so the
    // swap is published whole: without it the audio thread can be handed a list
    // that is still being built, and process a song through half an equaliser.
    @Volatile
    private var filters: List<BiquadFilter> = emptyList()

    @Volatile
    private var preampGain: Double = 1.0  // Linear preamp gain multiplier

    /**
     * The profile in force, kept for as long as it is in force.
     *
     * Biquad coefficients are computed for one sample rate. When the player
     * reconfigures — a new track at 44.1kHz after one at 48kHz — every filter
     * has to be built again for the new rate, and that needs the profile, not
     * just a note that one was applied once. Keeping it only until the first
     * configure is why the sound could collapse to bass and stay there until
     * the app was restarted: the old filters were still in place, designed for
     * a rate the audio no longer had.
     */
    private var currentProfile: ParametricEQ? = null

    companion object {
        private const val TAG = "CustomEqualizerAudioProcessor"
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    /**
     * Apply an EQ profile
     */
    @Synchronized
    fun applyProfile(parametricEQ: ParametricEQ) {
        currentProfile = parametricEQ
        equalizerEnabled = true

        if (sampleRate == 0) {
            // Nothing to build against yet. It will be built in configure().
            Timber.tag(TAG)
                .d("Not configured yet; holding profile with ${parametricEQ.bands.size} bands")
            return
        }

        // Convert preamp from dB to linear gain
        preampGain = 10.0.pow(parametricEQ.preamp / 20.0)

        // Built whole, then swapped in one assignment. The old filters keep
        // working right up to that moment, so a slider being dragged changes
        // the sound rather than interrupting it.
        //
        // Their state is deliberately NOT reset. Zeroing a filter's memory
        // mid-song is a discontinuity, and a discontinuity is a click — which
        // is what made every adjustment audible as a fault rather than as a
        // change.
        createFilters(parametricEQ.bands)

        Timber.tag(TAG)
            .d("Applied EQ profile with ${filters.size} bands and ${parametricEQ.preamp} dB preamp")
    }

    /**
     * Disable the equalizer
     */
    @Synchronized
    fun disable() {
        equalizerEnabled = false
        filters = emptyList()
        preampGain = 1.0
        currentProfile = null
        Timber.tag(TAG).d("Equalizer disabled")
    }

    /**
     * Check if equalizer is enabled
     */
    fun isEnabled(): Boolean = equalizerEnabled

    /**
     * Create biquad filters from ParametricEQ bands
     * Only creates filters for enabled bands below Nyquist frequency
     * Supports PK (peaking), LSC (low-shelf), and HSC (high-shelf) filter types
     */
    private fun createFilters(bands: List<ParametricEQBand>) {
        if (sampleRate == 0) {
            Timber.tag(TAG).w("Cannot create filters: sample rate not set")
            return
        }

        // Filter out disabled bands and frequencies above Nyquist limit
        filters = bands
            .filter { it.enabled && it.frequency < sampleRate / 2.0 }
            .map { band ->
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = band.frequency,
                    gain = band.gain,
                    q = band.q,
                    filterType = band.filterType
                )
            }

        Timber.tag(TAG)
            .d("Created ${filters.size} biquad filters from ${bands.size} bands (PK/LSC/HSC)")
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val rateChanged = sampleRate != inputAudioFormat.sampleRate
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        Timber.tag(TAG)
            .d("Configured: sampleRate=$sampleRate, channels=$channelCount, encoding=$encoding")

        // Always rebuilt for the rate now in play, every time, rather than only
        // the first time. A filter designed for one rate applied to another has
        // every centre frequency in the wrong place — which is heard as the
        // whole equaliser collapsing towards the bass.
        currentProfile?.let { profile ->
            if (rateChanged || filters.isEmpty()) {
                preampGain = 10.0.pow(profile.preamp / 20.0)
                createFilters(profile.bands)
                Timber.tag(TAG)
                    .d("Rebuilt ${filters.size} filters for ${sampleRate}Hz")
            }
        }

        // Only support 16-bit PCM stereo/mono
        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            val exception = AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            throw exception // Rethrow, unsupported
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!equalizerEnabled || filters.isEmpty()) {
            // Passthrough mode - directly use input as output
            val remaining = inputBuffer.remaining()
            if (remaining == 0) return

            // Ensure output buffer is large enough
            if (outputBuffer.capacity() < remaining) {
                outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
            } else {
                outputBuffer.clear()
            }
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) {
            return
        }

        // Ensure we have our own output buffer (reuse if possible to avoid allocations)
        // Note: We MUST NOT use inputBuffer as outputBuffer if we modify it
        if (outputBuffer === EMPTY_BUFFER || outputBuffer === inputBuffer) {
            // Need new buffer - was empty or same as input
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else if (outputBuffer.capacity() < inputSize) {
            // Need larger buffer
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            // Reuse existing buffer (most common path)
            outputBuffer.clear()
        }

        // Process audio samples
        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                // Ensure the output buffer is ready to receive data
                // We don't set limit() here because putShort will advance position
                processAudioBuffer16Bit(inputBuffer, outputBuffer)
            }
            else -> {
                // Unsupported format, passthrough
                outputBuffer.put(inputBuffer)
            }
        }

        outputBuffer.flip()
        // inputBuffer position is already updated by processAudioBuffer16Bit/put
    }

    /**
     * Process 16-bit PCM audio through all biquad filters
     */
    private fun processAudioBuffer16Bit(input: ByteBuffer, output: ByteBuffer) {
        // Ensure we are reading from the current position
        // Input is ready to be read from position() to limit()
        // Output is ready to be written to from position()

        val sampleCount = input.remaining() / 2 // 2 bytes per 16-bit sample

        repeat(sampleCount / channelCount) {
            when (channelCount) {
                1 -> {
                    // Mono
                    val sample = input.getShort().toDouble() / 32768.0 // Normalize to [-1, 1]
                    var processed = sample

                    // Apply all filters in series
                    for (filter in filters) {
                        processed = filter.processSample(processed)
                    }

                    // Apply preamp gain
                    processed *= preampGain

                    // Clamp and convert back to 16-bit
                    val outputSample = (processed * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    output.putShort(outputSample)
                }
                2 -> {
                    // Stereo
                    val leftSample = input.getShort().toDouble() / 32768.0
                    val rightSample = input.getShort().toDouble() / 32768.0

                    var processedLeft = leftSample
                    var processedRight = rightSample

                    // Apply all filters in series
                    for (filter in filters) {
                        val (left, right) = filter.processStereo(processedLeft, processedRight)
                        processedLeft = left
                        processedRight = right
                    }

                    // Apply preamp gain
                    processedLeft *= preampGain
                    processedRight *= preampGain

                    // Clamp and convert back to 16-bit
                    val outputLeft = (processedLeft * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    val outputRight = (processedRight * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()

                    output.putShort(outputLeft)
                    output.putShort(outputRight)
                }
                else -> {
                    // Should not happen as configure rejects > 2 channels
                    repeat(channelCount) {
                        output.putShort(input.getShort())
                    }
                }
            }
        }
    }

    override fun getOutput(): ByteBuffer {
        // Return output buffer ready for reading (already flipped in queueInput)
        val buffer = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer.remaining() == 0
    }

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false

        // Cleared here and only here. A flush means the audio itself jumped —
        // a seek, or a new track — so the filters' memory of the last samples
        // is genuinely worthless. Changing a band is not that, which is why
        // applyProfile leaves it alone.
        filters.forEach { it.reset() }
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        inputBuffer = EMPTY_BUFFER
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
        // Dropped, not merely quietened. Without a sample rate these filters
        // describe nothing, and leaving them in place is what let a stale set
        // survive into the next track. The profile is kept, so configure()
        // builds a correct set the moment it knows the new rate.
        filters = emptyList()
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
