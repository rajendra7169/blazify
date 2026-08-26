/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * The release notes for this build, carried inside it.
 *
 * The changelog used to be nothing but a request to a code-hosting site: no
 * connection, no notes — and on a repository that is not public, no notes ever,
 * because an unauthenticated reader is told the releases do not exist. That
 * turns a page somebody opened deliberately into an empty box with no
 * explanation, and it fails in exactly the situation where reading what changed
 * matters most: a build somebody has just been handed and is not sure about.
 *
 * What shipped is known at the moment it ships, so it travels with it. The site
 * is still asked, and anything it returns that is not already here is added —
 * it just is no longer the only source.
 */
object BundledChangelog {

    private const val ASSET = "changelog.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Entry(
        val tagName: String,
        val versionName: String,
        val releaseDate: String,
        val description: String,
    )

    /** Newest first. Empty if the asset is missing or unreadable — never an error. */
    suspend fun read(context: Context): List<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            json.decodeFromString<List<Entry>>(text).map {
                ReleaseInfo(
                    tagName = it.tagName,
                    versionName = it.versionName,
                    description = it.description,
                    releaseDate = it.releaseDate,
                    assets = emptyList(),
                )
            }.sortedWith { a, b -> Updater.compareVersions(b.tagName, a.tagName) }
        }.onFailure {
            Timber.tag("BundledChangelog").w(it, "Could not read the bundled release notes")
        }.getOrDefault(emptyList())
    }
}
