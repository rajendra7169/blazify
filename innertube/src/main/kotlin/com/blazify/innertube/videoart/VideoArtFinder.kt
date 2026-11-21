package com.blazify.innertube.videoart

import com.blazify.innertube.videoart.VideoArtText.LYRIC_WORDS
import com.blazify.innertube.videoart.VideoArtText.badWord
import com.blazify.innertube.videoart.VideoArtText.contains
import com.blazify.innertube.videoart.VideoArtText.coreTitle
import com.blazify.innertube.videoart.VideoArtText.fold
import com.blazify.innertube.videoart.VideoArtText.hasGoodWord
import com.blazify.innertube.videoart.VideoArtText.words
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/** The song a video is wanted for. [length] in seconds, 0 when not known. */
data class VideoArtSong(
    val title: String,
    val artists: List<String>,
    val length: Int,
)

enum class VideoArtSource {
    /** YouTube Music's official music video from the song's own artist. */
    OFFICIAL,

    /** Everyday YouTube, from a trusted channel, scored. */
    SCORED,

    /** The artist's own lyric video, when nothing better turned up. */
    LYRIC,
}

data class VideoArtPick(
    val videoId: String,
    val title: String,
    /** Seconds, 0 when not shown. */
    val length: Int,
    val source: VideoArtSource,
    val score: Double?,
    val channel: String? = null,
)

/** What the finder chose, and when it chose nothing, why the closest videos were turned down. */
data class VideoArtResult(
    val pick: VideoArtPick?,
    val notes: List<String> = emptyList(),
)

/**
 * Finds the video that goes behind a song, from the song's title, artists and length.
 *
 * 1. YouTube Music's official music videos from the song's own artist, the same song: the most
 *    watched, near the song's length.
 * 2. When there is none, everyday YouTube, from the artist's channel or a trusted channel
 *    naming the artist, scored on views, length and title.
 * 3. The artist's own lyric video, when that is all there is.
 */
object VideoArtFinder {
    /** How many YouTube Music rows are looked through. */
    const val MUSIC_CANDIDATES = 10

    /** How many everyday YouTube videos are looked through. */
    const val WEB_CANDIDATES = 15

    /** Below this, no scored video is trusted to be the song's. */
    const val MIN_SCORE = 5.5

    private const val OMV = "MUSIC_VIDEO_TYPE_OMV"

    /**
     * [musicSearch] and [webSearch] are asked only when needed: everyday YouTube only when
     * YouTube Music had no real video.
     */
    suspend fun find(
        song: VideoArtSong,
        musicSearch: suspend (String) -> List<MusicVideoRow>,
        webSearch: suspend (String) -> List<WebVideoRow>,
        musicCandidates: Int = MUSIC_CANDIDATES,
        webCandidates: Int = WEB_CANDIDATES,
    ): VideoArtResult {
        val main = song.artists.firstOrNull().orEmpty()
        val official = pickOfficial(song, musicSearch("${song.title} $main".trim()).take(musicCandidates))
        if (official != null && official.source == VideoArtSource.OFFICIAL) return VideoArtResult(official)
        val scored = pickScored(song, webSearch("${coreTitle(song.title)} $main".trim()).take(webCandidates))
        if (scored.pick != null) return scored
        if (official != null) return VideoArtResult(official)
        return VideoArtResult(null, scored.notes.take(6))
    }

    /**
     * The official music video among YouTube Music's [rows]: [VideoArtSource.OFFICIAL] for a real
     * video, [VideoArtSource.LYRIC] when the artist's lyric video is the only one, else null.
     */
    fun pickOfficial(song: VideoArtSong, rows: List<MusicVideoRow>): VideoArtPick? {
        val core = coreTitle(song.title)
        val artists = song.artists.filter { it.isNotEmpty() }

        // "Official" from the song's own artist: a fan channel can carry the mark too.
        fun own(row: MusicVideoRow) = artists.any { contains(it, row.artist) || contains(row.artist, it) }

        fun lyricOnly(row: MusicVideoRow) = badWord(row.title, song.title) in LYRIC_WORDS

        val candidates =
            rows.filter { row ->
                row.musicVideoType == OMV &&
                    own(row) &&
                    contains(core, row.title) &&
                    (badWord(row.title, song.title) == null || lyricOnly(row))
            }

        fun score(row: MusicVideoRow): Double {
            val near =
                if (song.length != 0 && row.length != 0) min(abs(row.length - song.length) / 60.0, 2.0) else 0.0
            // The artist's own lyric video only when there is nothing better, anywhere.
            return 0.8 * log10(max(row.views, 1L).toDouble()) - near - (if (lyricOnly(row)) 100 else 0)
        }

        val best = candidates.maxByOrNull { score(it) } ?: return null
        return VideoArtPick(
            videoId = best.videoId,
            title = best.title,
            length = best.length,
            source = if (lyricOnly(best)) VideoArtSource.LYRIC else VideoArtSource.OFFICIAL,
            score = score(best),
        )
    }

    /** The best everyday YouTube video among [rows] (in search order), if it scores high enough. */
    fun pickScored(song: VideoArtSong, rows: List<WebVideoRow>): VideoArtResult {
        val core = coreTitle(song.title)
        val length = song.length
        val artists = song.artists.filter { it.isNotEmpty() }
        // Labels shorten names in long titles ("Arijit S, Shilpa R"): a first name of 4+ letters counts.
        val firsts =
            song.artists.mapNotNull { words(it).firstOrNull() }.filter { it.codePointCount(0, it.length) >= 4 }
        val notes = ArrayList<String>()
        val kept = ArrayList<Pair<Double, WebVideoRow>>()

        rows.forEachIndexed { rank, row ->
            if (!contains(core, row.title)) return@forEachIndexed
            val bad = badWord(row.title, song.title)
            if (bad != null) {
                notes += "x ${row.title.take(40)} ($bad)"
                return@forEachIndexed
            }
            if (row.length != 0 &&
                (row.length < 60 || (length != 0 && row.length > max(length * 2, length + 240)))
            ) {
                notes += "x ${row.title.take(40)} (length ${row.length})"
                return@forEachIndexed
            }
            val byArtist = artists.any { contains(it, row.channel) }
            val titleWords = words(row.title).toHashSet()
            val named = artists.any { contains(it, row.title) } || firsts.any { it in titleWords }
            if (!(byArtist || named)) {
                notes += "x ${row.title.take(40)} (no artist)"
                return@forEachIndexed
            }
            if (row.artistBadge && !byArtist) {
                // Another artist's own channel: a cover, however it is titled.
                notes += "x ${row.title.take(40)} (another artist ${row.channel})"
                return@forEachIndexed
            }
            val trusted = row.artistBadge || row.verified || "vevo" in fold(row.channel)
            if (!trusted) {
                notes += "x ${row.title.take(40)} (unverified ${row.channel})"
                return@forEachIndexed
            }
            var score = 0.0
            score += if (row.artistBadge) 4 else 2
            score += if (byArtist) 3 else 0
            score += if (hasGoodWord(row.title)) 2 else 0
            score += 0.6 * log10(max(row.views, 1L).toDouble())
            if (length != 0 && row.length != 0) score -= min(abs(row.length - length) / 60.0, 3.0)
            score -= 0.05 * rank
            kept += score to row
        }
        if (kept.isEmpty()) return VideoArtResult(null, notes)

        // The most watched trusted upload is usually the video itself.
        val top = kept.maxOf { it.second.views }
        val (bestScore, best) =
            kept.map { (score, row) -> (score + if (row.views == top) 1.5 else 0.0) to row }.maxBy { it.first }
        if (bestScore < MIN_SCORE) return VideoArtResult(null, notes)
        return VideoArtResult(
            VideoArtPick(
                videoId = best.videoId,
                title = best.title,
                length = best.length,
                source = VideoArtSource.SCORED,
                score = bestScore,
                channel = best.channel,
            ),
            notes,
        )
    }
}
