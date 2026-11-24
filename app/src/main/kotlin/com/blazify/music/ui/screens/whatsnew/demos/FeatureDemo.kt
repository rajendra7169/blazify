/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.whatsnew.demos

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.blazify.music.whatsnew.DemoId

/** The animation for [demo]. [playing] is false for cards that should hold their still frame. */
@Composable
fun FeatureDemo(
    demo: DemoId,
    @DrawableRes icon: Int,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    // The card's own words say what the demo shows, so TalkBack reads those instead.
    val quiet = modifier.clearAndSetSemantics {}
    when (demo) {
        DemoId.SLEEP_TIMER -> SleepTimerDemo(playing, quiet)
        DemoId.DOWNLOAD -> DownloadDemo(playing, quiet)
        DemoId.RECOGNITION -> RecognitionDemo(playing, quiet)
        DemoId.SEARCH_OFFLINE -> SearchOfflineDemo(playing, quiet)
        DemoId.SPATIAL_SOUND -> SpatialSoundDemo(playing, quiet)
        DemoId.QUEUE_SWIPE -> QueueSwipeDemo(playing, quiet)
        DemoId.LYRICS -> LyricsDemo(playing, quiet)
        DemoId.EQUALIZER -> EqualizerDemo(playing, quiet)
        DemoId.TRENDING -> TrendingDemo(playing, quiet)
        DemoId.KEEPS_PLAYING -> KeepsPlayingDemo(playing, quiet)
        DemoId.ACCOUNTS -> AccountsDemo(playing, quiet)
        DemoId.PLAYER_DESIGNS -> PlayerDesignsDemo(playing, quiet)
        DemoId.TOGETHER -> TogetherDemo(playing, quiet)
        DemoId.LOCAL_MUSIC -> LocalMusicDemo(playing, quiet)
        DemoId.LOOK_AND_FEEL -> LookAndFeelDemo(playing, quiet)
        DemoId.VIDEO_ART -> VideoArtDemo(playing, quiet)
        DemoId.CAST -> CastDemo(playing, quiet)
        DemoId.TAP_ICON -> TapIconDemo(icon, playing, quiet)
    }
}
