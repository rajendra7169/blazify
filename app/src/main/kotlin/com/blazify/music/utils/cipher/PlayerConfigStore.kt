package com.blazify.music.utils.cipher

import android.content.Context
import android.util.Base64
import com.blazify.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Owns the player-config table at runtime: bundled asset as the offline default, overlaid
 * by the same JSON fetched from remote sources so rotated players are fixed without
 * an APK update. Parsing/validation is delegated to [PlayerConfigParser]; only validated
 * payloads ever replace the in-memory map or touch the disk cache.
 *
 * Read path is lock-free: lookups hit an immutable map behind a @Volatile reference that
 * refreshes swap wholesale.
 */
object PlayerConfigStore {
    private const val TAG = "Blazify_CipherConfig"
    private const val ASSET_NAME = "player_configs.json"

    /**
     * Where remote tables come from, in the order they are asked.
     *
     * [OWN] is our copy on the player-configs branch, kept in step with the upstream registry
     * by a scheduled job, so playback doesn't depend on someone else's repository staying where
     * it is. [MIRROR] is the same file through a CDN, for networks that block raw GitHub.
     * [UPSTREAM] is the registry itself, asked when our copy can't be read or doesn't know the
     * player yet: the hours before the job catches up, or if the job ever stops.
     *
     * Each source keeps its own cached body and ETag, and the table is the bundled asset with
     * every cached source laid over it, so a source that is behind can never take away a
     * player another one already taught.
     */
    internal enum class Source(private val encodedUrl: String, val cacheName: String) {
        OWN(
            "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3JhamVuZHJhNzE2OS9ibGF6aWZ5L3BsYXllci1jb25maWdzL3BsYXllcl9jb25maWdzLmpzb24=",
            "configs_own",
        ),
        MIRROR(
            "aHR0cHM6Ly9jZG4uanNkZWxpdnIubmV0L2doL3JhamVuZHJhNzE2OS9ibGF6aWZ5QHBsYXllci1jb25maWdzL3BsYXllcl9jb25maWdzLmpzb24=",
            "configs_mirror",
        ),

        // Keeps the file names from when this was the only source, so a copy cached back then still loads.
        UPSTREAM(
            "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL01ldHJvbGlzdEdyb3VwL2ZhcmFkYXkvbWFzdGVyL3JlZ2lzdHJ5L3BsYXllcl9jb25maWdzLmpzb24=",
            "configs_remote",
        ),
        ;

        val url: String by lazy { String(Base64.decode(encodedUrl, Base64.DEFAULT), StandardCharsets.UTF_8) }
    }

    // Lowest first. Our copy goes on top: when it disagrees with upstream it is either a fix made
    // on purpose or a few hours behind, while upstream's cache may be all that is left of a file
    // that has since moved. The mirror trails our copy, so it sits just under it.
    private val OVERLAY_ORDER = listOf(Source.UPSTREAM, Source.MIRROR, Source.OWN)

    // Mirrors PlayerJsFetcher.CACHE_TTL_MS.
    private const val REFRESH_TTL_MS = 6 * 60 * 60 * 1000L

    // Failure-triggered refreshes are rate-limited so a player that is unknown both locally
    // and remotely doesn't turn every song into a GitHub request.
    private const val FORCE_REFRESH_COOLDOWN_MS = 5 * 60 * 1000L

    // Note: cache names must not start with "player_" — PlayerJsFetcher.writeToCache() purges
    // "player_*" from this shared dir on every player-JS refresh. PlayerJsFetcher.invalidateCache()
    // deletes only player_*/current_hash.txt, so the config caches + ETags survive cipher retries.

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var bundledConfigs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    @Volatile
    private var remoteTables: Map<Source, Map<String, FunctionNameExtractor.HardcodedPlayerConfig>> = emptyMap()

    @Volatile
    private var mergedConfigs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    // Advanced every time a remote refresh actually changes the table. The cipher records the epoch
    // its WebView was built under and rebuilds when this advances, so a corrected config for the
    // current player — arriving by any refresh path AFTER the WebView was built from a missing or
    // wrong entry — takes effect on the next decipher instead of being ignored until the process is
    // restarted. See CipherDeobfuscator.getOrCreateWebView().
    @Volatile
    var configEpoch: Int = 0
        private set

    @Volatile
    private var lastForcedAttemptMs = 0L

    // Separate from lastForcedAttemptMs so a stream-rejection refresh (which fires on any 403,
    // including unrelated/expired-URL ones) can never arm a cooldown that blocks the unknown-hash
    // forceRefresh self-heal, or vice versa. They still serialize on refreshMutex (single-flight).
    @Volatile
    private var lastRejectionAttemptMs = 0L

    // The two cooldown gates read DIFFERENT stamps on purpose (see above). Routed through these
    // functions — which forceRefresh / refreshAfterStreamRejection actually call — so a unit test
    // can prove neither path is gated by the other's cooldown without touching the network.
    /**
     * True iff [stampMs] lies within [windowMs] of [now]. The in-range check (not a plain
     * `now - stamp < window`) matters: these are wall-clock stamps, and a backward clock
     * adjustment (NTP correction, manual change) makes the delta negative — a plain
     * less-than would then hold the window for the entire skew duration, wedging
     * cooldowns/TTLs exactly while playback is broken. Every wall-clock window check in
     * this module MUST go through this helper.
     */
    internal fun withinWindow(now: Long, stampMs: Long, windowMs: Long) =
        (now - stampMs) in 0 until windowMs

    internal fun forcedCooldownActive(now: Long) = withinWindow(now, lastForcedAttemptMs, FORCE_REFRESH_COOLDOWN_MS)

    internal fun rejectionCooldownActive(now: Long) = withinWindow(now, lastRejectionAttemptMs, FORCE_REFRESH_COOLDOWN_MS)

    // Test-only: arm a cooldown stamp without invoking the network refresh paths.
    internal fun armForcedCooldownForTest(ms: Long) { lastForcedAttemptMs = ms }

    internal fun armRejectionCooldownForTest(ms: Long) { lastRejectionAttemptMs = ms }

    private val refreshMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Built once on first use (mirrors PlayerJsFetcher's single client) so config refreshes
    // reuse one connection pool/dispatcher instead of allocating a fresh client per fetch.
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { YouTube.proxy?.let { proxy(it) } }
            .build()
    }

    /**
     * Synchronous: loads the bundled asset and, where present and valid, each source's
     * last-good cached copy. Cheap (a small asset + at most a few small files) and
     * guarantees configs exist before any lookup.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext

        bundledConfigs = when (val result = parseSource("bundled asset") { loadBundledJson(context) }) {
            null -> emptyMap()
            else -> result
        }
        if (bundledConfigs.isEmpty()) {
            Timber.tag(TAG).e("Bundled $ASSET_NAME missing or invalid — config table starts empty")
        } else {
            Timber.tag(TAG).d("Loaded bundled configs (${bundledConfigs.size} hashes)")
        }

        applyCachedOverlay()
    }

    /**
     * Lays every source's last-good cached copy over the bundled table. On ANY failure to load
     * one, that source's body AND meta file are deleted together: an ETag surviving a
     * corrupt/missing body would make every later conditional fetch 304 without a re-download,
     * locking that source out until its remote content happens to change.
     * (No-op deletes on a clean first run.)
     */
    internal fun applyCachedOverlay() {
        val tables = buildMap {
            for (source in Source.entries) {
                val cached = parseSource("cached ${source.name} copy") {
                    cacheFile(source)?.takeIf { it.exists() }?.readText()
                }
                if (cached != null) {
                    Timber.tag(TAG).d("Overlaying cached ${source.name} configs (${cached.size} hashes)")
                    put(source, cached)
                } else {
                    cacheFile(source)?.delete()
                    metaFile(source)?.delete()
                }
            }
        }
        remoteTables = tables
        mergedConfigs = overlay(tables)
    }

    private fun overlay(
        tables: Map<Source, Map<String, FunctionNameExtractor.HardcodedPlayerConfig>>,
    ): Map<String, FunctionNameExtractor.HardcodedPlayerConfig> =
        OVERLAY_ORDER.fold(bundledConfigs) { table, source ->
            tables[source]?.let { PlayerConfigParser.merge(table, it) } ?: table
        }

    /** Non-blocking TTL-gated refresh, kicked once from CipherDeobfuscator.initialize(). */
    fun scheduleStartupRefresh() {
        scope.launch {
            try {
                refreshIfStale()
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Startup config refresh failed: ${e.message}")
            }
        }
    }

    fun get(hash: String): FunctionNameExtractor.HardcodedPlayerConfig? {
        val configs = mergedConfigs
        if (configs.isEmpty()) {
            Timber.tag(TAG).w("Config table is empty (initialize not called or bundled asset broken)")
        }
        return configs[hash]
    }

    fun knownHashes(): Set<String> = mergedConfigs.keys

    /**
     * A cheap summary of which players this table can teach.
     *
     * Used to date a verdict recorded against a player: the verdict is only
     * worth keeping while the table that produced it is the same table. Any
     * refresh that adds or drops an entry changes this, and the verdict with it.
     */
    fun tableFingerprint(): String {
        val keys = mergedConfigs.keys
        return "${keys.size}:${keys.sorted().joinToString(",").hashCode()}"
    }

    /** Test-only: swaps the in-memory table without touching disk, context, or network. */
    internal fun setTableForTest(configs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig>) {
        remoteTables = emptyMap()
        mergedConfigs = configs
    }

    /**
     * Failure-triggered refresh: called when a player-hash lookup misses. Single-flight, with
     * the cooldown decided under the lock (a check-then-set outside it would let concurrent
     * misses race). Returns true iff [missingHash] is now in the table — whether THIS call's
     * fetch did the work or a concurrent/just-finished refresh already brought it in — so
     * callers retry extraction exactly when it can succeed.
     */
    suspend fun forceRefresh(missingHash: String): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            // A refresh that held the lock while we waited (startup TTL, another miss) may
            // have just landed this config — don't burn a fetch or arm the cooldown.
            if (mergedConfigs.containsKey(missingHash)) {
                Timber.tag(TAG).d("forceRefresh: $missingHash arrived via concurrent refresh")
                return@withLock true
            }

            val now = System.currentTimeMillis()
            if (forcedCooldownActive(now)) {
                Timber.tag(TAG).d("forceRefresh skipped (cooldown)")
                return@withLock false
            }
            lastForcedAttemptMs = now
            refreshFromSources(missingHash = missingHash, resetCooldown = { lastForcedAttemptMs = 0L })
            mergedConfigs.containsKey(missingHash)
        }
    }

    /**
     * [forceRefresh] without waiting for it, for a caller that has already moved on without
     * [missingHash] and only wants a later attempt to find it. Same cooldown, same single flight.
     */
    fun refreshInBackground(missingHash: String) {
        if (mergedConfigs.containsKey(missingHash)) return
        scope.launch {
            try {
                forceRefresh(missingHash)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Background config refresh failed: ${e.message}")
            }
        }
    }

    /**
     * Stream-rejection refresh: a deciphered URL was rejected by the CDN (e.g. a WEB_REMIX 403),
     * which can mean the cipher produced a wrong-but-non-throwing signature from a stale/wrong
     * player config — a failure the [CipherDeobfuscator] exception-retry never sees. Unlike
     * [forceRefresh], this does NOT short-circuit when the current hash is already present (the
     * entry may be present but WRONG), so it always re-fetches, from every source; it has its OWN
     * cooldown (so it can't starve the unknown-hash [forceRefresh] path) but shares the
     * single-flight lock. Returns whether the table changed — when true, [configEpoch] has advanced
     * and the cipher rebuilds.
     */
    suspend fun refreshAfterStreamRejection(): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val now = System.currentTimeMillis()
            if (rejectionCooldownActive(now)) {
                Timber.tag(TAG).d("refreshAfterStreamRejection skipped (cooldown)")
                return@withLock false
            }
            lastRejectionAttemptMs = now
            refreshFromSources(askEverySource = true, resetCooldown = { lastRejectionAttemptMs = 0L })
        }
    }

    private suspend fun refreshIfStale() {
        // Stamps are persisted, so a future one (wall clock stepped back after the write) must
        // count as stale, not fresh — withinWindow handles that.
        val lastFetchMs = Source.entries.maxOf { readMeta(it)?.second ?: 0L }
        if (withinWindow(System.currentTimeMillis(), lastFetchMs, REFRESH_TTL_MS)) {
            Timber.tag(TAG).d("Remote configs fresh (fetched ${System.currentTimeMillis() - lastFetchMs} ms ago)")
            return
        }
        withContext(Dispatchers.IO) {
            refreshMutex.withLock { refreshFromSources() }
        }
    }

    /**
     * Asks the sources in order and stops once the table has what was needed. The mirror is only
     * asked when our copy couldn't be read; upstream when neither could, when the table still
     * lacks [missingHash], or when [askEverySource]. If no source was reached at all (offline),
     * [resetCooldown] lets the next trigger retry instead of waiting out the cooldown, which exists
     * to spare the hosts, not to delay recovery. Returns whether the table changed.
     * Must be called with [refreshMutex] held.
     */
    private fun refreshFromSources(
        missingHash: String? = null,
        askEverySource: Boolean = false,
        resetCooldown: () -> Unit = {},
    ): Boolean {
        var changed = false
        var reachedServer = false
        var ourCopyRead = false
        for (source in Source.entries) {
            val skip = when (source) {
                Source.OWN -> false
                Source.MIRROR -> ourCopyRead
                Source.UPSTREAM ->
                    ourCopyRead && !askEverySource && (missingHash == null || mergedConfigs.containsKey(missingHash))
            }
            if (skip) continue

            val result = fetchAndApply(source)
            reachedServer = reachedServer || result.reachedServer
            changed = changed || result.changed
            if (source != Source.UPSTREAM && result.read) ourCopyRead = true
        }
        if (!reachedServer) resetCooldown()
        return changed
    }

    private class FetchResult(val reachedServer: Boolean, val read: Boolean, val changed: Boolean)

    /**
     * Fetches one source's JSON (with If-None-Match) and applies it when valid. Any failure —
     * HTTP error (including a 404 from a source that has moved), network exception, or
     * validation failure — keeps that source's previous table and cache. The fetch stamp is
     * only advanced on 200/304 so transient failures retry on the next trigger.
     */
    private fun fetchAndApply(source: Source): FetchResult {
        try {
            val etag = readMeta(source)?.first
            val request = Request.Builder()
                .url(sourceUrlsForTest?.get(source) ?: source.url)
                .header("User-Agent", "Mozilla/5.0")
                .apply { if (!etag.isNullOrEmpty()) header("If-None-Match", etag) }
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 304) {
                    Timber.tag(TAG).d("${source.name} configs unchanged (304)")
                    writeMeta(source, etag.orEmpty(), System.currentTimeMillis())
                    return FetchResult(reachedServer = true, read = true, changed = false)
                }
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w("${source.name} config fetch HTTP ${response.code} — keeping previous configs")
                    return FetchResult(reachedServer = true, read = false, changed = false)
                }

                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    Timber.tag(TAG).w("${source.name} config fetch returned empty body — keeping previous configs")
                    return FetchResult(reachedServer = true, read = false, changed = false)
                }

                val remote = when (val result = PlayerConfigParser.parse(body)) {
                    is PlayerConfigParser.ParseResult.Failure -> {
                        Timber.tag(TAG).w("${source.name} configs rejected: ${result.reason} — keeping previous configs")
                        return FetchResult(reachedServer = true, read = false, changed = false)
                    }
                    is PlayerConfigParser.ParseResult.Success -> {
                        if (result.skippedEntries.isNotEmpty()) {
                            Timber.tag(TAG).w("${source.name} configs: skipped invalid entries ${result.skippedEntries}")
                        }
                        result.configs
                    }
                }

                val changed = applyRemote(remote, body, response.header("ETag").orEmpty(), source)
                return FetchResult(reachedServer = true, read = true, changed = changed)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "${source.name} config fetch failed: ${e.message} — keeping previous configs")
            return FetchResult(reachedServer = false, read = false, changed = false)
        }
    }

    /**
     * Applies a validated table from [source] to memory FIRST, then best-effort persists the raw
     * body + meta. A disk failure (full disk, IO error) must never discard an in-hand
     * validated fix — losing the cache only costs a refetch on the next start, while losing
     * the memory update costs working playback now. Returns whether the table changed.
     */
    internal fun applyRemote(
        remote: Map<String, FunctionNameExtractor.HardcodedPlayerConfig>,
        body: String,
        etag: String,
        source: Source = Source.UPSTREAM,
    ): Boolean {
        val tables = remoteTables + (source to remote)
        val merged = overlay(tables)
        val changed = merged != mergedConfigs
        remoteTables = tables
        mergedConfigs = merged
        if (changed) configEpoch++
        Timber.tag(TAG).d("${source.name} configs applied (${remote.size} hashes, merged=${merged.size}, changed=$changed, epoch=$configEpoch)")

        try {
            cacheFile(source)?.let { writeAtomic(it, body) }
            writeMeta(source, etag, System.currentTimeMillis())
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not persist ${source.name} configs (kept in memory): ${e.message}")
        }
        return changed
    }

    private fun parseSource(
        label: String,
        read: () -> String?,
    ): Map<String, FunctionNameExtractor.HardcodedPlayerConfig>? {
        val text = try {
            read() ?: return null
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not read $label: ${e.message}")
            return null
        }
        return when (val result = PlayerConfigParser.parse(text)) {
            is PlayerConfigParser.ParseResult.Failure -> {
                Timber.tag(TAG).w("Rejected $label: ${result.reason}")
                null
            }
            is PlayerConfigParser.ParseResult.Success -> {
                if (result.skippedEntries.isNotEmpty()) {
                    Timber.tag(TAG).w("$label: skipped invalid entries ${result.skippedEntries}")
                }
                result.configs
            }
        }
    }

    private fun loadBundledJson(context: Context): String? =
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }

    // Test seam: unit tests point this at a temp dir; production resolves from appContext.
    internal var cacheDirForTest: File? = null

    // Test seam: unit tests point the sources at a local server.
    internal var sourceUrlsForTest: Map<Source, String>? = null

    private fun cacheDir(): File? {
        cacheDirForTest?.let { return it.apply { if (!exists()) mkdirs() } }
        val context = appContext ?: return null
        return File(context.filesDir, "cipher_cache").apply { if (!exists()) mkdirs() }
    }

    private fun cacheFile(source: Source): File? = cacheDir()?.let { File(it, "${source.cacheName}.json") }

    private fun metaFile(source: Source): File? = cacheDir()?.let { File(it, "${source.cacheName}.meta") }

    /** Meta file: line 1 = ETag (may be empty), line 2 = lastFetchMs. */
    private fun readMeta(source: Source): Pair<String, Long>? {
        return try {
            val file = metaFile(source)?.takeIf { it.exists() } ?: return null
            val lines = file.readText().split("\n")
            if (lines.size < 2) return null
            val lastFetchMs = lines[1].toLongOrNull() ?: return null
            lines[0] to lastFetchMs
        } catch (e: Exception) {
            null
        }
    }

    private fun writeMeta(source: Source, etag: String, lastFetchMs: Long) {
        try {
            metaFile(source)?.let { writeAtomic(it, "$etag\n$lastFetchMs") }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not write ${source.name} config meta: ${e.message}")
        }
    }

    /**
     * Temp-file + rename so a process death mid-write can't leave a truncated file (a
     * corrupt cache body beside a valid ETag is exactly the 304-lock state
     * [applyCachedOverlay] defends against).
     */
    internal fun writeAtomic(file: File, content: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(file)) {
            // renameTo won't overwrite an existing target on some filesystems — retry after
            // deleting it (two cheap metadata ops, still atomic) before the last-resort direct
            // write, which is both non-atomic and a second full write of the content.
            file.delete()
            if (!tmp.renameTo(file)) {
                file.writeText(content)
                tmp.delete()
            }
        }
    }
}
