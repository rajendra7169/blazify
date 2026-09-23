package com.blazify.music.utils

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class PlaylistLinkTest {
    private val ids = listOf("JGwWNGJdvx8", "NJAv_7lHUIU", "tvTRZJ-4EyI")

    @Test
    fun `a shared playlist comes back as it went in`() {
        val link = PlaylistLink.build("Raja's road trip 🎵", ids)
        val shared = PlaylistLink.parse(Uri.parse(link))
        assertEquals("Raja's road trip 🎵", shared?.name)
        assertEquals(ids, shared?.songIds)
    }

    @Test
    fun `the songs are not in the part a browser sends to the site`() {
        val link = PlaylistLink.build("Trip", ids)
        assertEquals(ids.joinToString(""), link.substringAfter('#').substringAfterLast('.'))
        assertEquals(false, link.substringBefore('#').contains(ids.first()))
    }

    @Test
    fun `the app's own link opens too`() {
        val link = PlaylistLink.build("Trip", ids)
        val appLink = "blazify://playlist#" + link.substringAfter('#')
        assertEquals(ids, PlaylistLink.parse(Uri.parse(appLink))?.songIds)
    }

    @Test
    fun `a very long playlist is cut to what a link can hold`() {
        val many = List(400) { "JGwWNGJdvx8" }
        assertEquals(PlaylistLink.MAX_SONGS, PlaylistLink.parse(Uri.parse(PlaylistLink.build("Long", many)))?.songIds?.size)
    }

    @Test
    fun `links that are not ours are left alone`() {
        listOf(
            "https://music.youtube.com/playlist?list=RDCLAK5uy",
            "https://rajendra7169.github.io/blazify/listen#abc",
            "https://rajendra7169.github.io/blazify/playlist",
            "https://rajendra7169.github.io/blazify/playlist#2.abc.def",
        ).forEach { assertNull(it, PlaylistLink.parse(Uri.parse(it))) }
    }
}
