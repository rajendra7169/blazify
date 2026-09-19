package com.blazify.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber

/**
 * Sends what is playing to ListenBrainz.
 *
 * The open counterpart to Last.fm: the history belongs to the listener, who can take it away
 * again. One address, one token the listener pastes in, and two kinds of message — "this is on
 * now", and "this one counted".
 */
object ListenBrainz {
    private const val API = "https://api.listenbrainz.org/1"

    /** Named in every submission, so a listener can see where a play came from. */
    private const val CLIENT = "Blazify"

    var token: String? = null

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy { HttpClient(CIO) }

    /** The name behind a token, or null when ListenBrainz will not have it. */
    suspend fun userName(token: String): String? =
        runCatching {
            val body =
                client
                    .get("$API/validate-token") {
                        header("Authorization", "Token ${token.trim()}")
                    }.bodyAsText()
            val parsed = json.parseToJsonElement(body).jsonObject
            if (parsed["valid"]?.jsonPrimitive?.content != "true") {
                null
            } else {
                parsed["user_name"]?.jsonPrimitive?.content
            }
        }.getOrNull()

    suspend fun updateNowPlaying(
        artist: String,
        track: String,
        album: String?,
        durationSeconds: Int,
        videoId: String?,
    ) {
        send(
            payload(
                listenType = "playing_now",
                artist = artist,
                track = track,
                album = album,
                durationSeconds = durationSeconds,
                videoId = videoId,
                listenedAt = null,
            ),
        )
    }

    suspend fun scrobble(
        artist: String,
        track: String,
        album: String?,
        durationSeconds: Int,
        videoId: String?,
        listenedAt: Long,
    ) {
        send(
            payload(
                listenType = "single",
                artist = artist,
                track = track,
                album = album,
                durationSeconds = durationSeconds,
                videoId = videoId,
                listenedAt = listenedAt,
            ),
        )
    }

    private suspend fun send(body: JsonObject) {
        val key = token?.trim().orEmpty()
        if (key.isEmpty()) return
        runCatching {
            client.post("$API/submit-listens") {
                header("Authorization", "Token $key")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }.onFailure {
            // A listen that does not arrive is not worth interrupting the music for.
            Timber.tag("ListenBrainz").w(it, "could not submit")
        }
    }

    /**
     * One submission, in the shape ListenBrainz asks for.
     *
     * A play with no artist or title is not sent at all: it would arrive as an entry nobody could
     * read, in a history the listener keeps for years.
     */
    internal fun payload(
        listenType: String,
        artist: String,
        track: String,
        album: String?,
        durationSeconds: Int,
        videoId: String?,
        listenedAt: Long?,
    ): JsonObject =
        buildJsonObject {
            put("listen_type", listenType)
            putJsonArray("payload") {
                add(
                    buildJsonObject {
                        if (listenedAt != null) put("listened_at", listenedAt)
                        putJsonObject("track_metadata") {
                            put("artist_name", artist)
                            put("track_name", track)
                            if (!album.isNullOrBlank()) put("release_name", album)
                            putJsonObject("additional_info") {
                                put("media_player", CLIENT)
                                put("submission_client", CLIENT)
                                if (durationSeconds > 0) put("duration", durationSeconds)
                                if (!videoId.isNullOrBlank()) {
                                    put("music_service", "music.youtube.com")
                                    put("origin_url", "https://music.youtube.com/watch?v=$videoId")
                                }
                            }
                        }
                    },
                )
            }
        }

    /** Whether there is enough here to be worth sending. */
    internal fun worthSending(artist: String, track: String) = artist.isNotBlank() && track.isNotBlank()
}
