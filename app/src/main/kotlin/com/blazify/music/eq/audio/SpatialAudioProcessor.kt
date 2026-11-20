/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The 3D sound choices: off, Surround (speakers round you) and 8D (the song circling your head). */
enum class SpatialSound { OFF, SURROUND, ORBIT }

/**
 * 3D sound in the player: stereo 16-bit sound goes through [Spatializer], placed round the listener's head. Off, it
 * passes the sound through untouched; mono and other formats are left alone. The choice can change while a song
 * plays, from the settings, without a stop.
 */
@UnstableApi
class SpatialAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var sound: SpatialSound = SpatialSound.OFF

    private var spatializer: Spatializer? = null
    private var input = FloatArray(0)
    private var output = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        spatializer = Spatializer(inputAudioFormat.sampleRate)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val out = replaceOutputBuffer(bytes)
        val chosen = sound
        val engine = spatializer
        if (chosen == SpatialSound.OFF || engine == null) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        engine.mode = if (chosen == SpatialSound.ORBIT) SpatialMode.Orbit else SpatialMode.Speakers
        val samples = bytes / 2
        val frames = samples / 2
        if (input.size < samples) {
            input = FloatArray(samples)
            output = FloatArray(samples)
        }
        val shorts = inputBuffer.order(ByteOrder.nativeOrder())
        for (i in 0 until samples) input[i] = shorts.short / 32768f
        engine.process(input, output, frames)
        out.order(ByteOrder.nativeOrder())
        for (i in 0 until samples) {
            val v = (output[i] * 32767f).coerceIn(-32768f, 32767f)
            out.putShort(v.toInt().toShort())
        }
        out.flip()
    }

    override fun onReset() {
        spatializer = null
    }
}
