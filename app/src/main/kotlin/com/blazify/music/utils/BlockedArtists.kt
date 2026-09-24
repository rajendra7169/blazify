/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import com.blazify.music.models.MediaMetadata

/**
 * Artists blocked with "Block this artist": their songs stay out of picks, radio, autoplay,
 * Home and search until the artist is let back in.
 *
 * An artist is kept as its YouTube id and its name together, because the same artist comes
 * with an id in one place and as plain text in another; either one matching is enough. A
 * blocked artist with no id is matched by name alone.
 */
object BlockedArtists {
    /** How an artist is kept: its id (empty when unknown), a bar, then its name. */
    fun entry(id: String?, name: String) = "${id.orEmpty()}|$name"

    fun idOf(entry: String) = entry.substringBefore('|').takeIf { it.isNotEmpty() }

    fun nameOf(entry: String) = entry.substringAfter('|')

    fun isBlocked(blocked: Set<String>, id: String?, name: String): Boolean {
        if (blocked.isEmpty()) return false
        return blocked.any { entry ->
            val blockedId = idOf(entry)
            (blockedId != null && blockedId == id) || nameOf(entry).equals(name, ignoreCase = true)
        }
    }

    fun blocksAnyOf(blocked: Set<String>, artists: List<MediaMetadata.Artist>): Boolean =
        blocked.isNotEmpty() && artists.any { isBlocked(blocked, it.id, it.name) }
}
