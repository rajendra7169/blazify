/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.content.Context
import com.blazify.innertube.models.BrowseEndpoint
import com.blazify.music.constants.BrowseArtKey
import com.blazify.music.constants.BrowseArtSavedAtKey
import kotlinx.coroutines.flow.first

/**
 * The picture on each tile of Search > Browse. YouTube's moods and genres come with a colour
 * but no picture, so each tile borrows the cover of the first playlist inside it, Charts the
 * cover of the song at number one, and New releases the newest album's. Finding one costs a
 * request of about 100 KB, a dozen of them over a megabyte, so the choices are kept for a week:
 * after the first time the tiles cost nothing to show. The images themselves sit in the
 * image cache like any other cover.
 */
object BrowseArt {
    const val CHARTS = "charts"
    const val NEW_RELEASES = "new_releases"

    private const val KEEP_MS = 7L * 24 * 60 * 60 * 1000

    fun keyOf(endpoint: BrowseEndpoint) = "${endpoint.browseId}|${endpoint.params}"

    /** The pictures kept, and when they were first chosen; nothing once a week has passed. */
    suspend fun saved(context: Context): Pair<Map<String, String>, Long> {
        val now = System.currentTimeMillis()
        val prefs = context.dataStore.data.first()
        val savedAt = prefs[BrowseArtSavedAtKey] ?: return emptyMap<String, String>() to now
        if (now - savedAt > KEEP_MS) return emptyMap<String, String>() to now
        val art =
            prefs[BrowseArtKey].orEmpty().lineSequence()
                .mapNotNull { line -> line.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }
                .toMap()
        return art to savedAt
    }

    /** Keeps [art], dated [savedAt] so that adding a missing tile does not put off the weekly refresh. */
    suspend fun save(context: Context, art: Map<String, String>, savedAt: Long) {
        context.safeDataStoreEdit { prefs ->
            prefs[BrowseArtKey] = art.entries.joinToString("\n") { "${it.key}\t${it.value}" }
            prefs[BrowseArtSavedAtKey] = savedAt
        }
    }
}
