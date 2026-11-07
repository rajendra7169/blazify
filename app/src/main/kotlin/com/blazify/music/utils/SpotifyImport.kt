package com.blazify.music.utils

import com.blazify.innertube.YouTube
import com.blazify.innertube.models.SongItem
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.PlaylistEntity
import com.blazify.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Rebuilds a Spotify playlist here.
 *
 * Spotify's tracks are names, not addresses: nothing in one library points at
 * anything in the other. So each one is searched for by title and artist and
 * accepted only when the length agrees too — the wrong song under the right
 * name is worse than an honest gap, and a gap is reported rather than hidden.
 */
object SpotifyImport {
    /** How far a result's length may differ from Spotify's before it is somebody else's song. */
    private const val TOLERANCE_SECONDS = 8

    /** The same allowance when the title and an artist both agree exactly. */
    private const val SAME_NAMES_TOLERANCE_SECONDS = 30

    /** Searches in flight at once: enough to be quick, few enough to stay polite. */
    private const val AT_ONCE = 4

    data class Progress(val done: Int, val total: Int)

    data class Outcome(
        val playlistId: String,
        val name: String,
        val matched: Int,
        val total: Int,
        /** Titles nothing was found for, in the order they appear on Spotify. */
        val missing: List<String>,
        val mayHaveMore: Boolean,
    )

    suspend fun import(
        link: String,
        database: MusicDatabase,
        onProgress: (Progress) -> Unit = {},
    ): Result<Outcome> {
        val playlist = SpotifyPlaylist.read(link).getOrElse { return Result.failure(it) }
        val found = arrayOfNulls<SongItem>(playlist.tracks.size)
        var done = 0

        coroutineScope {
            val limit = Semaphore(AT_ONCE)
            playlist.tracks
                .mapIndexed { index, track ->
                    async(Dispatchers.IO) {
                        limit.withPermit {
                            found[index] = search(track)
                            onProgress(Progress(++done, playlist.tracks.size))
                        }
                    }
                }.forEach { it.await() }
        }

        val entity =
            PlaylistEntity(
                name = playlist.name,
                bookmarkedAt = LocalDateTime.now(),
                isEditable = true,
            )

        return withContext(Dispatchers.IO) {
            runCatching {
                // Written straight through rather than through database.query{}, which hands the
                // work to a background executor and returns before any of it has happened — the
                // playlist is read back on the next line.
                database.insert(entity)
                found.filterNotNull().forEach { database.insert(it.toMediaMetadata()) }
                val stored = database.playlistBlocking(entity.id) ?: throw IllegalStateException("playlist vanished")
                database.addSongToPlaylist(stored, found.filterNotNull().map { it.id })

                val missing =
                    playlist.tracks
                        .filterIndexed { index, _ -> found[index] == null }
                        .map { it.title }
                Timber
                    .tag("SpotifyImport")
                    .i("${playlist.name}: ${playlist.tracks.size - missing.size} of ${playlist.tracks.size} found")

                Outcome(
                    playlistId = entity.id,
                    name = playlist.name,
                    matched = playlist.tracks.size - missing.size,
                    total = playlist.tracks.size,
                    missing = missing,
                    mayHaveMore = playlist.mayHaveMore,
                )
            }.onFailure { Timber.tag("SpotifyImport").w(it, "import failed") }
        }
    }

    private suspend fun search(track: SpotifyPlaylist.Track): SongItem? {
        // "Live And Let Die - 2022 Remaster" or "Axel F - From "Beverly Hills Cop" Soundtrack"
        // send the search elsewhere; the plain title and the first artist find the song.
        val firstArtist = splitArtistNames(track.artists).firstOrNull().orEmpty()
        val queries =
            listOf(
                listOf(searchTitle(track.title), firstArtist),
                listOf(track.title, track.artists),
            ).map { parts -> parts.filter { it.isNotBlank() }.joinToString(" ") }
                .distinct()
        var offered = emptyList<SongItem>()
        for (query in queries) {
            val results =
                runCatching {
                    YouTube
                        .search(query, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<SongItem>()
                        .orEmpty()
                }.getOrDefault(emptyList())
            pickMatch(results, track)?.let { return it }
            if (offered.isEmpty()) offered = results
        }
        Timber.tag("SpotifyImport").d(
            "no match for '${track.title}' by '${track.artists}' (${track.durationSeconds}s); offered: " +
                offered.take(5).joinToString(" | ") { "${it.title} / ${it.artists.joinToString { a -> a.name }} / ${it.duration}s" },
        )
        return null
    }

    /** The title as people would type it: no remaster, soundtrack or featuring tail. */
    internal fun searchTitle(title: String): String {
        var t = title.replace(Regex("""\s*[(\[](feat|ft|with)\.?[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
        val dash = t.indexOf(" - ")
        if (dash > 0 && VERSION_WORDS.containsMatchIn(t.substring(dash).lowercase())) t = t.substring(0, dash)
        return t.trim()
    }

    private fun splitArtistNames(text: String): List<String> =
        text.split(Regex(""",|;|\bfeat\.?|\bft\.?""", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * The closest result by length among those that are the same song by name, or nothing.
     *
     * A result has to carry the same title (once "(feat. …)", "- Remastered" and the like are
     * set aside) and, when it lists artists, one of Spotify's: length alone let another song by
     * the same artist, or somebody's cover, win whenever it ran about as long. A track Spotify
     * has no length for (it happens) is matched on the names alone.
     */
    internal fun pickMatch(results: List<SongItem>, track: SpotifyPlaylist.Track): SongItem? {
        if (results.isEmpty()) return null
        val wantTitle = normalize(track.title)
        val wantArtists = splitArtists(track.artists)
        val sameSong =
            results.filter { item ->
                titleMatches(wantTitle, normalize(item.title)) &&
                    artistMatches(wantArtists, item.artists.map { normalize(it.name) }.filter { it.isNotEmpty() })
            }
        if (track.durationSeconds <= 0) return sameSong.firstOrNull()
        return sameSong
            .mapNotNull { item -> item.duration?.let { item to abs(it - track.durationSeconds) } }
            .filter { (item, gap) ->
                // An album cut and a single edit of the same song can differ by more than a few
                // seconds; with the title and an artist both the same, half a minute is allowed.
                gap <= TOLERANCE_SECONDS ||
                    (gap <= SAME_NAMES_TOLERANCE_SECONDS && normalize(item.title) == wantTitle && item.artists.isNotEmpty() && wantArtists.isNotEmpty())
            }.minByOrNull { (item, gap) ->
                // When Spotify names no version, a "Live", "Re-Recorded" or "Remix" result is
                // the last choice among those that fit.
                gap + if (marksVersion(item.title) && !marksVersion(track.title)) 1000 else 0
            }?.first
    }

    /** Words after a title that say which version it is. */
    private val VERSION_TAIL =
        setOf("remix", "remastered", "remaster", "version", "edit", "mix", "live", "mono", "stereo", "acoustic", "radio", "original", "theme", "soundtrack", "extended", "single", "album")

    private val VERSION_MARK =
        Regex("""\b(live|re-?recorded|remix|karaoke|instrumental|cover|sped up|slowed|acoustic|demo)\b""", RegexOption.IGNORE_CASE)

    private fun marksVersion(title: String) = VERSION_MARK.containsMatchIn(title)

    /** Words that mark a version of a song rather than its name. */
    private val VERSION_WORDS =
        Regex("""\b(remaster(ed)?|version|edit|mix|live|mono|stereo|from|feat|ft|with|acoustic|radio|bonus|deluxe|soundtrack|original|theme)\b""")

    internal fun normalize(text: String): String {
        var t = java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
        // "(feat. X)", "[Live]" and the like.
        t = t.replace(Regex("""[(\[][^)\]]*[)\]]"""), " ")
        // "Title - 2011 Remaster", "Title - Radio Edit".
        val dash = t.indexOf(" - ")
        if (dash > 0 && VERSION_WORDS.containsMatchIn(t.substring(dash))) t = t.substring(0, dash)
        t = t.replace(Regex("""\b(feat|ft)\.?\s.*$"""), " ")
        return t.replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
    }

    private fun splitArtists(text: String): List<String> =
        text.split(Regex(""",|&| and | x |;|\bfeat\.?|\bft\.?""", RegexOption.IGNORE_CASE))
            .map { normalize(it) }
            .filter { it.isNotEmpty() }

    private fun titleMatches(want: String, have: String): Boolean {
        if (want.isEmpty() || have.isEmpty()) return want == have
        if (want == have) return true
        // One title running on past the other only counts when what follows names a version
        // ("despacito remix"); "ghostbusters afterlife theme" is not "ghostbusters".
        val (short, long) = if (want.length <= have.length) want to have else have to want
        if (long.startsWith("$short ") && long.removePrefix("$short ").split(" ").all { it in VERSION_TAIL }) return true
        val a = want.split(" ").toSet()
        val b = have.split(" ").toSet()
        return a.intersect(b).size.toDouble() / a.union(b).size >= 0.6
    }

    private fun artistMatches(want: List<String>, have: List<String>): Boolean {
        // Nothing to compare on one side: the title and the length have to do.
        if (want.isEmpty() || have.isEmpty()) return true
        return want.any { w -> have.any { h -> h == w || h.contains(w) || w.contains(h) } }
    }
}
