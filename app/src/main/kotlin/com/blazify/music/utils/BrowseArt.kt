/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import android.content.Context
import com.blazify.innertube.models.BrowseEndpoint
import com.blazify.music.constants.BrowseArtKey
import com.blazify.music.constants.BrowseArtSavedAtKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * The covers fanned on each tile of Search > Browse. YouTube's moods and genres come with a
 * colour but no picture, so each tile borrows the covers of the first playlists inside it,
 * Charts those of the songs at the top, and New releases the newest albums'. Finding them costs
 * a request of about 100 KB per tile, over a megabyte for the grid, so the choices are kept for
 * a week: after the first time the tiles cost nothing to show. The images themselves sit in the
 * image cache like any other cover.
 */
object BrowseArt {
    const val CHARTS = "charts"
    const val NEW_RELEASES = "new_releases"

    private const val KEEP_MS = 7L * 24 * 60 * 60 * 1000

    /** How many covers a tile fans out. */
    const val COVERS = 3

    fun keyOf(endpoint: BrowseEndpoint) = "${endpoint.browseId}|${endpoint.params}"

    /** The covers kept, and when they were first chosen; nothing once a week has passed. */
    suspend fun saved(context: Context): Pair<Map<String, List<String>>, Long> {
        val now = System.currentTimeMillis()
        val prefs = context.dataStore.data.first()
        val savedAt = prefs[BrowseArtSavedAtKey] ?: return emptyMap<String, List<String>>() to now
        if (now - savedAt > KEEP_MS) return emptyMap<String, List<String>>() to now
        // One tile a line: its key, a tab, then its covers' addresses split by spaces.
        val art =
            prefs[BrowseArtKey].orEmpty().lineSequence()
                .mapNotNull { line ->
                    line.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1].split(' ').filter(String::isNotBlank) }
                }
                .toMap()
        return art to savedAt
    }

    /** Keeps [art], dated [savedAt] so that adding a missing tile does not put off the weekly refresh. */
    suspend fun save(context: Context, art: Map<String, List<String>>, savedAt: Long) {
        context.safeDataStoreEdit { prefs ->
            prefs[BrowseArtKey] = art.entries.joinToString("\n") { "${it.key}\t${it.value.joinToString(" ")}" }
            prefs[BrowseArtSavedAtKey] = savedAt
            // The single covers kept before tiles fanned several.
            prefs.remove(stringPreferencesKey("browseArt"))
            prefs.remove(longPreferencesKey("browseArtSavedAt"))
        }
    }
}
