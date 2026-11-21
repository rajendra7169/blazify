package com.blazify.innertube.videoart

import com.blazify.innertube.YouTube
import com.blazify.innertube.models.YouTubeLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * The finder against the real searches, for a handful of songs. Off unless asked for:
 *
 *     VIDEOART_LIVE=1 ./gradlew :innertube:testDebugUnitTest --tests '*VideoArtLiveCheck*'
 *
 * The table goes to innertube/build/videoart-live.txt. Requests are spaced out: a burst from one
 * home connection gets it bot-checked.
 */
class VideoArtLiveCheck {
    private val songs =
        listOf(
            VideoArtSong("Dynamite", listOf("BTS"), 200),
            VideoArtSong("Rolling in the Deep", listOf("Adele"), 229),
            VideoArtSong("Chaleya", listOf("Arijit Singh", "Shilpa Rao"), 201),
            VideoArtSong("Apna Bana Le (From \"Bhediya\")", listOf("Arijit Singh", "Sachin-Jigar"), 262),
            VideoArtSong("Phoolko Aankhama", listOf("Ani Choying Drolma"), 311),
            VideoArtSong("Blinding Lights", listOf("The Weeknd"), 202),
            VideoArtSong("Tum Hi Ho", listOf("Arijit Singh"), 262),
            VideoArtSong("Sarangi", listOf("Sushant KC"), 288),
        )

    private var last = 0L

    private suspend fun spaced() {
        val wait = last + GAP_MS - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        last = System.currentTimeMillis()
    }

    @Test
    fun live() {
        assumeTrue(System.getenv("VIDEOART_LIVE") == "1")
        YouTube.locale = YouTubeLocale(gl = "NP", hl = "en")
        val table = StringBuilder("song | source | video | title | channel | length | score\n")
        runBlocking {
            for (song in songs) {
                val result =
                    VideoArtFinder.find(
                        song,
                        musicSearch = { spaced(); YouTube.videoArtMusicRows(it).getOrThrow() },
                        webSearch = { spaced(); YouTube.videoArtWebRows(it).getOrThrow() },
                    )
                val pick = result.pick
                table.append(
                    if (pick == null) {
                        "${song.title} | none | - | ${result.notes.joinToString("; ")} | | |\n"
                    } else {
                        "${song.title} | ${pick.source.name.lowercase()} | ${pick.videoId} | ${pick.title} | " +
                            "${pick.channel.orEmpty()} | ${pick.length}s vs ${song.length}s | " +
                            "${pick.score?.let { "%.1f".format(it) } ?: "-"}\n"
                    },
                )
            }
        }
        println(table)
        File("build/videoart-live.txt").writeText(table.toString())
    }

    private companion object {
        const val GAP_MS = 2_500L
    }
}
