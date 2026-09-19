package com.blazify.music.utils

import com.blazify.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyImportTest {
    @Test
    fun `takes the result whose length matches`() {
        val track = track(seconds = 223)
        val results =
            listOf(
                song("wrong-length", 180),
                song("right", 225),
                song("live-version", 402),
            )

        assertEquals("right", SpotifyImport.pickMatch(results, track)?.id)
    }

    @Test
    fun `takes the closest when several are near enough`() {
        val track = track(seconds = 200)
        val results = listOf(song("near", 205), song("nearer", 201), song("far", 240))

        assertEquals("nearer", SpotifyImport.pickMatch(results, track)?.id)
    }

    @Test
    fun `finds nothing rather than the wrong song`() {
        val track = track(seconds = 200)

        assertNull(SpotifyImport.pickMatch(listOf(song("cover", 140), song("extended", 400)), track))
        assertNull(SpotifyImport.pickMatch(emptyList(), track))
    }

    @Test
    fun `matches on names when there is no length to compare`() {
        val results = listOf(song("first", 300), song("second", 200))

        assertEquals("first", SpotifyImport.pickMatch(results, track(seconds = 0))?.id)
    }

    @Test
    fun `ignores results that carry no length`() {
        val results = listOf(song("lengthless", null), song("right", 198))

        assertEquals("right", SpotifyImport.pickMatch(results, track(seconds = 200))?.id)
    }

    private fun track(seconds: Int) = SpotifyPlaylist.Track(title = "Loser", artists = "Tame Impala", durationSeconds = seconds)

    private fun song(id: String, duration: Int?) =
        SongItem(
            id = id,
            title = "Loser",
            artists = emptyList(),
            duration = duration,
            thumbnail = "",
        )
}
