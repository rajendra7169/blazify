/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Client for the Better Lyrics aggregator API (lyrics.api.dacubeking.com) —
 * the same backend the Better Lyrics browser extension uses. It aggregates
 * Musixmatch, Better Lyrics, BiniLyrics, Portato/Legato and LRCLib, matched by
 * the EXACT YouTube video id, and streams results over SSE.
 *
 * Auth: the API is fronted by Cloudflare Turnstile. We solve the challenge in
 * an invisible WebView (the API hosts its own /challenge page that posts the
 * token via postMessage), exchange it at /verify-turnstile for a JWT, and cache
 * the JWT in DataStore until it expires.
 */

package com.blazify.music.lyrics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.blazify.music.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object BetterLyricsClient {
    private const val TAG = "BetterLyricsClient"
    private const val API = "https://lyrics.api.dacubeking.com/"
    private const val CHALLENGE_URL = API + "challenge"
    private const val SOLVE_TIMEOUT_MS = 45_000L
    private const val STREAM_BUDGET_MS = 9_000L

    private val JwtKey = stringPreferencesKey("betterLyricsJwt")
    private val jwtMutex = Mutex()

    // Back off after a failed Turnstile solve instead of hammering retries.
    private const val SOLVE_BACKOFF_MS = 120_000L

    @Volatile
    private var lastSolveFailureAt = 0L

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    /** Ensure a JWT exists (solves the challenge if needed). Call at app/service start. */
    suspend fun warmUp(context: Context) {
        getJwt(context)
    }

    /**
     * Fetch lyrics for the exact video id. Returns the best available lyrics
     * (Musixmatch word-synced > line-synced LRC from any source > plain), or null.
     */
    suspend fun getLyrics(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): String? = withContext(Dispatchers.IO) {
        val jwt = getJwt(context) ?: return@withContext null

        val form = FormBody.Builder()
            .add("videoId", videoId)
            .add("song", title)
            .add("artist", artist)
            .apply {
                if (duration > 0) add("duration", duration.toString())
                if (!album.isNullOrBlank()) add("album", album)
            }
            .add("alwaysFetchMetadata", "false")
            .add("token", jwt)
            .build()

        val request = Request.Builder().url(API + "v2/lyrics").post(form).build()
        try {
            http.newCall(request).execute().use { response ->
                if (response.code == 403 || response.code == 401) {
                    // JWT rejected — clear it so the next fetch re-solves the challenge.
                    Timber.tag(TAG).w("JWT rejected (${response.code}); clearing cached token")
                    context.dataStore.edit { it.remove(JwtKey) }
                    return@withContext null
                }
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w("v2/lyrics failed: ${response.code}")
                    return@withContext null
                }
                val source = response.body?.source() ?: return@withContext null

                // SSE parsing: "event: provider" + "data: {...}" blocks, blank-line separated.
                val results = LinkedHashMap<String, String>()
                var event = ""
                val data = StringBuilder()
                val deadline = System.currentTimeMillis() + STREAM_BUDGET_MS

                // org.json turns an explicit JSON null into the STRING "null" via
                // optString — guard against it or we show literal "null" as lyrics.
                fun JSONObject.lyricsField(key: String): String? {
                    if (!has(key) || isNull(key)) return null
                    return optString(key).takeIf { it.isNotBlank() && it != "null" }
                }

                fun flushBlock() {
                    val payload = data.toString()
                    data.setLength(0)
                    if (payload.isBlank() || payload == "[DONE]") return
                    if (event != "provider") return
                    try {
                        val json = JSONObject(payload)
                        val provider = json.optString("provider")
                        val res = json.optJSONObject("results") ?: return
                        res.lyricsField("wordByWord")?.let { results["rich:$provider"] = it }
                        res.lyricsField("synced")?.let { results["sync:$provider"] = it }
                        res.lyricsField("plainLyrics")?.let { results["plain:$provider"] = it }
                        res.lyricsField("plain")?.let { results["plain:$provider"] = it }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w("Bad SSE payload: ${e.message}")
                    }
                }

                while (!source.exhausted() && System.currentTimeMillis() < deadline) {
                    val line = source.readUtf8Line() ?: break
                    when {
                        line.startsWith("event:") -> event = line.substringAfter(':').trim()
                        line.startsWith("data:") -> data.append(line.substringAfter(':').trim())
                        line.isBlank() -> {
                            flushBlock()
                            // Musixmatch word-synced is the best possible — stop early.
                            if (results.containsKey("rich:musixmatch")) break
                            event = ""
                        }
                    }
                }
                flushBlock()

                if (results.isNotEmpty()) {
                    Timber.tag(TAG).i("Stream results: ${results.keys.joinToString()}")
                }
                pickBest(results)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Stream error: ${e.message}")
            null
        }
    }

    private fun pickBest(results: Map<String, String>): String? {
        val order = listOf(
            "rich:musixmatch",
            "sync:musixmatch", "sync:blyrics", "sync:binimum", "sync:portato", "sync:legato", "sync:lrclib",
        )
        // Timed candidates must actually parse as synced lyrics with our parser
        // (guards against formats we can't render being classified as plain).
        fun parsesSynced(lyrics: String): Boolean =
            try {
                LyricsUtils.parseLyrics(lyrics).isNotEmpty()
            } catch (e: Exception) {
                false
            }

        for (key in order) results[key]?.let { if (parsesSynced(it)) return it }
        results.entries.firstOrNull { it.key.startsWith("sync:") && parsesSynced(it.value) }?.let { return it.value }
        // Fall back to plain text (min length filters out junk fragments).
        results.entries.firstOrNull { it.key.startsWith("plain:") && it.value.length > 40 }?.let { return it.value }
        return null
    }

    /* ---------------- JWT management ---------------- */

    private suspend fun getJwt(context: Context): String? = jwtMutex.withLock {
        val cached = context.dataStore.data.map { it[JwtKey] }.first()
        if (cached != null && !isJwtExpired(cached)) return cached
        if (System.currentTimeMillis() - lastSolveFailureAt < SOLVE_BACKOFF_MS) return null

        val turnstileToken = solveTurnstile(context)
        if (turnstileToken == null) {
            lastSolveFailureAt = System.currentTimeMillis()
            return null
        }
        val jwt = exchangeToken(turnstileToken)
        if (jwt == null) {
            lastSolveFailureAt = System.currentTimeMillis()
            return null
        }
        context.dataStore.edit { it[JwtKey] = jwt }
        Timber.tag(TAG).i("Obtained fresh JWT")
        jwt
    }

    private fun isJwtExpired(token: String): Boolean =
        try {
            val payload = token.split(".")[1]
            val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            val exp = JSONObject(decoded).optLong("exp", 0L)
            exp <= 0L || System.currentTimeMillis() / 1000 > exp - 60
        } catch (e: Exception) {
            true
        }

    private fun exchangeToken(turnstileToken: String): String? =
        try {
            val body = JSONObject().put("token", turnstileToken).toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(API + "verify-turnstile").post(body).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w("verify-turnstile failed: ${response.code}")
                    null
                } else {
                    JSONObject(response.body?.string().orEmpty()).optString("jwt").takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("verify-turnstile error: ${e.message}")
            null
        }

    /**
     * Solve the Cloudflare Turnstile challenge in an invisible WebView. The API's
     * own /challenge page posts {type: "turnstile-token", token} to its parent —
     * at top level that lands on the page's own window, where our injected
     * listener forwards it through the JS bridge.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun solveTurnstile(context: Context): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val resumed = AtomicBoolean(false)
            var webView: WebView? = null
            val handler = Handler(Looper.getMainLooper())

            fun finish(token: String?) {
                if (resumed.compareAndSet(false, true)) {
                    handler.post {
                        try {
                            webView?.destroy()
                        } catch (_: Exception) {
                        }
                        webView = null
                    }
                    cont.resume(token)
                }
            }

            try {
                val wv = WebView(context.applicationContext)
                webView = wv
                wv.settings.javaScriptEnabled = true
                wv.settings.domStorageEnabled = true
                wv.addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onMessage(raw: String) {
                            try {
                                val json = JSONObject(raw)
                                when (json.optString("type")) {
                                    "turnstile-token" -> {
                                        Timber.tag(TAG).i("Turnstile solved")
                                        finish(json.optString("token").takeIf { it.isNotBlank() })
                                    }
                                    "turnstile-error", "turnstile-timeout" -> {
                                        Timber.tag(TAG).w("Turnstile failed: ${json.optString("error")}")
                                        finish(null)
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }
                    },
                    "BlazifyBridge",
                )
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            """
                            window.addEventListener('message', function(e) {
                                try { BlazifyBridge.onMessage(JSON.stringify(e.data)); } catch (err) {}
                            });
                            """.trimIndent(),
                            null,
                        )
                    }
                }
                wv.loadUrl(CHALLENGE_URL)
                handler.postDelayed({
                    Timber.tag(TAG).w("Turnstile solve timed out")
                    finish(null)
                }, SOLVE_TIMEOUT_MS)
                cont.invokeOnCancellation { finish(null) }
            } catch (e: Exception) {
                Timber.tag(TAG).w("WebView error: ${e.message}")
                finish(null)
            }
        }
    }
}
