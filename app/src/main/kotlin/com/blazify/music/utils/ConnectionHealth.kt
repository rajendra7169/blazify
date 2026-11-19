package com.blazify.music.utils

import android.os.SystemClock
import com.blazify.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Whether YouTube can really be reached, as Blazify last found out for itself.
 *
 * Android goes on calling a network connected long after it has stopped carrying anything: Wi-Fi
 * whose line is down, mobile data whose pack has run out. Trusting it, a shuffle asked YouTube for
 * a mix that could not come, and songs that could not load were waited on and blamed one by one,
 * while the songs saved on the phone sat unplayed.
 */
object ConnectionHealth {
    /** Answers with nothing at all, quickly, wherever YouTube itself answers. */
    private const val REACH_URL = "https://www.youtube.com/generate_204"
    private const val REACH_TIMEOUT_MS = 4_000L

    /** How long a failed look is trusted before asking again is worth the wait. */
    private const val MEMORY_MS = 10 * 60_000L

    @Volatile
    private var deadAt = 0L

    /** Asks YouTube directly, giving it a few seconds, and remembers the answer. */
    suspend fun canReachYouTube(): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                OkHttpClient
                    .Builder()
                    .proxy(YouTube.proxy)
                    .callTimeout(REACH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .build()
                    .newCall(Request.Builder().url(REACH_URL).head().build())
                    .execute()
                    .use { true }
            }.getOrDefault(false)
        }.also { deadAt = if (it) 0L else SystemClock.elapsedRealtime() }

    /** True when YouTube could not be reached a little while ago and has not answered since. */
    fun recentlyDead(): Boolean = deadAt != 0L && SystemClock.elapsedRealtime() - deadAt < MEMORY_MS
}
