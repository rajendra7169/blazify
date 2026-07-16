/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.lyrics

import android.content.Context
import com.blazify.music.constants.EnableBetterLyricsKey
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.get

/**
 * Better Lyrics via the aggregator API the browser extension uses
 * (Musixmatch, Better Lyrics, BiniLyrics, Portato/Legato, LRCLib), matched by
 * the exact YouTube video id. The old lyrics-api.boidu.dev endpoint now
 * returns 401 for everyone; [BetterLyricsClient] handles the Turnstile auth
 * the new API requires.
 */
object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        val lyrics = BetterLyricsClient.getLyrics(context, id, title, artist, duration, album)
        return if (lyrics != null) {
            Result.success(lyrics)
        } else {
            Result.failure(IllegalStateException("No lyrics from Better Lyrics"))
        }
    }
}
