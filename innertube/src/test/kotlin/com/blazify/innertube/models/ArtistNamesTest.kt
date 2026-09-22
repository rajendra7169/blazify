package com.blazify.innertube.models

import com.blazify.innertube.pages.PageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Artist lines as YouTube Music sends them (captured 2026-09-22), and the names read from them.
 */
class ArtistNamesTest {
    private fun name(text: String) = Run(text, null)

    private fun artist(text: String, id: String) =
        Run(text, NavigationEndpoint(browseEndpoint = BrowseEndpoint(browseId = id)))

    // "Aahun Aahun": Pritam, Neeraj Shridhar, Master Saleem & Suzi Q • Love Aaj Kal • 2009
    private val aahunAahun =
        listOf(
            artist("Pritam", "UCCTN01plFzn4npREHKT2_9Q"),
            name(", "),
            artist("Neeraj Shridhar", "UCjK4uCLdPYnIatZFB9Nvbrg"),
            name(", "),
            artist("Master Saleem", "UC-y6G86uA_xpTCdBB5xBBYg"),
            name(" & "),
            name("Suzi Q"),
            name(" • "),
            artist("Love Aaj Kal (Original Motion Picture Soundtrack)", "MPREb_9d9DnxMcqFu"),
            name(" • "),
            name("2009"),
        )

    @Test
    fun `a queue line gives the four artists`() {
        val names = aahunAahun.splitBySeparator().first().oddElements().map { it.text }
        assertEquals(listOf("Pritam", "Neeraj Shridhar", "Master Saleem", "Suzi Q"), names)
    }

    @Test
    fun `a playlist line gives the artists, not the commas and the ampersand between them`() {
        val names = PageHelper.extractArtists(aahunAahun.splitBySeparator().first()).map { it.name }
        assertEquals(listOf("Pritam", "Neeraj Shridhar", "Master Saleem", "Suzi Q"), names)
    }

    @Test
    fun `a song under a top search result has no artist, not an artist called 5 00`() {
        // "Zaalima" under the Arijit Singh card: the line is only "Song • 5:00".
        val line = listOf(name("Song"), name(" • "), name("5:00")).splitBySeparator()
        assertTrue(line.getOrNull(1)!!.oddElements().isEmpty())
    }

    @Test
    fun `a search song line with only a length gives no artist`() {
        // "Main Na Raha Mera" under the Arijit Singh card, read the way search songs are.
        val line = listOf(name("Song"), name(" • "), name("4:41")).splitBySeparator().clean()
        assertTrue(line.first().splitArtistsByConjunction().isEmpty())
    }

    @Test
    fun `a search song line keeps its artists and drops what is between them`() {
        val line = listOf(artist("Sonu Nigam", "UCsC4u-BJAd4OX1hJXtwXSOQ"), name(", "), name("Anu Malik & Javed Akhtar"))
        assertEquals(listOf("Sonu Nigam", "Anu Malik", "Javed Akhtar"), line.splitArtistsByConjunction().map { it.text })
    }

    @Test
    fun `a year after an album is not an artist`() {
        // "Arijit Forever": "Album • 2026"
        val line = listOf(name("Album"), name(" • "), name("2026")).splitBySeparator()
        assertTrue(line.getOrNull(1)!!.oddElements().isEmpty())
    }

    @Test
    fun `what sits between or after names is not a name`() {
        listOf(",", ", ", "&", " & ", "•", "and", "And", "4:41", "2026", "  ").forEach {
            assertFalse("\"$it\" read as a name", looksLikeArtistName(it))
        }
    }

    @Test
    fun `names in any script are names`() {
        listOf("Arijit Singh", "सोनू निगम", "宇多田ヒカル", "Sonu Nigam | Anuradha Paudwal", "50 Cent", "2Pac").forEach {
            assertTrue("\"$it\" not read as a name", looksLikeArtistName(it))
        }
    }

    @Test
    fun `a band whose name has no letters still counts when it links to its page`() {
        assertTrue(artist("112", "UCxxxxxxxxxxxxxxxxxxxxxx").isArtistName())
        assertFalse(name("112").isArtistName())
    }
}
