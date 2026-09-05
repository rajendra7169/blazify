/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.music.db.MusicDatabase
import com.blazify.music.constants.SongSortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

/** One directory on the phone, and how much music is sitting in it. */
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
)

/**
 * The folders the music on this device actually lives in.
 *
 * Derived rather than stored: a folder is only ever "somewhere a scanned file
 * was found", so it needs nothing of its own in the database and cannot go
 * stale against it. Grouping happens here because SQLite has no notion of a
 * parent directory.
 */
@HiltViewModel
class LocalFoldersViewModel
@Inject
constructor(
    database: MusicDatabase,
) : ViewModel() {
    val folders =
        database.localSongs(SongSortType.NAME, false)
            .map { songs ->
                songs
                    .mapNotNull { it.song.localPath }
                    .mapNotNull { File(it).parent }
                    .groupingBy { it }
                    .eachCount()
                    .map { (path, count) ->
                        MusicFolder(
                            path = path,
                            name = File(path).name.ifBlank { path },
                            songCount = count,
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
