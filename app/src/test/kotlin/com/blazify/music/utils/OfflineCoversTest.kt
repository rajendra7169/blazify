package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OfflineCoversTest {
    @Test
    fun `every size of a cover is the same saved picture`() {
        val base = "https://lh3.googleusercontent.com/abcDEF123"
        assertEquals(OfflineCovers.key("$base=w60-h60-l90-rj"), OfflineCovers.key("$base=w544-h544-p-l90-rj"))
        assertEquals(OfflineCovers.key("https://yt3.ggpht.com/xyz=s88"), OfflineCovers.key("https://yt3.ggpht.com/xyz=s88-s544"))
    }

    @Test
    fun `a video picture is known by its address without the query`() {
        assertEquals(
            OfflineCovers.key("https://i.ytimg.com/vi/abc/hqdefault.jpg"),
            OfflineCovers.key("https://i.ytimg.com/vi/abc/hqdefault.jpg?sqp=xyz&rs=123"),
        )
    }

    @Test
    fun `different covers stay apart`() {
        assertNotEquals(
            OfflineCovers.key("https://lh3.googleusercontent.com/one=w544-h544"),
            OfflineCovers.key("https://lh3.googleusercontent.com/two=w544-h544"),
        )
    }
}
