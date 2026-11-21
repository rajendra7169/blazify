package com.blazify.innertube.videoart

import com.blazify.innertube.videoart.VideoArtText.badWord
import com.blazify.innertube.videoart.VideoArtText.contains
import com.blazify.innertube.videoart.VideoArtText.containsWords
import com.blazify.innertube.videoart.VideoArtText.coreTitle
import com.blazify.innertube.videoart.VideoArtText.fold
import com.blazify.innertube.videoart.VideoArtText.hasGoodWord
import com.blazify.innertube.videoart.VideoArtText.parseSeconds
import com.blazify.innertube.videoart.VideoArtText.parseViews
import com.blazify.innertube.videoart.VideoArtText.splitArtists
import com.blazify.innertube.videoart.VideoArtText.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The expected values are what the reference finder (bench.py, 2026-11-22) gives for the same text. */
class VideoArtTextTest {
    @Test
    fun foldDropsAccentsAndSigns() {
        assertEquals("besame mucho ", fold("Bésame Mucho!"))
        assertEquals("gymnopedie no  1", fold("Gymnopédie No. 1"))
        assertEquals("krsna", fold("KR\$NA"))
        assertEquals("tom  and  jerry", fold("Tom & Jerry"))
        assertEquals("x2 xii fi 123", fold("x² Ⅻ ﬁ １２３"))
        assertEquals("snake_case", fold("snake_case"))
        assertEquals("naive cafe", fold("naïve café"))
        assertEquals("ελληνικα σας", fold("Ελληνικά ΣΑΣ"))
        assertEquals("محمد رمضان", fold("محمد رمضان"))
    }

    @Test
    fun foldDropsDakutenButSplitsDevanagariVowelSigns() {
        // Japanese voiced marks come off ("ド" reads as "ト"), as accents do.
        assertEquals("アイトル", fold("アイドル"))
        // Devanagari vowel signs are not word letters: they split the word; the virama goes.
        assertEquals("फ लक  आ ख म ", fold("फूलको आँखामा"))
        assertEquals(
            "आन  छ इङ ड लम ",
            fold("आनी छोइङ डोल्मा"),
        )
    }

    @Test
    fun wordsSplitOnSpaces() {
        assertEquals(listOf("tom", "and", "jerry"), words("Tom & Jerry"))
        assertEquals(listOf("gymnopedie", "no", "1"), words("Gymnopédie No. 1"))
        assertEquals(emptyList<String>(), words("  !!  "))
    }

    @Test
    fun coreTitleKeepsTheSongsName() {
        assertEquals("Kesariya", coreTitle("Kesariya (From \"Brahmastra\")"))
        assertEquals("Bhool Bhulaiyaa 3", coreTitle("Bhool Bhulaiyaa 3 - Title Track"))
        assertEquals("YKWIM", coreTitle("YKWIM (feat. KRSNA)"))
        assertEquals("Hataarindai, Bataasindai", coreTitle("Hataarindai, Bataasindai (feat. Shyam Nepali)"))
        assertEquals("Song tail", coreTitle("Song [Remix) tail"))
        // Only a dash with spaces round it cuts: "A-ha" stays whole.
        assertEquals("A-ha", coreTitle("A-ha - Take On Me"))
        // Nothing left: the whole title.
        assertEquals("(Intro)", coreTitle("(Intro)"))
    }

    @Test
    fun containsCountsWholeWords() {
        assertTrue(contains("Rolling in the Deep", "Adele - Rolling In The Deep (Official Music Video)"))
        assertEquals(0.5, containsWords("Arijit Singh", "Arijit S, Shilpa R"), 1e-9)
        assertFalse(contains("Arijit Singh", "JAWAN: Chaleya (Hindi) | Arijit S, Shilpa R"))
        // Words, not letters: "Song" is not in "Songs".
        assertFalse(contains("Song", "Songs of love"))
        assertEquals(0.0, containsWords("", "anything"), 0.0)
        assertTrue(contains("KR\$NA", "krsna"))
    }

    @Test
    fun badWordFindsWhatTheSongDoesNotHave() {
        assertEquals("lyric", badWord("Dynamite (Official Lyric Video)", "Dynamite"))
        assertNull(badWord("Live Forever (Live)", "Live Forever"))
        assertEquals("lo-fi", badWord("Chaleya Lo-Fi", "Chaleya"))
        assertEquals("#shorts", badWord("Song #shorts", "Song"))
        assertEquals("sped up", badWord("Song (Sped Up)", "Song"))
        assertEquals("@", badWord("BTS @ MMA", "Dynamite"))
        assertNull(badWord("Song (Official Video)", "Song"))
        assertNull(badWord("Songs of love", "Song"))
        assertNull(badWord("The Edit", "The Edit"))
        assertEquals("festival", badWord("BTS(방탄소년단) - Dynamite (2020 KBS Song Festival)", "Dynamite"))
    }

    @Test
    fun goodWords() {
        assertTrue(hasGoodWord("Adele - Rolling in the Deep (Official Music Video)"))
        assertTrue(hasGoodWord("Sajni (Song): Arijit Singh, Ram Sampath"))
        assertTrue(hasGoodWord("BTS (방탄소년단) 'Dynamite' Official MV"))
        assertTrue(hasGoodWord("Kill This Love M/V"))
        assertFalse(hasGoodWord("Apna Bana Le - Bhediya | Varun Dhawan, Kriti Sanon"))
    }

    @Test
    fun viewsAndLengths() {
        assertEquals(2_100_000_000L, parseViews("2.1B views"))
        assertEquals(641_356_248L, parseViews("641,356,248 views"))
        assertEquals(33_000L, parseViews("33K views"))
        assertEquals(1_500_000L, parseViews("1.5M"))
        assertEquals(0L, parseViews("No views"))
        assertEquals(0L, parseViews(null))
        assertEquals(224, parseSeconds("3:44"))
        assertEquals(3723, parseSeconds("1:02:03"))
        assertEquals(0, parseSeconds(""))
    }

    @Test
    fun creditLinesSplitIntoPeople() {
        assertEquals(listOf("Vishal", "Sheykhar", "Atif Aslam"), splitArtists(listOf("Vishal & Sheykhar", "Atif Aslam")))
        assertEquals(listOf("Arijit Singh", "Shilpa Rao"), splitArtists(listOf("Arijit Singh, Shilpa Rao")))
        assertEquals(listOf("Sachin-Jigar"), splitArtists(listOf("Sachin-Jigar", " ")))
    }
}
