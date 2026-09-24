/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import android.net.Uri
import android.util.Base64

/**
 * A playlist of one's own, packed into a link to send to someone.
 *
 * The link carries the playlist's name and its songs' YouTube ids, nothing else, and carries
 * them after the "#": a browser never sends that part to a server, so sharing a playlist does
 * not tell the site what is in it. Opened on a phone with Blazify it lands in the app; opened
 * anywhere else it lands on a page that offers the app.
 *
 * Ids are eleven characters each and go one after another with nothing between them, which
 * keeps the link short enough to hold as a QR code.
 */
object PlaylistLink {
    const val ID_LENGTH = 11

    /** Songs past this are left out: the link, and the square that holds it, have their limits. */
    const val MAX_SONGS = 150

    private const val SITE = "https://rajendra7169.github.io/blazify/playlist"

    data class Shared(val name: String, val songIds: List<String>)

    fun build(name: String, songIds: List<String>): String {
        val ids = songIds.filter { it.length == ID_LENGTH }.take(MAX_SONGS)
        val packedName = Base64.encodeToString(name.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return "$SITE#1.$packedName.${ids.joinToString("")}"
    }

    /** The playlist in [uri], or null when it is not one of our playlist links. */
    fun parse(uri: Uri): Shared? {
        val isPlaylistLink =
            uri.host?.equals("playlist", ignoreCase = true) == true ||
                uri.pathSegments.any { it.equals("playlist", ignoreCase = true) }
        if (!isPlaylistLink) return null
        val parts = (uri.fragment ?: return null).split('.')
        if (parts.size != 3 || parts[0] != "1") return null
        val name =
            runCatching { String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: return null
        val ids = parts[2].chunked(ID_LENGTH).filter { it.length == ID_LENGTH }
        if (ids.isEmpty()) return null
        return Shared(name, ids)
    }
}
