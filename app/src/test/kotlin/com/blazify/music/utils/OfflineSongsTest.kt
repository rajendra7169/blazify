package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSongsTest {
    private data class Track(val title: String, val artist: String, val album: String? = null)

    private fun search(query: String, tracks: List<Track>) =
        searchOffline(query, tracks, title = { it.title }, others = { listOf(it.artist, it.album) })

    private val tracks =
        listOf(
            Track("Tum Hi Ho", "Arijit Singh", "Aashiqui 2"),
            Track("Halo", "Beyoncé", "I Am... Sasha Fierce"),
            Track("Kesariya", "Arijit Singh", "Brahmastra"),
            Track("Arijit Medley", "Various"),
            Track("Resham Firiri", "Nepali Folk"),
        )

    @Test
    fun `finds by title, artist or album, ignoring case and accents`() {
        assertEquals(listOf("Halo"), search("HALO", tracks).map { it.title })
        assertEquals(listOf("Halo"), search("beyonce", tracks).map { it.title })
        assertEquals(listOf("Kesariya"), search("brahmastra", tracks).map { it.title })
    }

    @Test
    fun `every word has to be found somewhere`() {
        assertEquals(listOf("Kesariya"), search("arijit kesariya", tracks).map { it.title })
        assertTrue(search("arijit halo", tracks).isEmpty())
    }

    @Test
    fun `titles that start with what was typed come first`() {
        assertEquals(
            listOf("Arijit Medley", "Tum Hi Ho", "Kesariya"),
            search("arijit", tracks).map { it.title },
        )
    }

    @Test
    fun `an empty box finds nothing`() {
        assertTrue(search("   ", tracks).isEmpty())
    }

    @Test
    fun `a song is kept when any place it is kept is switched on`() {
        val both = setOf(OfflineSource.CACHED, OfflineSource.DOWNLOADED)
        assertTrue(OfflineKinds().allows(both))
        assertTrue(OfflineKinds(cached = false).allows(both))
        assertFalse(OfflineKinds(cached = false, downloaded = false).allows(both))
        assertFalse(OfflineKinds(device = false).allows(setOf(OfflineSource.DEVICE)))
        assertTrue(OfflineKinds(cached = false, downloaded = false).allows(setOf(OfflineSource.DEVICE)))
    }
}
