package com.blazify.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads a Spotify playlist or album from its link.
 *
 * Spotify's own API needs an app registration, and the token its web player
 * hands out anonymously is now refused for anything but the player itself. The
 * page behind a share link, though, carries the whole track list in a script
 * tag: what the player would draw, ready to read, with no key and no sign-in.
 *
 * That page stops at 100 tracks, which is the one thing this cannot do
 * anything about — [Playlist.mayHaveMore] says when a list was long enough to
 * be cut, so the person importing is told rather than left wondering.
 */
object SpotifyPlaylist {
    /** A track as Spotify describes it; the names are what the search will look for. */
    data class Track(
        val title: String,
        val artists: String,
        val durationSeconds: Int,
    )

    data class Playlist(
        val name: String,
        val tracks: List<Track>,
        val mayHaveMore: Boolean,
    )

    /** The most a share page will list, whatever the playlist really holds. */
    private const val PAGE_LIMIT = 100

    private val LINK =
        Regex("""(?:open\.spotify\.com/(?:intl-[a-z-]+/)?|spotify:)(playlist|album)[:/]([A-Za-z0-9]+)""")

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy { HttpClient(CIO) }

    /**
     * What the link points at, or null when it is not a Spotify playlist or
     * album — a track link, a profile, or something that is not Spotify at all.
     */
    fun parseLink(link: String): Pair<String, String>? =
        LINK.find(link.trim())?.let { it.groupValues[1] to it.groupValues[2] }

    suspend fun read(link: String): Result<Playlist> {
        val (kind, id) = parseLink(link) ?: return Result.failure(IllegalArgumentException("not a Spotify playlist link"))
        return runCatching {
            val page =
                client
                    .get("https://open.spotify.com/embed/$kind/$id") {
                        // Served to a browser; without this the page comes back
                        // without the script tag everything here depends on.
                        header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36",
                        )
                        header("Accept-Language", "en")
                    }.bodyAsText()
            parse(page).getOrThrow()
        }
    }

    /** Pulled out of [read] so it can be checked without the network. */
    fun parse(page: String): Result<Playlist> {
        val data =
            Regex("""<script id="__NEXT_DATA__" type="application/json"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                .find(page)
                ?.groupValues
                ?.get(1)
                ?: return Result.failure(IllegalStateException("no track list on the page"))

        return runCatching {
            val entity = json.decodeFromString<Page>(data).props.pageProps.state.data.entity
            val tracks =
                entity.trackList.mapNotNull { track ->
                    val title = track.title?.trim().orEmpty()
                    if (title.isEmpty()) {
                        null
                    } else {
                        Track(
                            title = title,
                            artists = track.subtitle?.trim().orEmpty(),
                            durationSeconds = ((track.duration ?: 0L) / 1000).toInt(),
                        )
                    }
                }
            if (tracks.isEmpty()) throw IllegalStateException("no tracks on the page")
            Playlist(
                name = entity.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Spotify playlist",
                tracks = tracks,
                mayHaveMore = entity.trackList.size >= PAGE_LIMIT,
            )
        }
    }

    @Serializable
    private data class Page(val props: Props)

    @Serializable
    private data class Props(val pageProps: PageProps)

    @Serializable
    private data class PageProps(val state: State)

    @Serializable
    private data class State(val data: Data)

    @Serializable
    private data class Data(val entity: Entity)

    @Serializable
    private data class Entity(
        val name: String? = null,
        val trackList: List<TrackJson> = emptyList(),
    )

    @Serializable
    private data class TrackJson(
        val title: String? = null,
        val subtitle: String? = null,
        val duration: Long? = null,
    )
}
