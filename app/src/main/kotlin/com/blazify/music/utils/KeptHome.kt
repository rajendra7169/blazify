/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0
 */

package com.blazify.music.utils

import android.content.Context
import com.blazify.innertube.YouTube
import java.io.File

/**
 * The last home page YouTube sent, kept so the next start can open on it.
 *
 * Marked with whose it was: a page saved for one account is never shown to another. Old ones are
 * ignored, since a home page from last week says less than a moment's placeholder.
 */
object KeptHome {
    private const val MOST_AGE_MS = 3L * 24 * 60 * 60 * 1000
    private const val RECENT_MS = 12L * 60 * 60 * 1000

    private fun file(context: Context) = File(context.filesDir, "kept_home.json")

    private fun owner(): String = (YouTube.cookie.orEmpty() + "|" + YouTube.dataSyncId.orEmpty()).hashCode().toString()

    fun read(context: Context): String? =
        runCatching {
            val file = file(context)
            if (!file.exists() || System.currentTimeMillis() - file.lastModified() > MOST_AGE_MS) return null
            val text = file.readText()
            val line = text.indexOf('\n')
            if (line < 0 || text.substring(0, line) != owner()) null else text.substring(line + 1)
        }.getOrNull()

    /** Recent enough to stay on screen, rather than give way to the fresh page when it arrives. */
    fun isRecent(context: Context): Boolean =
        System.currentTimeMillis() - file(context).lastModified() < RECENT_MS

    fun write(context: Context, answer: String) {
        runCatching {
            val file = file(context)
            val tmp = File(context.filesDir, "kept_home.json.tmp")
            tmp.writeText(owner() + "\n" + answer)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    fun forget(context: Context) {
        runCatching { file(context).delete() }
    }
}
