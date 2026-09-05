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
import com.blazify.innertube.YouTube
import com.blazify.innertube.models.SongItem
import com.blazify.music.constants.LocalMusicArtworkOnlineKey
import com.blazify.music.ui.utils.resize
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
import kotlin.math.abs
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

        /** Loose enough for a re-encode, tight enough to reject a different song. */
        private const val DURATION_TOLERANCE_SECONDS = 5

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

            // Written straight through rather than through database.query{},
            // which hands the work to a background executor and returns before
            // any of it has happened. The artwork pass below reads these rows
            // back, so it has to be able to see them.
            found.forEach { track ->
                database.insert(track.song)
                // A rescan should pick up a retagged file, and insert alone
                // ignores conflicts, so the row is refreshed explicitly.
                database.updateLocalSong(
                    id = track.song.id,
                    title = track.song.title,
                    duration = track.song.duration,
                    thumbnailUrl = track.song.thumbnailUrl,
                    albumName = track.song.albumName,
                    localPath = track.song.localPath,
                )
                track.artist?.let {
                    database.insert(it)
                    database.insert(SongArtistMap(songId = track.song.id, artistId = it.id, position = 0))
                }
                track.album?.let {
                    database.insert(it)
                    database.insert(SongAlbumMap(songId = track.song.id, albumId = it.id, index = 0))
                }
            }

            Timber.tag("LocalMusic").i("scan found ${found.size} tracks")

            // Files that carried their own cover are already done. The rest are
            // looked up unless that has been turned off, which is the only part
            // of this that touches the network at all.
            if (context.dataStore.get(LocalMusicArtworkOnlineKey, true)) {
                runCatching { fetchMissingArtwork() }
                    .onFailure { Timber.tag("LocalMusic").w(it, "artwork lookup failed") }
            }

            found.size
        }

    /**
     * Fills in artwork the file did not carry.
     *
     * Only tracks whose picture had to fall back to MediaStore's album-art
     * table are looked up, so a file with its own cover never causes a request.
     * A result is accepted only when the duration agrees to within five
     * seconds: the titles here come from filenames, and a confident wrong
     * cover is worse than an honest blank one.
     */
    suspend fun fetchMissingArtwork(): Int =
        withContext(Dispatchers.IO) {
            var filled = 0
            // Rows still on MediaStore's album-art fallback, plus any picture
            // fetched before this asked for a full-size one. A file's own cover
            // is a file: URL and is never touched.
            val needing =
                database.localSongsBlocking().filter {
                    val art = it.song.thumbnailUrl
                    art == null ||
                        art.startsWith(ALBUM_ART.toString()) ||
                        (art.contains("googleusercontent.com") && !art.contains("=w1080"))
                }

            for (song in needing) {
                val query =
                    listOfNotNull(song.song.title, song.artists.firstOrNull()?.name)
                        .joinToString(" ")
                        .trim()
                if (query.isBlank()) continue

                val match =
                    runCatching {
                        YouTube
                            .search(query, YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()
                            ?.items
                            ?.filterIsInstance<SongItem>()
                            ?.firstOrNull { candidate ->
                                val d = candidate.duration ?: return@firstOrNull false
                                abs(d - song.song.duration) <= DURATION_TOLERANCE_SECONDS
                            }
                    }.getOrNull() ?: continue

                // Search results carry a thumbnail sized for a search result,
                // which is roughly 60px and looks it on a full player screen.
                // Everything else in the app asks for 1080 the same way.
                database.updateLocalArtwork(song.song.id, match.thumbnail.resize(1080, 1080))
                filled++
            }

            Timber.tag("LocalMusic").i("artwork filled for $filled of ${needing.size}")
            filled
        }

    /** A directory that actually contains music, and how much. */
    data class Folder(
        val path: String,
        val name: String,
        val count: Int,
    )

    /**
     * The folders music was found in, commonest first.
     *
     * Offered instead of the system folder picker on purpose. That returns a
     * document tree rather than a path, which is not what the scan filters on,
     * and it would happily let someone pick a folder with nothing in it. This
     * only ever offers places that already hold music, and can say how much.
     */
    suspend fun folders(): List<Folder> =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) return@withContext emptyList()
            val counts = linkedMapOf<String, Int>()
            context.contentResolver
                .query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media.DATA),
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                    null,
                    null,
                )?.use { cursor ->
                    val col = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    while (cursor.moveToNext()) {
                        val dir = cursor.getString(col)?.substringBeforeLast('/', "").orEmpty()
                        if (dir.isNotEmpty()) counts[dir] = (counts[dir] ?: 0) + 1
                    }
                }
            counts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { Folder(it.key, it.key.substringAfterLast('/'), it.value) }
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
                MediaStore.Audio.Media.MIME_TYPE,
            )

        val tracks = mutableListOf<Track>()
        var skipped = 0

        context.contentResolver
            .query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                // Ringtones, notification sounds and voice memos are audio but
                // nobody wants them turning up next to their albums.
                //
                // Format is checked as each row is read, below: the player has
                // a fixed set of decoders and a file it cannot open is worse
                // than a file it never listed. A library that appears in full
                // and plays nothing reads as a broken app.
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
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

                    // Skipped rather than listed and then refused at the tap of
                    // play. Windows Media, Monkey's Audio and the DSD formats
                    // are the common ones here: MediaStore calls them music
                    // because they are, and the player has no decoder for them.
                    val mime = cursor.getString(mimeCol)?.lowercase()
                    if (mime != null && !playable(mime, path)) {
                        skipped += 1
                        continue
                    }

                    val mediaId = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
                    val albumId = cursor.getLong(albumIdCol)
                    // Addresses are built with Uri.fromFile, not File.toURI.
                    // The two look alike and are not: File.toURI writes
                    // "file:/path" with one slash, which the image loader does
                    // not open, so a cover pulled out of a file's own tags was
                    // saved correctly and then never drawn — a blank square in
                    // the list and four blank squares on the library card.
                    //
                    // The file's own picture frame first, then MediaStore's
                    // album-art table, which is empty more often than not. A
                    // row is left with no picture at all rather than an address
                    // that will not open, so the app draws its placeholder
                    // instead of a black square.
                    val artwork =
                        embeddedArt(uri, SONG_PREFIX + mediaId)
                            ?: albumArtIfReadable(albumId)

                    // A file with no title tag still has a name, and a name is
                    // more use than "<unknown>".
                    val title =
                        (
                            cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                                ?: path?.let { File(it).nameWithoutExtension }
                                ?: continue
                        ).let(::tidy)

                    // A file with no artist tag usually still says who it is,
                    // in the one place people have always written it: the file
                    // name, as "Somebody - The Song". Without this the row has
                    // a title and a blank line under it, which reads as the app
                    // having failed rather than the file being untagged.
                    val taggedArtist =
                        cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    val artistName = taggedArtist ?: artistFromFileName(path)
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
                                        // A name read off the file name has no
                                        // MediaStore id behind it, so it is
                                        // keyed by the name itself — otherwise
                                        // every such song would be filed under
                                        // whatever artist id the untagged file
                                        // happened to be given, which is one
                                        // shared id for all of them.
                                        id =
                                            if (taggedArtist != null) {
                                                ARTIST_PREFIX + cursor.getLong(artistIdCol)
                                            } else {
                                                ARTIST_PREFIX + "name-" + it.lowercase().hashCode()
                                            },
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

        if (skipped > 0) {
            Timber.tag("LocalMusic").i("skipped $skipped files the player has no decoder for")
        }
        return tracks
    }

    /**
     * Files downloaded from the web arrive named like
     * `Sweety_Tera_Drama(128k).mp3`, and MediaStore takes that as the title.
     * Lyrics and artwork are both matched on the title, so the underscores
     * alone are enough to make every lookup miss.
     */
    /**
     * The artist a file name is claiming, or nothing.
     *
     * "Artist - Title.mp3" is the one convention almost every downloaded file
     * follows. Only the first dash is a separator, and only when both halves
     * look like words rather than a track number or a stray hyphen in a title.
     */
    private fun artistFromFileName(path: String?): String? {
        val name = path?.let { File(it).nameWithoutExtension } ?: return null
        val at = name.indexOf(" - ").takeIf { it > 0 } ?: return null
        val candidate = tidy(name.take(at))
        // "01 - Song" is a track number, not a person.
        if (candidate.isBlank() || candidate.all { it.isDigit() }) return null
        if (candidate.length > 60) return null
        return candidate
    }


    /**
     * Whether the player has a decoder for this file.
     *
     * Media3 ships a fixed set: MP3, AAC and the MP4 family, FLAC, Vorbis,
     * Opus, WAV, Matroska, AMR, ALAC. It has none for Windows Media, Monkey's
     * Audio, DSD or Real Audio, and no amount of trying will play them.
     *
     * MediaStore's own mime type is trusted first. Some devices leave it
     * blank or wrong, so the file name is the fallback rather than a guess in
     * either direction: unknown is allowed through, since refusing to list a
     * file that would have played is the worse mistake.
     */
    private fun playable(mime: String, path: String?): Boolean {
        val refused = listOf(
            "x-ms-wma", "wma", "monkeys-audio", "ape", "dsd", "dsf", "dff",
            "vnd.rn-realaudio", "realaudio", "x-tta", "x-musepack", "musepack",
        )
        if (refused.any { mime.contains(it) }) return false
        val extension = path?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return extension !in setOf("wma", "ape", "dsf", "dff", "dsd", "ra", "rm", "tta", "mpc")
    }

    private fun tidy(raw: String): String {
        var t = raw.replace(Regex("(\\.(mp3|m4a|aac|flac|ogg|opus|wav|wma|vm))+$", RegexOption.IGNORE_CASE), "")
        t = t.replace('_', ' ')
        t = t.replace(Regex("\\s*[(\\[][^)\\]]*(?:kbps|k|bit|hq|hd|official|audio|video|lyrics?)[^)\\]]*[)\\]]", RegexOption.IGNORE_CASE), " ")
        t = t.replace(Regex("\\s*\\b\\d{2,3}\\s?kbps\\b", RegexOption.IGNORE_CASE), " ")
        t = t.replace(Regex("\\s{2,}"), " ").trim(' ', '-', '\u2013', '\u2014', '_')
        return t.ifBlank { raw }
    }

    /**
     * MediaStore's album-art table is often empty for files that were never
     * part of a real album, but the file itself may still carry a picture
     * frame. Reading it costs nothing and needs no network.
     */
    private fun embeddedArt(uri: Uri, songId: String): String? {
        val out = File(context.filesDir, "localart/${songId.substringAfter(SONG_PREFIX)}.jpg")
        if (out.exists() && out.length() > 0) return Uri.fromFile(out).toString()
        return runCatching {
            android.media.MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                val bytes = r.embeddedPicture ?: return null
                out.parentFile?.mkdirs()
                out.writeBytes(bytes)
                Uri.fromFile(out).toString()
            }
        }.getOrNull()
    }

    /**
     * MediaStore hands out an album-art address for every album id whether or
     * not there is a picture behind it, so the only way to know is to open it.
     */
    private fun albumArtIfReadable(albumId: Long): String? {
        val uri = ContentUris.withAppendedId(ALBUM_ART, albumId)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                if (stream.read() == -1) null else uri.toString()
            }
        }.getOrNull()
    }

    /** MediaStore counts in seconds; everything in the database is a LocalDateTime. */
    private fun Long.asDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneId.systemDefault())
}
