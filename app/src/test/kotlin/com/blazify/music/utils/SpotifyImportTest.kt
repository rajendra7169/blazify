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

    @Test
    fun `turns down another song of the same length`() {
        val results = listOf(song("other", 200, title = "Let It Happen", artist = "Tame Impala"))

        assertNull(SpotifyImport.pickMatch(results, track(seconds = 200)))
    }

    @Test
    fun `turns down the same title by somebody else`() {
        val results = listOf(song("cover", 200, artist = "Some Cover Band"), song("right", 203, artist = "Tame Impala"))

        assertEquals("right", SpotifyImport.pickMatch(results, track(seconds = 200))?.id)
    }

    @Test
    fun `sees past featured artists and remaster notes`() {
        val track = SpotifyPlaylist.Track(title = "Despacito - Remix (feat. Justin Bieber)", artists = "Luis Fonsi, Daddy Yankee, Justin Bieber", durationSeconds = 229)
        val results = listOf(song("right", 230, title = "Despacito (Remix)", artist = "Luis Fonsi"))

        assertEquals("right", SpotifyImport.pickMatch(results, track)?.id)
        assertEquals("let it be", SpotifyImport.normalize("Let It Be - Remastered 2009"))
    }

    @Test
    fun `allows a longer cut of the very same song`() {
        val track = SpotifyPlaylist.Track(title = "Livin' On The Edge", artists = "Aerosmith", durationSeconds = 367)
        val results = listOf(song("album cut", 381, title = "Livin' On The Edge", artist = "Aerosmith"))

        assertEquals("album cut", SpotifyImport.pickMatch(results, track)?.id)
        assertNull(SpotifyImport.pickMatch(listOf(song("unnamed", 381, title = "Livin' On The Edge")), track))
    }

    @Test
    fun `searches for the plain title`() {
        assertEquals("Live And Let Die", SpotifyImport.searchTitle("Live And Let Die - 2022 Remaster"))
        assertEquals("Axel F", SpotifyImport.searchTitle("Axel F - From \"Beverly Hills Cop\" Soundtrack"))
        assertEquals("Despacito", SpotifyImport.searchTitle("Despacito (feat. Justin Bieber)"))
        assertEquals("Back In Black", SpotifyImport.searchTitle("Back In Black"))
    }

    @Test
    fun `prefers the original over a re-recording`() {
        val track = SpotifyPlaylist.Track(title = "We Built This City", artists = "Starship", durationSeconds = 293)
        val results = listOf(
            song("re-recorded", 293, title = "We Built This City (Re-Recorded Version)", artist = "Starship"),
            song("original", 296, title = "We Built This City", artist = "Starship"),
        )

        assertEquals("original", SpotifyImport.pickMatch(results, track)?.id)
    }

    @Test
    fun `a longer title is another song unless it only names a version`() {
        val track = SpotifyPlaylist.Track(title = "Ghostbusters", artists = "Ray Parker Jr.", durationSeconds = 240)

        assertNull(SpotifyImport.pickMatch(listOf(song("other", 240, title = "Ghostbusters Afterlife Theme")), track))
    }

    private fun track(seconds: Int) = SpotifyPlaylist.Track(title = "Loser", artists = "Tame Impala", durationSeconds = seconds)

    private fun song(id: String, duration: Int?, title: String = "Loser", artist: String? = null) =
        SongItem(
            id = id,
            title = title,
            artists = listOfNotNull(artist?.let { com.blazify.innertube.models.Artist(name = it, id = null) }),
            duration = duration,
            thumbnail = "",
        )
}
