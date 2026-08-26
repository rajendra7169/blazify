/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import com.blazify.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String // "foss" or "gms"
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set
    
    private var cachedReleaseInfo: ReleaseInfo? = null
    private var cachedAllReleases: List<ReleaseInfo> = emptyList()
    
    private const val CHECK_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L // 2 hours
    private const val GITHUB_API_BASE = "https://api.github.com/repos/rajendra7169/blazify"

    /**
     * Compares two version strings.
     * Returns: 1 if v1 > v2, -1 if v1 < v2, 0 if equal
     */
    fun compareVersions(v1: String, v2: String): Int {
        val v1Parts = v1.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = v2.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until maxLength) {
            val part1 = v1Parts.getOrNull(i) ?: 0
            val part2 = v2Parts.getOrNull(i) ?: 0
            when {
                part1 > part2 -> return 1
                part1 < part2 -> return -1
            }
        }
        return 0
    }

    /**
     * Checks if the latest version is newer than the current version.
     * Returns true if an update is available (latestVersion > currentVersion)
     */
    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }

    /**
     * Get the current app's architecture and variant
     */
    private fun getCurrentAppVariant(): Pair<String, String> {
        val architecture = BuildConfig.ARCHITECTURE
        val variant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"
        return architecture to variant
    }

    /**
     * Read the downloadable builds off a release.
     *
     * This used to insist on a handful of exact filenames inherited from the
     * project this was forked from, and recognised none of the names actually
     * published here — so every release looked like a release with nothing in
     * it, and the updater had nothing to offer however well it worked.
     *
     * Any .apk is a candidate now. The name is read for hints about which build
     * it is, and anything it does not recognise is treated as the ordinary
     * universal one, because a build nobody can identify is still better offered
     * than silently dropped.
     */
    private fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()

        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue

            val lower = name.lowercase()
            val variant = when {
                "google-cast" in lower || "-gms" in lower -> "gms"
                else -> "foss"
            }
            val arch = ARCHITECTURES.firstOrNull { it in lower } ?: "universal"

            assets.add(
                ReleaseAsset(
                    name = name,
                    downloadUrl = asset.getString("browser_download_url"),
                    size = asset.getLong("size"),
                    architecture = arch,
                    variant = variant,
                ),
            )
        }

        return assets
    }

    private val ARCHITECTURES = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

    /**
     * The build to offer somebody, out of whatever a release happens to carry.
     *
     * Preferring an exact match on architecture and variant, then the universal
     * build, then simply the first one there — a release with a single APK on it
     * is the common case and should not need to match anything to be offered.
     */
    fun pickAsset(assets: List<ReleaseAsset>): ReleaseAsset? {
        if (assets.isEmpty()) return null
        val (arch, variant) = getCurrentAppVariant()
        return assets.firstOrNull { it.architecture == arch && it.variant == variant }
            ?: assets.firstOrNull { it.architecture == "universal" && it.variant == variant }
            ?: assets.firstOrNull { it.variant == variant }
            ?: assets.first()
    }

    /**
     * Fetch latest release from GitHub API
     */
    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Return cached if available and not forcing refresh
                if (cachedReleaseInfo != null && !forceRefresh) {
                    return@runCatching cachedReleaseInfo!!
                }
                
                val response = client.get("$GITHUB_API_BASE/releases/latest")
                    .bodyAsText()
                val json = JSONObject(response)
                
                val tag = json.getString("tag_name")
                val releaseInfo = ReleaseInfo(
                    tagName = tag,
                    // The version, taken from the tag rather than from the
                    // release headline. The headline is prose — "Blazify 9.12.3"
                    // — and comparing it as a version read the first component
                    // as zero, so every release looked older than the build
                    // asking about it and no update was ever offered. It is also
                    // what the About page compared against to decide whether to
                    // show its badge, which is why the badge was always on.
                    versionName = tag.removePrefix("v"),
                    description = json.getString("body"),
                    releaseDate = json.getString("published_at"),
                    assets = parseAssets(json.getJSONArray("assets"))
                )
                
                cachedReleaseInfo = releaseInfo
                lastCheckTime = System.currentTimeMillis()
                releaseInfo
            }
        }

    /**
     * Fetch all releases from GitHub API (paginated)
     */
    suspend fun getAllReleases(forceRefresh: Boolean = false): Result<List<ReleaseInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }
                
                val releases = mutableListOf<ReleaseInfo>()
                var page = 1
                var hasMore = true
                
                while (hasMore && page <= 10) { // Limit to 10 pages
                    val response = client.get("$GITHUB_API_BASE/releases?page=$page&per_page=30")
                        .bodyAsText()
                    val json = JSONArray(response)
                    
                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }
                    
                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        releases.add(ReleaseInfo(
                            tagName = releaseObj.getString("tag_name"),
                            versionName = releaseObj.getString("tag_name").removePrefix("v"),
                            description = releaseObj.getString("body"),
                            releaseDate = releaseObj.getString("published_at"),
                            assets = parseAssets(releaseObj.getJSONArray("assets"))
                        ))
                    }
                    
                    page++
                }
                
                cachedAllReleases = releases
                releases
            }
        }

    /**
     * Get the download URL for the correct app variant
     */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val (currentArch, currentVariant) = getCurrentAppVariant()
        
        return releaseInfo.assets
            .find { it.architecture == currentArch && it.variant == currentVariant }
            ?.downloadUrl
    }

    /**
     * Get all available download URLs for a release
     */
    fun getAllDownloadUrls(releaseInfo: ReleaseInfo): Map<String, String> {
        return releaseInfo.assets.associate { "${it.architecture}-${it.variant}" to it.downloadUrl }
    }

    /**
     * Check if update is needed (respects 2-hour cache)
     */
    suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Check if we should fetch (2 hour interval)
                val shouldFetch = forceRefresh || 
                    (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS
                
                if (!shouldFetch && cachedReleaseInfo != null) {
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.VERSION_NAME,
                        cachedReleaseInfo!!.versionName
                    )
                    return@runCatching cachedReleaseInfo!! to hasUpdate
                }
                
                val result = getLatestRelease(forceRefresh = true)
                if (result.isSuccess) {
                    val releaseInfo = result.getOrThrow()
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.VERSION_NAME,
                        releaseInfo.versionName
                    )
                    releaseInfo to hasUpdate
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
            }
        }

    /**
     * Get the download URL for the correct app variant
     * Returns null if no matching asset is found
     */
    fun getLatestDownloadUrl(): String? {
        return cachedReleaseInfo?.let { getDownloadUrlForCurrentVariant(it) }
    }
    
    /**
     * Get the latest release info (cached)
     */
    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo
}