package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPlaylistTest {
    @Test
    fun `reads playlist and album links`() {
        assertEquals("playlist" to "37i9dQZF1DXcBWIGoYBM5M", SpotifyPlaylist.parseLink("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123"))
        assertEquals("album" to "1ATL5GLyefJaxhQzSPVrLX", SpotifyPlaylist.parseLink("https://open.spotify.com/album/1ATL5GLyefJaxhQzSPVrLX"))
        assertEquals("playlist" to "37i9dQZF1DXcBWIGoYBM5M", SpotifyPlaylist.parseLink("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M"))
        // Spotify puts the language in front of the kind when the link is shared from a localised page.
        assertEquals("playlist" to "37i9dQZF1DXcBWIGoYBM5M", SpotifyPlaylist.parseLink("https://open.spotify.com/intl-pt/playlist/37i9dQZF1DXcBWIGoYBM5M"))
    }

    @Test
    fun `refuses what it cannot import`() {
        assertNull(SpotifyPlaylist.parseLink("https://open.spotify.com/track/2FZcjBYK4dTt48q94pJbJD"))
        assertNull(SpotifyPlaylist.parseLink("https://music.youtube.com/playlist?list=PL123"))
        assertNull(SpotifyPlaylist.parseLink("just some words"))
    }

    @Test
    fun `reads the name and tracks off a share page`() {
        val playlist = SpotifyPlaylist.parse(page(TWO_TRACKS)).getOrThrow()

        assertEquals("Late night", playlist.name)
        assertEquals(2, playlist.tracks.size)
        assertEquals("Loser", playlist.tracks[0].title)
        assertEquals("Tame Impala", playlist.tracks[0].artists)
        assertEquals(223, playlist.tracks[0].durationSeconds)
        assertEquals("KAROL G, Judeline", playlist.tracks[1].artists)
        assertFalse(playlist.mayHaveMore)
    }

    @Test
    fun `says when the page was cut short`() {
        val hundred = (1..100).joinToString(",") { """{"title":"Song $it","subtitle":"Somebody","duration":180000}""" }
        val playlist = SpotifyPlaylist.parse(page(hundred)).getOrThrow()

        assertEquals(100, playlist.tracks.size)
        assertTrue(playlist.mayHaveMore)
    }

    @Test
    fun `fails on a page with no track list`() {
        assertTrue(SpotifyPlaylist.parse("<html><body>nothing here</body></html>").isFailure)
        assertTrue(SpotifyPlaylist.parse(page("")).isFailure)
    }

    private fun page(tracks: String) =
        """
        <html><head><script id="__NEXT_DATA__" type="application/json">
        {"props":{"pageProps":{"state":{"data":{"entity":{"name":"Late night","trackList":[$tracks]}}}}}}
        </script></head><body></body></html>
        """.trimIndent()

    private companion object {
        const val TWO_TRACKS =
            """{"title":"Loser","subtitle":"Tame Impala","duration":223069},""" +
                """{"title":"BbY WOW","subtitle":"KAROL G, Judeline","duration":225834}"""
    }
}
