/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A correction someone made to what a local file calls itself.
 *
 * Kept apart from the song row on purpose. A rescan rewrites that row from
 * whatever the file says, so an edit written there would survive only until
 * the next scan; held here it is reapplied afterwards instead, and deleting
 * the row is a complete undo back to the file's own tags.
 *
 * Nothing is written to the file. Other players and the computer this music
 * came from still see the original tags.
 */
@Entity(tableName = "local_tag_override")
data class LocalTagOverride(
    @PrimaryKey val songId: String,
    val title: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
)
