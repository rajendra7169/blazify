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
                // duration+title matching) beat results from FUZZY keyword sources
                // (KuGou/LyricsPlus). Within each tier, synced > plain — but a synced
                // timeline must PLAUSIBLY fit this song to stay synced: a trusted
                // result whose timestamps don't fit is a different EDIT of the right
                // song (video vs album cut) and scrolls visibly out of sync — the
                // singer sings while the interlude logo fills. Its TEXT is still
                // trustworthy, so keep it with the timestamps STRIPPED (plain).
                var trustedSynced: LyricsWithProvider? = null
                var trustedPlain: LyricsWithProvider? = null
                var fuzzySynced: LyricsWithProvider? = null
                var fuzzyPlain: LyricsWithProvider? = null
                for ((index, attempt) in attempts.withIndex()) {
                    val providerName = enabledProviders[index].name
                    val providerResult = attempt.await()
                    if (providerResult != null && providerResult.isSuccess) {
                        val raw = providerResult.getOrNull()!!
                        val plausible = isPlausible(raw, mediaMetadata.duration)
                        if (providerName !in TRUSTED_PROVIDERS && !plausible) {
                            // Implausible FUZZY results are wrong-song matches — drop.
                            Timber.tag("LyricsHelper").w("$providerName returned implausible lyrics (timeline/duration mismatch) — skipping")
                            continue
                        }
                        val filtered = LyricsUtils.filterLyricsCreditLines(raw)
                        val synced = isSynced(filtered)
                        val trusted = providerName in TRUSTED_PROVIDERS
                        Timber.tag("LyricsHelper").i("Got ${if (synced) "SYNCED" else "plain"} lyrics from $providerName (${if (trusted) "trusted" else "fuzzy"}${if (synced && !plausible) ", DRIFTED timeline" else ""})")
                        when {
                            trusted && synced && !plausible -> {
                                // Right song, wrong edit: demote to plain text.
                                Timber.tag("LyricsHelper").w(
                                    "$providerName timeline doesn't fit (last ts ${lastTimestampSec(filtered)}s vs song ${mediaMetadata.duration}s) — keeping text as PLAIN"
                                )
                                val plainText = stripTimestamps(filtered)
                                if (plainText.isNotBlank() && trustedPlain == null) {
                                    trustedPlain = LyricsWithProvider(plainText, providerName)
                                }
                            }
                            trusted && synced && trustedSynced == null -> trustedSynced = LyricsWithProvider(filtered, providerName)
                            trusted && !synced && trustedPlain == null -> trustedPlain = LyricsWithProvider(filtered, providerName)
                            !trusted && synced && fuzzySynced == null -> fuzzySynced = LyricsWithProvider(filtered, providerName)
                            !trusted && !synced && fuzzyPlain == null -> fuzzyPlain = LyricsWithProvider(filtered, providerName)
                        }
                        // Best possible outcome reached — stop early.
                        if (trustedSynced != null) break
                    }
                }
                // Cancel any still-running lower-priority attempts.
                attempts.forEach { it.cancel() }

                val chosen = trustedSynced ?: trustedPlain ?: fuzzySynced ?: fuzzyPlain
                if (chosen == null) {
                    Timber.tag("LyricsHelper").w("No lyrics found after racing all providers")
                } else {
                    Timber.tag("LyricsHelper").i("Chose lyrics from ${chosen.provider}")
                }
                chosen ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
            }
        }

        return result ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
    }

    /**
     * Best-effort SYNCED-only fetch, used to upgrade a song that is currently
     * showing plain lyrics. Races only the TRUSTED providers like [getLyrics],
     * but returns a result only when it actually carries timestamps AND its
     * timeline plausibly fits the song — so a correct plain lyric is never
     * replaced by a wrong scrolling one. Returns null when no trustworthy synced
     * version exists (the plain lyrics then stay in place).
     */
    suspend fun getSyncedLyricsOrNull(mediaMetadata: MediaMetadata): LyricsWithProvider? {
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        if (!isNetworkAvailable) return null

        return withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
            val syncedCapableProviders = context.dataStore.data
                .map { preferences -> resolveLyricsProviders(preferences) }
                .first()
                .filter { it.isEnabled(context) && it.name in TRUSTED_PROVIDERS }
                // LrcLib matches by title+artist+DURATION, so its timeline fits the
                // edit that is actually playing — prefer it for upgrades. (Stable
                // sort keeps the user's order for the rest.)
                .sortedByDescending { it.name == "LrcLib" }

            if (syncedCapableProviders.isEmpty()) return@withTimeoutOrNull null

            Timber.tag("LyricsHelper").d("Synced-upgrade: racing ${syncedCapableProviders.size} trusted providers for: $cleanedTitle")

            coroutineScope {
                val attempts = syncedCapableProviders.map { provider ->
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
                            null
                        }
                    }
                }

                // Take the highest-priority provider that returns a real, plausible
                // synced result.
                var best: LyricsWithProvider? = null
                for ((index, attempt) in attempts.withIndex()) {
                    val providerName = syncedCapableProviders[index].name
                    val providerResult = attempt.await()
                    if (providerResult != null && providerResult.isSuccess) {
                        val filtered = LyricsUtils.filterLyricsCreditLines(providerResult.getOrNull()!!)
                        if (isSynced(filtered) && fitsTimelineTightly(filtered, mediaMetadata.duration)) {
                            Timber.tag("LyricsHelper").i("Synced-upgrade: found synced lyrics from $providerName")
                            best = LyricsWithProvider(filtered, providerName)
                            break
                        }
                    }
                }
                attempts.forEach { it.cancel() }
                best
            }
        }
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

    /**
     * Drop LRC timestamps but keep the line text — used when a synced timeline
     * doesn't fit the edit that is actually playing. The text then scrolls with
     * the plain-lyrics estimate instead of a wrong karaoke timeline.
     */
    private fun stripTimestamps(lyrics: String): String =
        try {
            LyricsUtils.parseLyrics(lyrics).joinToString("\n") { it.text }.trim()
        } catch (e: Exception) {
            ""
        }

    /** Last LRC timestamp in seconds, for diagnostics. */
    private fun lastTimestampSec(lyrics: String): Long =
        try {
            LyricsUtils.parseLyrics(lyrics).maxOf { it.time } / 1000
        } catch (e: Exception) {
            -1
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
     * Stricter timeline check used ONLY for the synced-upgrade path: an upgrade
     * REPLACES already-correct plain lyrics, so the candidate's last timestamp
     * must land close to the end of what is actually playing (60% .. +10s).
     * A drifting upgrade would be a downgrade.
     */
    private fun fitsTimelineTightly(lyrics: String, durationSec: Int): Boolean {
        if (durationSec <= 0) return false
        val entries = try {
            LyricsUtils.parseLyrics(lyrics)
        } catch (e: Exception) {
            return false
        }
        if (entries.isEmpty()) return false
        val lastMs = entries.maxOf { it.time }
        val durationMs = durationSec * 1000L
        return lastMs >= (durationMs * 0.6).toLong() && lastMs <= durationMs + 10_000L
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
