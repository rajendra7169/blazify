package com.blazify.innertube.videoart

import com.blazify.innertube.videoart.VideoArtSamples.Sample
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The finder on recorded answers, with the picks the reference finder (bench.py) made for them.
 */
class VideoArtFinderTest {
    private class Asked {
        val music = ArrayList<String>()
        val web = ArrayList<String>()
    }

    private fun find(sample: Sample, asked: Asked = Asked()): VideoArtResult =
        runBlocking {
            VideoArtFinder.find(
                sample.song,
                musicSearch = { asked.music += it; sample.music },
                webSearch = { asked.web += it; sample.web },
            )
        }

    @Test
    fun dynamitePrefersTheMusicVideoOverAFestivalPerformance() {
        val asked = Asked()
        val pick = find(VideoArtSamples.dynamite, asked).pick!!
        assertEquals("gdZLi9oWNZg", pick.videoId)
        assertEquals(VideoArtSource.OFFICIAL, pick.source)
        assertEquals(7.1, pick.score!!, 0.05)
        assertEquals(listOf("Dynamite BTS"), asked.music)
        // A real official video ends the search: everyday YouTube is not asked.
        assertTrue(asked.web.isEmpty())
    }

    @Test
    fun dynamiteNeverTakesTheShowsEvenWithoutTheVideo() {
        val sample = VideoArtSamples.dynamite
        val withoutVideo = sample.music.filter { it.videoId != "gdZLi9oWNZg" }
        // "@ FNS MUSIC FESTIVAL" (86M views) and the dance break practice (62M) are BTS's own OMVs, and
        // still not the song's video: the next best is a 2.9M-view tour clip with neither word.
        val next = VideoArtFinder.pickOfficial(sample.song, withoutVideo)!!
        assertEquals("kwAhKmSWdx8", next.videoId)
        val shows = withoutVideo.filter { it.videoId == "YW7JVCiDgJk" || it.videoId == "uBZXCkwCKxw" }
        assertNull(VideoArtFinder.pickOfficial(sample.song, shows))
    }

    @Test
    fun dynamiteOnEverydayYouTubeAlsoFindsTheMusicVideo() {
        val sample = VideoArtSamples.dynamite
        val result = VideoArtFinder.pickScored(sample.song, sample.web)
        assertEquals("gdZLi9oWNZg", result.pick!!.videoId)
    }

    @Test
    fun rollingInTheDeepRejectsTheFanChannelOfficialVideo() {
        val sample = VideoArtSamples.rollingInTheDeep
        val fan = sample.music.single { it.title.startsWith("MASSIVE Crowd") }
        assertEquals("MUSIC_VIDEO_TYPE_OMV", fan.musicVideoType)
        assertNull(VideoArtFinder.pickOfficial(sample.song, listOf(fan)))
        // Live on Letterman is Adele's own, but live.
        assertNull(VideoArtFinder.pickOfficial(sample.song, sample.music))

        val asked = Asked()
        val pick = find(sample, asked).pick!!
        assertEquals("rYEDA3JcQqw", pick.videoId)
        assertEquals(VideoArtSource.SCORED, pick.source)
        assertEquals("Adele", pick.channel)
        assertEquals(16.1, pick.score!!, 0.05)
        assertEquals(listOf("Rolling in the Deep Adele"), asked.web)
    }

    @Test
    fun chaleyaMatchesTheShortenedArtistName() {
        val asked = Asked()
        val pick = find(VideoArtSamples.chaleya, asked).pick!!
        // "… | Arijit S, Shilpa R | Kumaar" on T-Series: "Arijit" is enough.
        assertEquals("VAdGW7QDJiU", pick.videoId)
        assertEquals(VideoArtSource.SCORED, pick.source)
        assertEquals("T-Series", pick.channel)
        assertEquals(8.6, pick.score!!, 0.05)
    }

    @Test
    fun apnaBanaLeTakesTheLabelsMostWatchedUpload() {
        val sample = VideoArtSamples.apnaBanaLe
        val asked = Asked()
        val pick = find(sample, asked).pick!!
        assertEquals("ElZfdU54Cp8", pick.videoId)
        assertEquals("Zee Music Company", pick.channel)
        assertEquals(7.6, pick.score!!, 0.05)
        // The same title re-uploaded by an unverified channel comes first in search, and is turned down.
        assertEquals("UEvOsQBu1jY", sample.web.first().videoId)
        // Searched by the song's name without (From "Bhediya").
        assertEquals(listOf("Apna Bana Le Arijit Singh"), asked.web)
        assertEquals(listOf("Apna Bana Le (From \"Bhediya\") Arijit Singh"), asked.music)
    }

    @Test
    fun phoolkoAankhamaFallsBackToTheArtistsLyricVideo() {
        val sample = VideoArtSamples.phoolkoAankhama
        val official = VideoArtFinder.pickOfficial(sample.song, sample.music)!!
        assertEquals(VideoArtSource.LYRIC, official.source)
        assertNull(VideoArtFinder.pickScored(sample.song, sample.web).pick)

        val asked = Asked()
        val pick = find(sample, asked).pick!!
        assertEquals("bJNLrJ7MUzM", pick.videoId)
        assertEquals(VideoArtSource.LYRIC, pick.source)
        // The lyric video was only a fallback: everyday YouTube was asked first.
        assertEquals(1, asked.web.size)
    }

    @Test
    fun aRealVideoBeatsALyricVideoOfTheSameArtist() {
        val song = VideoArtSong("Song", listOf("Artist"), 200)
        val rows =
            listOf(
                MusicVideoRow("lyric000001", "Song (Official Lyric Video)", "Artist", "MUSIC_VIDEO_TYPE_OMV", 900_000_000L, 200),
                MusicVideoRow("video000001", "Song", "Artist", "MUSIC_VIDEO_TYPE_OMV", 1_000L, 200),
            )
        assertEquals("video000001", VideoArtFinder.pickOfficial(song, rows)!!.videoId)
    }

    @Test
    fun scoredVideosMustBeTrustedNamedAndNearTheSongsLength() {
        val song = VideoArtSong("Song", listOf("Artist"), 200)
        fun row(id: String, title: String, channel: String, verified: Boolean = true, length: Int = 200) =
            WebVideoRow(id, title, channel, artistBadge = false, verified = verified, views = 1_000_000_000L, length = length)

        val result =
            VideoArtFinder.pickScored(
                song,
                listOf(
                    row("unverified1", "Artist - Song (Official Video)", "Uploads", verified = false),
                    row("nobodynamed", "Song (Official Video)", "Label"),
                    row("toolong0001", "Artist - Song (Official Video)", "Label", length = 641),
                    row("tooshort001", "Artist - Song (Official Video)", "Label", length = 59),
                ),
            )
        assertNull(result.pick)
        assertEquals(4, result.notes.size)
        assertTrue(result.notes[0].endsWith("(unverified Uploads)"))
        assertTrue(result.notes[1].endsWith("(no artist)"))
        assertTrue(result.notes[2].endsWith("(length 641)"))
        assertTrue(result.notes[3].endsWith("(length 59)"))

        // A Vevo channel counts as trusted without a tick.
        val vevo = VideoArtFinder.pickScored(song, listOf(row("vevo0000001", "Artist - Song", "ArtistVEVO", verified = false)))
        assertFalse(vevo.pick == null)
    }

    @Test
    fun anotherArtistsChannelIsACoverHoweverItIsTitled() {
        // A cover singer's own channel, titled with the original singer and without the word "cover".
        val song = VideoArtSong("Apna Bana Le", listOf("Arijit Singh"), 262)
        val cover = WebVideoRow("cover000001", "Apna Bana Le - Arijit Singh | Emma Heesters", "Emma Heesters",
            artistBadge = true, verified = true, views = 20_000_000L, length = 250)
        val result = VideoArtFinder.pickScored(song, listOf(cover))
        assertNull(result.pick)
        assertTrue(result.notes.single().endsWith("(another artist Emma Heesters)"))

        // The singer's own channel with the same badge is the song's.
        val own = cover.copy(videoId = "own00000001", channel = "Arijit Singh")
        assertEquals("own00000001", VideoArtFinder.pickScored(song, listOf(own)).pick?.videoId)
    }

    @Test
    fun nothingFoundShowsTheCover() {
        val song = VideoArtSong("Weightless Part 1", listOf("Marconi Union"), 487)
        val result = runBlocking { VideoArtFinder.find(song, { emptyList() }, { emptyList() }) }
        assertNull(result.pick)
    }
}
