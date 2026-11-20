package com.blazify.music.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import com.blazify.innertube.YouTube
import com.blazify.music.extensions.isInternetConfirmed
import com.blazify.music.extensions.isInternetConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
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

    /**
     * Whether there is no working connection: none at all, or one Android has not confirmed
     * reaches the internet, or one Blazify found dead a little while ago, where YouTube is then
     * asked directly and does not answer. A confirmed connection is taken at its word, as Home's
     * buttons take it, so nothing is asked when the connection is fine.
     */
    suspend fun isOffline(context: Context): Boolean =
        when {
            !context.isInternetConnected() -> true
            !context.isInternetConfirmed() || recentlyDead() -> !canReachYouTube()
            else -> false
        }

    /** Fires whenever the phone's connection comes, goes or changes. */
    fun changes(context: Context): Flow<Unit> =
        callbackFlow {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            if (manager == null) {
                awaitClose {}
                return@callbackFlow
            }
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(Unit)
                    }

                    override fun onLost(network: Network) {
                        trySend(Unit)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(Unit)
                    }
                }
            val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
            awaitClose { if (registered) runCatching { manager.unregisterNetworkCallback(callback) } }
        }.conflate()
}
