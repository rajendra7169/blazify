/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.lyrics

import android.content.Context
import android.util.LruCache
import com.blazify.music.constants.LyricsProviderOrderKey
import com.blazify.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.blazify.music.models.MediaMetadata
import com.blazify.music.utils.NetworkConnectivityObserver
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_LYRICS_FETCH_MS = 16000L
private const val PER_PROVIDER_TIMEOUT_MS = 12000L
private const val PROVIDER_NONE = ""

// MUST be a singleton: the service preload, the player view and the download
// hook all fetch lyrics — without a shared instance each gets its own in-flight
// map and the same song is raced against every provider several times over
// (seen in logcat as duplicate "Racing N providers" lines seconds apart).
@Singleton
class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    val preferred =
        context.dataStore.data
            .map { preferences ->
                resolveLyricsProviders(preferences)
            }.distinctUntilChanged()

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    // The service preload and the player view often request the same song at the
    // same moment — share one in-flight fetch instead of hitting every provider twice.
    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<LyricsWithProvider>>()

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        val deferred = inFlight.computeIfAbsent(mediaMetadata.id) {
            fetchScope.async {
                try {
                    fetchLyrics(mediaMetadata)
                } finally {
                    inFlight.remove(mediaMetadata.id)
                }
            }
        }
        return deferred.await()
    }

    private suspend fun fetchLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return LyricsWithProvider(cached.lyrics, cached.providerName)
        }

        val orderedProviders = context.dataStore.data
            .map { preferences -> resolveLyricsProviders(preferences) }
            .first()

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
            val enabledProviders = orderedProviders.filter { it.isEnabled(context) }

            Timber.tag("LyricsHelper").d("Racing ${enabledProviders.size} providers in parallel for: $cleanedTitle by ${mediaMetadata.artists.joinToString { it.name }}")

            // Fire ALL providers concurrently; take the first success in priority
            // order. Wall time ≈ fastest successful provider instead of the sum of
            // every failing provider's timeout.
            coroutineScope {
                val attempts = enabledProviders.map { provider ->
                    async {
                        try {
                            withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                                provider.getLyrics(
                                    context,
                                    mediaMetadata.id,
                                    cleanedTitle,
                                    mediaMetadata.artists.joinToString { it.name },
                                    mediaMetadata.duration,
                                    mediaMetadata.album?.title,
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.tag("LyricsHelper").w("${provider.name} threw: ${e.message}")
                            null
                        }
                    }
                }

                // Trust tiers: results from TRUSTED sources (exact-id or strict
                // duration+title matching) always beat results from FUZZY keyword
                // sources (KuGou/LyricsPlus), even when the trusted result is plain
                // and the fuzzy one is synced — a correct plain lyric beats a wrong
                // scrolling one. Within each tier, synced > plain.
                //
                // Among TRUSTED SYNCED results we don't just take the first: different
                // sources are timed to different editions (Apple's full version vs a
                // shorter YouTube cut), so we keep the one whose timeline best fits the
                // edition actually playing. We stop early only once a near-perfect fit
                // (≤15% off) is in hand — good enough, no need to wait for the rest.
                var trustedSynced: LyricsWithProvider? = null
                var trustedSyncedMisfit = Float.MAX_VALUE
                var trustedPlain: LyricsWithProvider? = null
                var fuzzySynced: LyricsWithProvider? = null
                var fuzzyPlain: LyricsWithProvider? = null
                for ((index, attempt) in attempts.withIndex()) {
                    val providerName = enabledProviders[index].name
                    val providerResult = attempt.await()
                    if (providerResult != null && providerResult.isSuccess) {
                        val raw = providerResult.getOrNull()!!
                        // Reject only FUZZY results whose timeline doesn't fit — that's
                        // the signature of a wrong-song keyword match. Trusted sources
                        // matched by id / strict search, so a timeline mismatch there is
                        // just a different edit of the right song — keep it.
                        if (providerName !in TRUSTED_PROVIDERS && !isPlausible(raw, mediaMetadata.duration)) {
                            Timber.tag("LyricsHelper").w("$providerName returned implausible lyrics (timeline/duration mismatch) — skipping")
                            continue
                        }
                        val filtered = LyricsUtils.filterLyricsCreditLines(raw)
                        val synced = isSynced(filtered)
                        val trusted = providerName in TRUSTED_PROVIDERS
                        val misfit = if (synced) timelineMisfit(filtered, mediaMetadata.duration) else Float.MAX_VALUE
                        Timber.tag("LyricsHelper").i("Got ${if (synced) "SYNCED" else "plain"} lyrics from $providerName (${if (trusted) "trusted" else "fuzzy"}${if (synced && misfit < Float.MAX_VALUE) ", fit ${(100 - (misfit * 100).toInt())}%" else ""})")
                        when {
                            trusted && synced -> if (misfit < trustedSyncedMisfit) {
                                trustedSynced = LyricsWithProvider(filtered, providerName)
                                trustedSyncedMisfit = misfit
                            }
                            trusted && !synced && trustedPlain == null -> trustedPlain = LyricsWithProvider(filtered, providerName)
                            !trusted && synced && fuzzySynced == null -> fuzzySynced = LyricsWithProvider(filtered, providerName)
                            !trusted && !synced && fuzzyPlain == null -> fuzzyPlain = LyricsWithProvider(filtered, providerName)
                        }
                        // A near-perfect trusted synced fit is the best possible outcome — stop.
                        if (trustedSynced != null && trustedSyncedMisfit <= 0.15f) break
                    }
                }
                // Cancel any still-running lower-priority attempts.
                attempts.forEach { it.cancel() }

                val chosen = trustedSynced ?: trustedPlain ?: fuzzySynced ?: fuzzyPlain
                if (chosen == null) {
                    Timber.tag("LyricsHelper").w("No lyrics found after racing all providers")
                } else {
                    // Diagnostic only: how well does the chosen synced timeline fit the
                    // playing edit? A last timestamp far from the song length means the
                    // lyrics are timed to a different recording and can't scroll in sync.
                    val fitNote = if (isSynced(chosen.lyrics) && mediaMetadata.duration > 0) {
                        val lastSec = runCatching { LyricsUtils.parseLyrics(chosen.lyrics).maxOf { it.time } / 1000 }.getOrDefault(-1)
                        val pct = if (lastSec > 0) (lastSec * 100 / mediaMetadata.duration) else -1
                        " [sync fit: last ${lastSec}s / song ${mediaMetadata.duration}s = ${pct}%]"
                    } else ""
                    Timber.tag("LyricsHelper").i("Chose lyrics from ${chosen.provider}$fitNote")
                }
                chosen ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
            }
        }

        return result ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach { callback(it) }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) return

        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
            val allProviders = context.dataStore.data
                .map { preferences -> resolveLyricsProviders(preferences) }
                .first()
            val enabledProviders = allProviders.filter { it.isEnabled(context) }

            val otherProviders = enabledProviders.filter { it.name != "LyricsPlus" }
            val lyricsPlusProvider = enabledProviders.find { it.name == "LyricsPlus" }

            val callbackMutex = Any()

            val otherJobs = otherProviders.map { provider ->
                launch {
                    try {
                        provider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val result = LyricsResult(provider.name, filteredLyrics)
                            synchronized(callbackMutex) {
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            otherJobs.forEach { it.join() }

            val otherLyricsCount = allResult.count { it.providerName != "LyricsPlus" }
            if (lyricsPlusProvider != null && otherLyricsCount <= 2) {
                launch {
                    try {
                        lyricsPlusProvider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val result = LyricsResult(lyricsPlusProvider.name, filteredLyrics)
                            synchronized(callbackMutex) {
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }.join()
            }

            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    /** True when the lyrics carry [mm:ss] timestamps (scrolling/karaoke capable). */
    private fun isSynced(lyrics: String): Boolean =
        try {
            LyricsUtils.parseLyrics(lyrics).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    /**
     * Sanity-check fetched lyrics against the song: for SYNCED lyrics the last
     * timestamp must fall inside a sane window of the track length (40% .. +30s).
     * Wrong-song fuzzy matches almost always fail this. Plain (unsynced) lyrics
     * can't be time-verified and are accepted as-is.
     */
    private fun isPlausible(lyrics: String, durationSec: Int): Boolean {
        if (durationSec <= 0) return true
        if (lyrics == LYRICS_NOT_FOUND || lyrics.isBlank()) return true
        val entries = try {
            LyricsUtils.parseLyrics(lyrics)
        } catch (e: Exception) {
            return true
        }
        if (entries.isEmpty()) return true // plain lyrics — nothing to verify against
        val lastMs = entries.maxOf { it.time }
        val durationMs = durationSec * 1000L
        return lastMs >= (durationMs * 0.4).toLong() && lastMs <= durationMs + 15_000L
    }

    /**
     * How badly a SYNCED timeline fits the playing edit: |lastTimestamp − duration|
     * as a fraction of duration. 0 = the last line lands exactly at the song's end
     * (perfect); 0.76 = the timeline runs to 176% of the song (a different, longer
     * edition). Used to pick the best-fitting synced source, not to reject any.
     * Returns 0 when the duration is unknown (can't judge — treat as acceptable).
     */
    private fun timelineMisfit(lyrics: String, durationSec: Int): Float {
        if (durationSec <= 0) return 0f
        val entries = try {
            LyricsUtils.parseLyrics(lyrics)
        } catch (e: Exception) {
            return Float.MAX_VALUE
        }
        if (entries.isEmpty()) return Float.MAX_VALUE
        val lastSec = entries.maxOf { it.time } / 1000f
        return kotlin.math.abs(lastSec - durationSec) / durationSec
    }

    private fun resolveLyricsProviders(preferences: androidx.datastore.preferences.core.Preferences): List<LyricsProvider> {
        val providerOrder = preferences[LyricsProviderOrderKey].orEmpty()
        if (providerOrder.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(providerOrder)
        }

        return LyricsProviderRegistry.getDefaultProviderOrder()
            .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3

        // Sources that identify the song exactly (video id) or verify candidates
        // by duration AND title before accepting (Paxsenix scores Apple Music
        // results; LrcLib matches duration strictly). Everything else is fuzzy
        // keyword search and can return wrong-song lyrics (KuGou, LyricsPlus).
        private val TRUSTED_PROVIDERS = setOf("Paxsenix", "LrcLib", "BetterLyrics", "YouTube", "YouTubeSubtitle")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
