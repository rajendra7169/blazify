package com.blazify.music.utils.cipher

import android.content.Context
import android.util.Base64
import com.blazify.innertube.YouTube
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Cosmetic "when did we add cipher support for this player" dates, shown in the song-details
 * sheet next to the player hash.
 *
 * Pulled **purely from a remote file** — nothing is bundled in the APK, so new players get a
 * date with no APK update. A small on-disk cache makes it instant/offline on later launches.
 *
 * Deliberately decoupled from [PlayerConfigStore] and the decipher path: it is a separate file,
 * it is parsed tolerantly, and every failure (no network, bad JSON, no cache yet) just yields
 * an unknown date — playback is never touched.
 *
 * File shape — the player registry kept beside the configs, where each player carries the
 * moment it was first seen:
 *   { "players": [ { "playerHash": "8c3fda2d", "firstSeenAt": "2026-09-07T08:47:23Z", ... } ] }
 *
 * The old flat map ({ "959dabb2": "2026-06-12", ... }) is still read, because a phone
 * may have it cached from before the move.
 */
object PlayerDatesStore {
    private const val TAG = "Blazify_CipherDates"

    // Our copy first, then its CDN mirror for networks that block raw GitHub, then the
    // registry our copy is kept in step with. The first one that gives any dates wins.
    private val REMOTE_URLS by lazy {
        listOf(
            "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3JhamVuZHJhNzE2OS9ibGF6aWZ5L3BsYXllci1jb25maWdzL3BsYXllci1yZWdpc3RyeS5qc29u",
            "aHR0cHM6Ly9jZG4uanNkZWxpdnIubmV0L2doL3JhamVuZHJhNzE2OS9ibGF6aWZ5QHBsYXllci1jb25maWdzL3BsYXllci1yZWdpc3RyeS5qc29u",
            "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL01ldHJvbGlzdEdyb3VwL2ZhcmFkYXkvbWFzdGVyL3JlZ2lzdHJ5L3BsYXllci1yZWdpc3RyeS5qc29u",
        ).map { String(Base64.decode(it, Base64.DEFAULT), StandardCharsets.UTF_8) }
    }

    // Own dir, NOT the shared cipher_cache (PlayerJsFetcher purges/wipes that one).
    private const val CACHE_DIR = "cipher_dates"
    private const val CACHE_FILE = "player_dates.json"

    @Volatile
    private var dates: Map<String, String> = emptyMap()

    /** Tolerant parse of either file shape into `hash -> YYYY-MM-DD`. Bad entries are skipped; never throws. */
    internal fun parse(text: String): Map<String, String> =
        runCatching {
            val root = Json.parseToJsonElement(text) as? JsonObject ?: return emptyMap()
            val players = root["players"] as? JsonArray
            buildMap {
                if (players != null) {
                    for (player in players) {
                        val entry = player as? JsonObject ?: continue
                        val hash = (entry["playerHash"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: continue
                        val seen = (entry["firstSeenAt"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: continue
                        put(hash, seen.take(10))
                    }
                } else {
                    for ((hash, value) in root) {
                        (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { put(hash, it) }
                    }
                }
            }
        }.getOrDefault(emptyMap())

    /** Load the last-fetched cache (instant/offline), then refresh from the remote files in the background. */
    fun initialize(context: Context) {
        val cache = File(File(context.filesDir, CACHE_DIR).apply { mkdirs() }, CACHE_FILE)

        dates = runCatching {
            if (cache.exists()) parse(cache.readText()) else emptyMap()
        }.getOrDefault(emptyMap())

        Thread {
            for (remoteUrl in REMOTE_URLS) {
                val loaded = runCatching {
                    val body = fetchRemote(remoteUrl)
                    val remote = parse(body)
                    if (remote.isNotEmpty()) {
                        dates = remote // the remote file is the single source of truth
                        runCatching { cache.writeText(body) } // persist for the next launch / offline
                    }
                    remote.isNotEmpty()
                }.onFailure { Timber.tag(TAG).d("dates refresh skipped: ${it.message}") }
                    .getOrDefault(false)
                if (loaded) break
            }
        }.apply { isDaemon = true; name = "PlayerDatesRefresh" }.start()
    }

    /** Onboarding date for [hash] (`YYYY-MM-DD`), or null if unknown. */
    fun get(hash: String?): String? = hash?.let { dates[it] }

    private fun fetchRemote(remoteUrl: String): String {
        val url = URL(remoteUrl)
        val conn = (YouTube.proxy?.let { url.openConnection(it) } ?: url.openConnection()) as HttpURLConnection
        return try {
            conn.run {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            conn.disconnect() // release the socket immediately, including on the error path
        }
    }
}
