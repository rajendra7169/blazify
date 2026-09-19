package com.blazify.music.utils

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenBrainzTest {
    @Test
    fun `a finished listen carries when it started`() {
        val listen =
            ListenBrainz
                .payload(
                    listenType = "single",
                    artist = "Tame Impala",
                    track = "Loser",
                    album = "Deadbeat",
                    durationSeconds = 223,
                    videoId = "abc123",
                    listenedAt = 1_700_000_000,
                ).jsonObject

        assertEquals("single", listen["listen_type"]?.jsonPrimitive?.content)
        val entry = listen["payload"]!!.jsonArray[0].jsonObject
        assertEquals("1700000000", entry["listened_at"]?.jsonPrimitive?.content)

        val metadata = entry["track_metadata"]!!.jsonObject
        assertEquals("Tame Impala", metadata["artist_name"]?.jsonPrimitive?.content)
        assertEquals("Loser", metadata["track_name"]?.jsonPrimitive?.content)
        assertEquals("Deadbeat", metadata["release_name"]?.jsonPrimitive?.content)

        val extra = metadata["additional_info"]!!.jsonObject
        assertEquals("Blazify", extra["media_player"]?.jsonPrimitive?.content)
        assertEquals("223", extra["duration"]?.jsonPrimitive?.content)
        assertEquals("music.youtube.com", extra["music_service"]?.jsonPrimitive?.content)
        assertEquals(
            "https://music.youtube.com/watch?v=abc123",
            extra["origin_url"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun `what is on now has no start time`() {
        val entry =
            ListenBrainz
                .payload(
                    listenType = "playing_now",
                    artist = "Tame Impala",
                    track = "Loser",
                    album = null,
                    durationSeconds = 223,
                    videoId = null,
                    listenedAt = null,
                ).jsonObject["payload"]!!
                .jsonArray[0]
                .jsonObject

        assertNull(entry["listened_at"])
        val metadata = entry["track_metadata"]!!.jsonObject
        assertNull(metadata["release_name"])
        assertNull(metadata["additional_info"]!!.jsonObject["origin_url"])
    }

    @Test
    fun `leaves out a length it does not know`() {
        val extra =
            ListenBrainz
                .payload("single", "A", "B", null, 0, null, 1L)
                .jsonObject["payload"]!!
                .jsonArray[0]
                .jsonObject["track_metadata"]!!
                .jsonObject["additional_info"]!!
                .jsonObject

        assertNull(extra["duration"])
    }

    @Test
    fun `a nameless play is not worth sending`() {
        assertTrue(ListenBrainz.worthSending("Tame Impala", "Loser"))
        assertFalse(ListenBrainz.worthSending("", "Loser"))
        assertFalse(ListenBrainz.worthSending("Tame Impala", "  "))
    }
}
