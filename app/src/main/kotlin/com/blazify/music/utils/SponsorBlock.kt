package com.blazify.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.MessageDigest

/**
 * The parts of a video that are not the song, as marked by SponsorBlock's community.
 *
 * Asked for by the first four characters of the video id's hash rather than the id itself: the
 * answer covers every video whose hash starts the same way, so the server is never told which
 * song is playing. That costs a slightly bigger reply and keeps the promise this app makes about
 * not being watched.
 */
object SponsorBlock {
    /** What can be skipped. Only the ones that make sense for music are offered. */
    enum class Category(val id: String) {
        /** Talking, credits, silence — everything in a music video that is not the song. */
        NON_MUSIC("music_offtopic"),
        SPONSOR("sponsor"),
        SELF_PROMOTION("selfpromo"),
        INTRO("intro"),
        OUTRO("outro"),
        ;

        companion object {
            fun byId(id: String) = entries.firstOrNull { it.id == id }
        }
    }

    /** A stretch to skip, in milliseconds, so it lines up with the player's own clock. */
    data class Segment(
        val startMs: Long,
        val endMs: Long,
        val category: Category,
    )

    private const val API = "https://sponsor.ajay.app/api/skipSegments"

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy { HttpClient(CIO) }

    suspend fun segments(videoId: String, categories: Set<Category>): List<Segment> {
        if (categories.isEmpty()) return emptyList()
        return runCatching {
            val body =
                client
                    .get("$API/${hashPrefix(videoId)}") {
                        parameter("categories", categories.joinToString(",", "[", "]") { "\"${it.id}\"" })
                    }.bodyAsText()
            parse(body, videoId, categories)
        }.onFailure {
            Timber.tag("SponsorBlock").w(it, "could not fetch segments")
        }.getOrDefault(emptyList())
    }

    /** The first four characters of the video id's SHA-256, which is all the server is told. */
    internal fun hashPrefix(videoId: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(videoId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(4)

    /**
     * The segments for one video, out of an answer that covers many.
     *
     * Only what the community agrees on is kept: a segment voted below zero is one people say is
     * wrong, and skipping on a wrong mark is worse than not skipping at all.
     */
    internal fun parse(body: String, videoId: String, categories: Set<Category>): List<Segment> {
        val videos = runCatching { json.decodeFromString<List<VideoJson>>(body) }.getOrNull() ?: return emptyList()
        val wanted = categories.map { it.id }.toSet()
        return videos
            .firstOrNull { it.videoID == videoId }
            ?.segments
            .orEmpty()
            .asSequence()
            .filter { it.actionType == "skip" && it.votes >= 0 && it.category in wanted }
            .mapNotNull { raw ->
                val category = Category.byId(raw.category) ?: return@mapNotNull null
                val start = (raw.segment.getOrNull(0) ?: return@mapNotNull null)
                val end = (raw.segment.getOrNull(1) ?: return@mapNotNull null)
                if (end <= start) return@mapNotNull null
                Segment((start * 1000).toLong(), (end * 1000).toLong(), category)
            }.sortedBy { it.startMs }
            .toList()
    }

    /**
     * The segment the player is inside now, or null.
     *
     * The last few hundred milliseconds are left alone: jumping out of a segment that is about to
     * end anyway only makes the music stutter.
     */
    internal fun segmentAt(segments: List<Segment>, positionMs: Long): Segment? =
        segments.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs - 500 }

    @Serializable
    private data class VideoJson(
        val videoID: String = "",
        val segments: List<SegmentJson> = emptyList(),
    )

    @Serializable
    private data class SegmentJson(
        val category: String = "",
        val actionType: String = "skip",
        val segment: List<Double> = emptyList(),
        val votes: Int = 0,
    )
}
