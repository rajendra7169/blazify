/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.AlbumEntity
import com.blazify.music.db.entities.ArtistEntity
import com.blazify.music.db.entities.SongAlbumMap
import com.blazify.music.db.entities.SongArtistMap
import com.blazify.music.db.entities.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The music already on the phone.
 *
 * Folders are remembered rather than copied: pointing Blazify at a music
 * library and then finding a second copy of it inside the app would be both
 * surprising and expensive. What is stored is a row per track saying where the
 * file is, so the library fills instantly and the disk is only re-read when
 * asked.
 *
 * A local song is an ordinary [SongEntity] whose id carries a marker and whose
 * [SongEntity.localPath] holds its content URI. Everything downstream — the
 * queue, the mini player, the equalizer, liking, play counts — treats it
 * exactly like anything streamed, because as far as those are concerned it is
 * just another song.
 */
@Singleton
class LocalMusic
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    companion object {
        const val SONG_PREFIX = "local:"
        const val ARTIST_PREFIX = "local-artist:"
        const val ALBUM_PREFIX = "local-album:"

        /** Album art has lived at this address since long before MediaStore grew a thumbnail API. */
        private val ALBUM_ART = Uri.parse("content://media/external/audio/albumart")

        fun isLocal(id: String) = id.startsWith(SONG_PREFIX)

        /** The one permission that matters, which Android 13 split out by media type. */
        val permission: String
            get() =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

        fun hasPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Re-reads the device and brings the database in line with it.
     *
     * [folders] limits the scan to files under those directories. An empty set
     * means everything MediaStore considers music, which is the sensible
     * default before anyone has expressed a preference.
     *
     * Returns how many tracks are now on file, or null if permission is missing.
     */
    suspend fun scan(folders: Set<String> = emptySet()): Int? =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) return@withContext null

            val found = read(folders)
            val seen = found.map { it.song.id }.toSet()

            // Anything that was here last time and is not here now has been
            // deleted or moved off the device, so it should not linger in the
            // library pretending to be playable.
            database.localSongIds().filterNot { it in seen }.forEach { gone ->
                runCatching { database.deleteLocalSong(gone) }
                    .onFailure { Timber.tag("LocalMusic").w(it, "could not remove $gone") }
            }

            found.forEach { track ->
                database.query {
                    insert(track.song)
                    // A rescan should pick up a retagged file, and insert alone
                    // ignores conflicts, so the row is refreshed explicitly.
                    updateLocalSong(
                        id = track.song.id,
                        title = track.song.title,
                        duration = track.song.duration,
                        thumbnailUrl = track.song.thumbnailUrl,
                        albumName = track.song.albumName,
                        localPath = track.song.localPath,
                    )
                    track.artist?.let {
                        insert(it)
                        insert(SongArtistMap(songId = track.song.id, artistId = it.id, position = 0))
                    }
                    track.album?.let {
                        insert(it)
                        insert(SongAlbumMap(songId = track.song.id, albumId = it.id, index = 0))
                    }
                }
            }

            Timber.tag("LocalMusic").i("scan found ${found.size} tracks")
            found.size
        }

    private data class Track(
        val song: SongEntity,
        val artist: ArtistEntity?,
        val album: AlbumEntity?,
    )

    private fun read(folders: Set<String>): List<Track> {
        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DATA,
            )

        val tracks = mutableListOf<Track>()

        context.contentResolver
            .query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                // Ringtones, notification sounds and voice memos are audio but
                // nobody wants them turning up next to their albums.
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(pathCol)
                    if (folders.isNotEmpty() && folders.none { path != null && path.startsWith(it) }) continue

                    val mediaId = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
                    val albumId = cursor.getLong(albumIdCol)
                    val artwork = ContentUris.withAppendedId(ALBUM_ART, albumId).toString()

                    // A file with no title tag still has a name, and a name is
                    // more use than "<unknown>".
                    val title =
                        cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                            ?: path?.let { File(it).nameWithoutExtension }
                            ?: continue

                    val artistName = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    val albumName = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }

                    tracks +=
                        Track(
                            song =
                                SongEntity(
                                    id = SONG_PREFIX + mediaId,
                                    title = title,
                                    duration = (cursor.getLong(durationCol) / 1000).toInt(),
                                    thumbnailUrl = artwork,
                                    albumId = albumName?.let { ALBUM_PREFIX + albumId },
                                    albumName = albumName,
                                    year = cursor.getInt(yearCol).takeIf { it > 0 },
                                    dateModified = cursor.getLong(modifiedCol).asDateTime(),
                                    inLibrary = LocalDateTime.now(),
                                    isLocal = true,
                                    localPath = uri.toString(),
                                ),
                            artist =
                                artistName?.let {
                                    ArtistEntity(
                                        id = ARTIST_PREFIX + cursor.getLong(artistIdCol),
                                        name = it,
                                        isLocal = true,
                                    )
                                },
                            album =
                                albumName?.let {
                                    AlbumEntity(
                                        id = ALBUM_PREFIX + albumId,
                                        title = it,
                                        year = cursor.getInt(yearCol).takeIf { y -> y > 0 },
                                        thumbnailUrl = artwork,
                                        songCount = 0,
                                        duration = 0,
                                        isLocal = true,
                                    )
                                },
                        )
                }
            }

        return tracks
    }

    /** MediaStore counts in seconds; everything in the database is a LocalDateTime. */
    private fun Long.asDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneId.systemDefault())
}
