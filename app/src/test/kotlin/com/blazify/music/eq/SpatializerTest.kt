package com.blazify.music.eq

import com.blazify.music.eq.audio.SpatialMode
import com.blazify.music.eq.audio.Spatializer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Test
import org.junit.Assert.assertTrue

class SpatializerTest {
    private fun level(x: FloatArray, channel: Int) = sqrt(x.filterIndexed { i, _ -> i % 2 == channel }.sumOf { (it * it).toDouble() } / (x.size / 2))

    @Test
    fun leftOnlySoundStaysLeftButReachesTheOtherEar() {
        val rate = 48_000
        val frames = rate
        val input = FloatArray(frames * 2) { if (it % 2 == 0) (0.5 * sin(2 * PI * 440 * (it / 2) / rate)).toFloat() else 0f }
        val output = FloatArray(frames * 2)
        Spatializer(rate).process(input, output, frames)
        assertTrue(output.all { it.isFinite() && abs(it) <= 1f })
        val l = level(output, 0)
        val r = level(output, 1)
        // A speaker on the left is heard by both ears, the left more.
        assertTrue("left $l right $r", l > r * 1.3)
        assertTrue("left $l right $r", r > l * 0.2)
    }

    @Test
    fun orbitMovesTheSound() {
        val rate = 48_000
        val spatial = Spatializer(rate).also { it.mode = SpatialMode.Orbit; it.orbitSeconds = 4.0 }
        val quarter = rate // a quarter of the way round each second
        // Full-range noise, like music: the head hides the treble from the far ear but lets the bass round.
        val random = kotlin.random.Random(3)
        val tone = { _: Int -> (random.nextDouble() * 0.6 - 0.3).toFloat() }
        val levels = (0 until 4).map { q ->
            val input = FloatArray(quarter * 2) { tone(q * quarter + it / 2) }
            val output = FloatArray(quarter * 2)
            spatial.process(input, output, quarter)
            level(output, 1) / level(output, 0)
        }
        // Right-heavy while it is on the right (the first half round), left-heavy on the way back.
        assertTrue(levels.toString(), levels[0] > 1.1 && levels[1] > 1.1 && levels[2] < 0.9 && levels[3] < 0.9)
    }
}
