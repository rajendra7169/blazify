package com.blazify.music.playback

import androidx.media3.common.Timeline
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import org.junit.Assert.assertFalse
import org.junit.Test

class BufferAheadTest {
    private fun whenBuffered(seconds: Long) =
        LoadControl.Parameters(
            PlayerId.UNSET,
            Timeline.EMPTY,
            MediaSource.MediaPeriodId(Any()),
            /* playbackPositionUs = */ 0,
            /* bufferedDurationUs = */ seconds * 1_000_000,
            /* playbackSpeed = */ 1f,
            /* playWhenReady = */ true,
            /* rebuffering = */ false,
            /* targetLiveOffsetUs = */ 0,
            /* lastRebufferRealtimeMs = */ 0,
        )

    @Test
    fun `on mobile data it stops fetching once a minute is held`() {
        val control = BufferAhead { true }

        assertFalse(control.shouldContinueLoading(whenBuffered(50)))
        assertFalse(control.shouldContinueLoading(whenBuffered(240)))
    }
}
