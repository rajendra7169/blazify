package com.blazify.music.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PlaybackErrorReportTest {

    private fun report(error: Throwable) = playbackErrorReport(
        error = error,
        errorCodeName = "IO_BAD_HTTP_STATUS",
        errorCode = 2004,
        timestampMs = 0L,
        app = "9.13.3 (156) gms, universal",
        android = "16 (API 36)",
        device = "Xiaomi Redmi Note 8",
        song = "Shape of You - Ed Sheeran (JGwWNGJdvx8)",
        streamClient = "WEB_REMIX",
    )

    @Test
    fun `the report says what failed, on which song and phone`() {
        val text = report(RuntimeException("Source error", IOException("Response code: 403")))

        assertTrue(text.startsWith("Blazify playback report"))
        assertTrue("Song: Shape of You - Ed Sheeran (JGwWNGJdvx8)" in text)
        assertTrue("Stream: WEB_REMIX" in text)
        assertTrue("Error: IO_BAD_HTTP_STATUS (2004)" in text)
        assertTrue("- RuntimeException: Source error" in text)
        assertTrue("- IOException: Response code: 403" in text)
    }

    @Test
    fun `stream links keep only their host`() {
        val link = "https://rr3---sn-abc.googlevideo.com/videoplayback?expire=1&sig=SECRET&n=abc"
        val text = report(IOException("Unable to connect to $link"))

        assertFalse("SECRET" in text)
        assertTrue("rr3---sn-abc.googlevideo.com/…" in text)
        assertEquals("go to example.com/…", withoutLinkDetails("go to https://example.com/a/b?c=d"))
    }

    @Test
    fun `unknown song and stream are said plainly`() {
        val text = playbackErrorReport(
            error = IOException("boom"),
            errorCodeName = "IO_UNSPECIFIED",
            errorCode = 2000,
            timestampMs = 0L,
            app = "a",
            android = "b",
            device = "c",
            song = null,
            streamClient = null,
        )

        assertTrue("Song: unknown" in text)
        assertTrue("Stream: unknown" in text)
    }
}
