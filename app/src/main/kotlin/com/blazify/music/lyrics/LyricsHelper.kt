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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

private const val MAX_LYRICS_FETCH_MS = 15000L
private const val PER_PROVIDER_TIMEOUT_MS = 8000L
private const val PROVIDER_NONE = ""

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

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
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

                // Prefer SYNCED (scrolling) lyrics: take the first valid synced result
                // in priority order; remember the best plain result only as a fallback
                // for songs where no source has a synced match.
                var synced: LyricsWithProvider? = null
                var plainFallback: LyricsWithProvider? = null
                for ((index, attempt) in attempts.withIndex()) {
                    val providerResult = attempt.await()
                    if (providerResult != null && providerResult.isSuccess) {
                        val raw = providerResult.getOrNull()!!
                        // Reject results whose synced timeline doesn't fit this song —
                        // that's the signature of a wrong-song fuzzy match.
                        if (!isPlausible(raw, mediaMetadata.duration)) {
                            Timber.tag("LyricsHelper").w("${enabledProviders[index].name} returned implausible lyrics (timeline/duration mismatch) — skipping")
                            continue
                        }
                        val filtered = LyricsUtils.filterLyricsCreditLines(raw)
                        if (isSynced(filtered)) {
                            Timber.tag("LyricsHelper").i("Got SYNCED lyrics from ${enabledProviders[index].name}")
                            synced = LyricsWithProvider(filtered, enabledProviders[index].name)
                            break
                        }
                        if (plainFallback == null) {
                            Timber.tag("LyricsHelper").i("Got plain lyrics from ${enabledProviders[index].name} — holding as fallback, still looking for synced")
                            plainFallback = LyricsWithProvider(filtered, enabledProviders[index].name)
                        }
                    }
                }
                // Cancel any still-running lower-priority attempts.
                attempts.forEach { it.cancel() }

                val chosen = synced ?: plainFallback
                if (chosen == null) Timber.tag("LyricsHelper").w("No lyrics found after racing all providers")
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
