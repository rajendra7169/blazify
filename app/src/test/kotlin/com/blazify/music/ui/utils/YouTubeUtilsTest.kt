package com.blazify.music.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeUtilsTest {

    @Test
    fun `lh3 googleusercontent is rewritten to the requested size`() {
        val url = "https://lh3.googleusercontent.com/abc=w120-h120-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/abc=w544-h544-p-l90-rj",
            url.resize(544, 544),
        )
    }

    @Test
    fun `yt3 googleusercontent is rewritten too (YouTube migrated host - fixes the blurry player)`() {
        // YT moved album art to yt3.googleusercontent.com and serves a ~60px thumbnail; without
        // matching this host, resize() would no-op and the player upscales the 60px image → blur.
        val url = "https://yt3.googleusercontent.com/_zDuDZFnSKmuQwDX=w60-h60-l90-rj"
        assertEquals(
            "https://yt3.googleusercontent.com/_zDuDZFnSKmuQwDX=w544-h544-p-l90-rj",
            url.resize(544, 544),
        )
    }

    @Test
    fun `an i_ytimg url is left as it is`() {
        // maxresdefault does not exist for every video, and asking for one that is not there
        // leaves a blank square where the artwork should be. The size YouTube gave is kept.
        val url = "https://i.ytimg.com/vi/abc/hqdefault.jpg?sqp=-oaymwE"
        assertEquals(url, url.resize(544, 544))
    }

    @Test
    fun `null dimensions return the url unchanged`() {
        val url = "https://yt3.googleusercontent.com/abc=w60-h60-l90-rj"
        assertEquals(url, url.resize())
    }

    @Test
    fun `a ggpht avatar is asked for at the size wanted`() {
        val url = "https://yt3.ggpht.com/abc=s88"
        assertEquals("https://yt3.ggpht.com/abc=s88-s544", url.resize(544, 544))
    }
}
