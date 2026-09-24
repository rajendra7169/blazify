/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.menu

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.blazify.music.R
import com.blazify.music.constants.BlockedArtistsKey
import com.blazify.music.ui.component.Material3MenuItemData
import com.blazify.music.utils.BlockedArtists
import com.blazify.music.utils.rememberPreference

/**
 * The "Block <artist>" entry shared by the song, player and artist menus. Blocking keeps
 * that artist's songs out of picks, radio, autoplay, Home and search; the same entry lets
 * them back in, and Settings > Content > Blocked artists lists everyone blocked.
 */
@Composable
fun blockArtistMenuItem(
    artistId: String?,
    artistName: String,
    onDismiss: () -> Unit,
): Material3MenuItemData {
    val context = LocalContext.current
    val (blocked, setBlocked) = rememberPreference(BlockedArtistsKey, emptySet())
    val isBlocked = BlockedArtists.isBlocked(blocked, artistId, artistName)
    return Material3MenuItemData(
        title = {
            Text(
                text = stringResource(
                    if (isBlocked) R.string.unblock_artist else R.string.block_artist,
                    artistName,
                ),
            )
        },
        description = {
            Text(
                text = stringResource(
                    if (isBlocked) R.string.unblock_artist_hint else R.string.block_artist_hint,
                ),
            )
        },
        icon = {
            Icon(
                painter = painterResource(if (isBlocked) R.drawable.artist else R.drawable.hide_image),
                contentDescription = null,
            )
        },
        onClick = {
            if (isBlocked) {
                // By name as well as by id: the same artist is kept both ways from
                // different places, and unblocking should let all of them back in.
                setBlocked(
                    blocked.filterNot { entry ->
                        val entryId = BlockedArtists.idOf(entry)
                        (entryId != null && entryId == artistId) ||
                            BlockedArtists.nameOf(entry).equals(artistName, ignoreCase = true)
                    }.toSet(),
                )
            } else {
                setBlocked(blocked + BlockedArtists.entry(artistId, artistName))
            }
            Toast.makeText(
                context,
                context.getString(
                    if (isBlocked) R.string.artist_unblocked else R.string.artist_blocked,
                    artistName,
                ),
                Toast.LENGTH_SHORT,
            ).show()
            onDismiss()
        },
    )
}
