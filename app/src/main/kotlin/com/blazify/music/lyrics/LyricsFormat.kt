/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.lyrics

private val LRC_TIMESTAMP_HINT = Regex("""\[\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?\]""")

/**
 * Whether raw lyrics text is time-synced (LRC-style): it carries at least one `[mm:ss.xx]`
 * tag, wherever that first tag sits — after a BOM, blank lines or `[ar:]` / `[ti:]` tags.
 *
 * Starting with `[` is not enough. Plain lyrics often open with a section header such as
 * `[Verse 1]`, and treating those as synced left every line without a time, so nothing ever
 * lit up and the scroll for plain lyrics never ran either.
 */
fun lyricsTextLooksSynced(lyrics: String?): Boolean {
    if (lyrics.isNullOrBlank()) return false
    return LRC_TIMESTAMP_HINT.containsMatchIn(lyrics.take(4096))
}
