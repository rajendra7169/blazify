package com.blazify.innertube.videoart

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One row of YouTube Music's video search, as Video Art needs it. */
data class MusicVideoRow(
    val videoId: String,
    val title: String,
    /** The first part of the row's second line: whoever put the video up. */
    val artist: String,
    /** "MUSIC_VIDEO_TYPE_OMV" for an official music video, or null when the row says nothing. */
    val musicVideoType: String?,
    val views: Long,
    /** Seconds, 0 when not shown. */
    val length: Int,
)

/** One video of everyday YouTube's search, as Video Art needs it. */
data class WebVideoRow(
    val videoId: String,
    val title: String,
    val channel: String,
    /** The channel carries the music note: the artist's own official channel. */
    val artistBadge: Boolean,
    /** The channel carries a tick (an artist's channel has one too). */
    val verified: Boolean,
    val views: Long,
    /** Seconds, 0 when not shown (a live stream). */
    val length: Int,
)

/**
 * Reads search answers loosely, by looking for the rows wherever they are in the answer rather
 * than by a fixed path: the pages move their shelves around often, the rows themselves rarely.
 */
object VideoArtRows {
    /** YouTube Music's "Videos" search filter, the one the finder was tuned with. */
    const val MUSIC_VIDEO_PARAMS = "EgWKAQIQAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D"

    private val VIDEO_ID = Regex("\"videoId\":\"([\\w-]{11})\"")
    private val VIDEO_TYPE = Regex("MUSIC_VIDEO_TYPE_(\\w+)")

    /** Rows of a YouTube Music search answer, in order. */
    fun music(answer: JsonElement): List<MusicVideoRow> =
        findAll(answer, "musicResponsiveListItemRenderer").mapNotNull { row ->
            val columns =
                (row["flexColumns"] as? JsonArray).orEmpty().map { column ->
                    runs((column as? JsonObject)?.obj("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
                }
            val raw = row.toString()
            val videoId = VIDEO_ID.find(raw)?.groupValues?.get(1) ?: return@mapNotNull null
            val type = VIDEO_TYPE.find(raw)?.groupValues?.get(1)
            val parts = columns.getOrElse(1) { "" }.split("•").map { it.trim() }
            MusicVideoRow(
                videoId = videoId,
                title = columns.getOrElse(0) { "" },
                artist = parts.first(),
                musicVideoType = type?.let { "MUSIC_VIDEO_TYPE_$it" },
                views = VideoArtText.parseViews(parts.firstOrNull { "view" in it }),
                length = VideoArtText.parseSeconds(parts.last()),
            )
        }

    /** Videos of an everyday YouTube search answer, in order. */
    fun web(answer: JsonElement): List<WebVideoRow> =
        findAll(answer, "videoRenderer").mapNotNull { video ->
            val badges =
                (video["ownerBadges"] as? JsonArray).orEmpty().mapNotNull { badge ->
                    (badge as? JsonObject)?.obj("metadataBadgeRenderer")?.string("style")
                }
            WebVideoRow(
                videoId = video.string("videoId") ?: return@mapNotNull null,
                title = runs(video["title"]),
                channel = runs(video["ownerText"]),
                artistBadge = badges.any { "ARTIST" in it },
                verified = badges.any { "VERIFIED" in it },
                views = VideoArtText.parseViews(video.obj("viewCountText")?.string("simpleText")),
                length = VideoArtText.parseSeconds(video.obj("lengthText")?.string("simpleText")),
            )
        }

    /** Every object under [key], anywhere in [element], in the order a reader meets them. */
    private fun findAll(element: JsonElement, key: String): List<JsonObject> {
        val found = ArrayList<JsonObject>()
        fun walk(e: JsonElement) {
            when (e) {
                is JsonObject -> {
                    (e[key] as? JsonObject)?.let { found += it }
                    e.values.forEach { walk(it) }
                }
                is JsonArray -> e.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(element)
        return found
    }

    private fun runs(text: JsonElement?): String =
        ((text as? JsonObject)?.get("runs") as? JsonArray).orEmpty().joinToString("") { run ->
            (run as? JsonObject)?.string("text").orEmpty()
        }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
