/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.whatsnew

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blazify.music.BuildConfig
import com.blazify.music.R
import com.blazify.music.utils.Updater

/** Which small animation a feature card plays above its words. */
enum class DemoId {
    SLEEP_TIMER,
    DOWNLOAD,
    RECOGNITION,
    SEARCH_OFFLINE,
    SPATIAL_SOUND,
    QUEUE_SWIPE,
    LYRICS,
    EQUALIZER,
    TRENDING,
    KEEPS_PLAYING,
    ACCOUNTS,
    PLAYER_DESIGNS,
    TOGETHER,
    LOCAL_MUSIC,
    LOOK_AND_FEEL,
    VIDEO_ART,
    CAST,

    /** A finger taps the feature's own icon. Stands in until a feature has a demo of its own. */
    TAP_ICON,
}

/** What kind of news a card is: something new, something better, or something that works again. */
enum class FeatureKind {
    NEW,
    IMPROVED,
    FIXED,
}

/**
 * One feature as the What's new page shows it.
 *
 * [heading] is one string with the words to colour marked
 * `<annotation type="accent">…</annotation>`, so translations can move them.
 */
data class FeatureEntry(
    val id: String,
    @StringRes val heading: Int,
    @StringRes val description: Int,
    val demo: DemoId,
    @DrawableRes val icon: Int,
    val kind: FeatureKind = FeatureKind.NEW,
    /** Where a "Try it" button would go. Not shown yet. */
    val route: String? = null,
    /** False for a feature this build does not have, such as casting outside the gms build. */
    val available: () -> Boolean = { true },
)

data class WhatsNewRelease(
    val version: String,
    val entries: List<FeatureEntry>,
)

/**
 * Everything the What's new page can show. Next release: add one [WhatsNewRelease] at the
 * top of [releases] and its strings, nothing else.
 */
object WhatsNewCatalog {
    /** At most this many new-feature cards, however many versions were skipped. */
    const val MAX_NEW = 6

    /** How many of [coreFeatures] the page shows before "See all features". */
    const val HIGHLIGHT_COUNT = 5

    /** Newest first. The entries are a proposal for the owner to confirm. */
    val releases: List<WhatsNewRelease> =
        listOf(
            WhatsNewRelease(
                version = "9.20.0",
                entries =
                    listOf(
                        FeatureEntry(
                            id = "spatial_sound",
                            heading = R.string.whats_new_spatial_sound_title,
                            description = R.string.whats_new_spatial_sound_desc,
                            demo = DemoId.SPATIAL_SOUND,
                            icon = R.drawable.music_note,
                        ),
                        FeatureEntry(
                            id = "offline_search",
                            heading = R.string.whats_new_offline_search_title,
                            description = R.string.whats_new_offline_search_desc,
                            demo = DemoId.SEARCH_OFFLINE,
                            icon = R.drawable.search,
                        ),
                        FeatureEntry(
                            id = "trending_country",
                            heading = R.string.whats_new_trending_title,
                            description = R.string.whats_new_trending_desc,
                            demo = DemoId.TRENDING,
                            icon = R.drawable.trending_up,
                            kind = FeatureKind.IMPROVED,
                        ),
                        FeatureEntry(
                            id = "keeps_playing",
                            heading = R.string.whats_new_keeps_playing_title,
                            description = R.string.whats_new_keeps_playing_desc,
                            demo = DemoId.KEEPS_PLAYING,
                            icon = R.drawable.queue_music,
                            kind = FeatureKind.FIXED,
                        ),
                        FeatureEntry(
                            id = "video_art_finder",
                            heading = R.string.whats_new_video_art_finder_title,
                            description = R.string.whats_new_video_art_finder_desc,
                            demo = DemoId.VIDEO_ART,
                            icon = R.drawable.slow_motion_video,
                            kind = FeatureKind.FIXED,
                        ),
                    ),
            ),
            WhatsNewRelease(
                version = "9.19.0",
                entries =
                    listOf(
                        FeatureEntry(
                            id = "accounts",
                            heading = R.string.whats_new_accounts_title,
                            description = R.string.whats_new_accounts_desc,
                            demo = DemoId.ACCOUNTS,
                            icon = R.drawable.person,
                        ),
                        FeatureEntry(
                            id = "queue_swipe",
                            heading = R.string.whats_new_queue_swipe_title,
                            description = R.string.whats_new_queue_swipe_desc,
                            demo = DemoId.QUEUE_SWIPE,
                            icon = R.drawable.swipe,
                            kind = FeatureKind.IMPROVED,
                        ),
                        FeatureEntry(
                            id = "recognition_wave",
                            heading = R.string.whats_new_recognition_wave_title,
                            description = R.string.whats_new_recognition_wave_desc,
                            demo = DemoId.RECOGNITION,
                            icon = R.drawable.sound_wave,
                            kind = FeatureKind.IMPROVED,
                        ),
                    ),
            ),
        )

    /** Blazify's main features, the highlights first. */
    val coreFeatures: List<FeatureEntry> =
        listOf(
            FeatureEntry(
                id = "downloads",
                heading = R.string.whats_new_core_downloads_title,
                description = R.string.whats_new_core_downloads_desc,
                demo = DemoId.DOWNLOAD,
                icon = R.drawable.download,
            ),
            FeatureEntry(
                id = "lyrics",
                heading = R.string.whats_new_core_lyrics_title,
                description = R.string.whats_new_core_lyrics_desc,
                demo = DemoId.LYRICS,
                icon = R.drawable.lyrics,
            ),
            FeatureEntry(
                id = "player_designs",
                heading = R.string.whats_new_core_player_designs_title,
                description = R.string.whats_new_core_player_designs_desc,
                demo = DemoId.PLAYER_DESIGNS,
                icon = R.drawable.palette,
            ),
            FeatureEntry(
                id = "equalizer",
                heading = R.string.whats_new_core_equalizer_title,
                description = R.string.whats_new_core_equalizer_desc,
                demo = DemoId.EQUALIZER,
                icon = R.drawable.equalizer,
            ),
            FeatureEntry(
                id = "sleep_timer",
                heading = R.string.whats_new_core_sleep_timer_title,
                description = R.string.whats_new_core_sleep_timer_desc,
                demo = DemoId.SLEEP_TIMER,
                icon = R.drawable.bedtime,
            ),
            FeatureEntry(
                id = "together",
                heading = R.string.whats_new_core_together_title,
                description = R.string.whats_new_core_together_desc,
                demo = DemoId.TOGETHER,
                icon = R.drawable.group,
            ),
            FeatureEntry(
                id = "recognition",
                heading = R.string.whats_new_core_recognition_title,
                description = R.string.whats_new_core_recognition_desc,
                demo = DemoId.RECOGNITION,
                icon = R.drawable.sound_wave,
            ),
            FeatureEntry(
                id = "local_music",
                heading = R.string.whats_new_core_local_music_title,
                description = R.string.whats_new_core_local_music_desc,
                demo = DemoId.LOCAL_MUSIC,
                icon = R.drawable.folder,
            ),
            FeatureEntry(
                id = "look_and_feel",
                heading = R.string.whats_new_core_look_and_feel_title,
                description = R.string.whats_new_core_look_and_feel_desc,
                demo = DemoId.LOOK_AND_FEEL,
                icon = R.drawable.gradient,
            ),
            FeatureEntry(
                id = "video_art",
                heading = R.string.whats_new_core_video_art_title,
                description = R.string.whats_new_core_video_art_desc,
                demo = DemoId.VIDEO_ART,
                icon = R.drawable.slow_motion_video,
            ),
            FeatureEntry(
                id = "cast",
                heading = R.string.whats_new_core_cast_title,
                description = R.string.whats_new_core_cast_desc,
                demo = DemoId.CAST,
                icon = R.drawable.cast,
                available = { BuildConfig.CAST_AVAILABLE },
            ),
        )

    /** The core features this build has. */
    fun availableCore(features: List<FeatureEntry> = coreFeatures): List<FeatureEntry> = features.filter { it.available() }

    /**
     * Every release's cards, newest first and with no limit. Debug builds show these from
     * Settings, so the next release's cards can be checked before its version number exists.
     */
    fun previewEntries(releases: List<WhatsNewRelease> = this.releases): List<FeatureEntry> =
        releases
            .flatMap { it.entries }
            .filter { it.available() }
            .distinctBy { it.id }

    /** The cards for [releases], newest first, each feature once, at most [MAX_NEW]. */
    fun newEntries(releases: List<WhatsNewRelease>): List<FeatureEntry> =
        releases
            .flatMap { it.entries }
            .filter { it.available() }
            .distinctBy { it.id }
            .take(MAX_NEW)

    /**
     * What Settings › What's new shows: the newest release that is not newer than the running
     * app and has something for this build, whatever was seen before.
     */
    fun latestUpTo(
        version: String,
        releases: List<WhatsNewRelease> = this.releases,
    ): List<WhatsNewRelease> =
        listOfNotNull(
            releases.firstOrNull { release ->
                Updater.compareVersions(release.version, version) <= 0 && release.entries.any { it.available() }
            },
        )
}
