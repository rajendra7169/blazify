package com.blazify.innertube.videoart

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * How Video Art compares a song with the titles of videos: plain words, accents and punctuation
 * left out, so "Bésame Mucho" and "BESAME MUCHO!" read the same.
 */
object VideoArtText {
    /**
     * Words that make a video something other than the song's own video — a lyric video, a live
     * show, a cover, a slowed edit. Only counted when the song's own title does not have them too.
     */
    val BAD_WORDS =
        listOf(
            "lyric", "lyrics", "lyrical", "slowed", "reverb", "sped up", "speed up", "8d", "bass boosted", "bass boost",
            "karaoke", "cover", "reaction", "status", "teaser", "trailer", "#shorts", "shorts", "jukebox", "full album",
            "audio", "visualizer", "visualiser", "remix", "live", "instrumental", "ringtone", "whatsapp", "mashup",
            "nightcore", "lofi", "lo-fi", "1 hour", "10 hours", "extended", "reply version", "female version", "male version",
            "making of", "behind the scenes", "dance cover", "tutorial", "guitar", "piano", "unplugged", "acoustic",
            "reprise", "version", "edit", "loop", "fanmade", "fan made", "amv", "tiktok", "trend", "challenge",
            "@", "festival", "performance", "concert", "choreography", "dance break", "dance practice", "fancam",
            "awards", "show", "stage", "clip", "8k", "4k",
        )

    /** Words a song's own video tends to carry in its title. */
    val GOOD_WORDS =
        listOf(
            "official video", "official music video", "music video", "full video", "video song", "(song)",
            "official mv", " mv", "m/v",
        )

    /** The artist's own lyric video: kept only for when there is nothing better anywhere. */
    val LYRIC_WORDS = setOf("lyric", "lyrics", "lyrical")

    private val foldedBad = BAD_WORDS.filter { it != "@" }.map { it to " " + fold(it).trim() + " " }
    private val foldedGood = GOOD_WORDS.map { fold(it) }

    /**
     * Lower case, accents taken off, `$` read as "s" and `&` as "and", and every other sign that
     * is not a letter, a digit or a space turned into a space.
     */
    fun fold(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFKD)
        val out = StringBuilder(decomposed.length + 8)
        var i = 0
        while (i < decomposed.length) {
            val cp = decomposed.codePointAt(i)
            i += Character.charCount(cp)
            if (isCombining(cp)) continue
            when {
                cp == '$'.code -> out.append('s')
                cp == '&'.code -> out.append(" and ")
                isWordChar(cp) || isSpace(cp) -> out.appendCodePoint(cp)
                else -> out.append(' ')
            }
        }
        return out.toString()
    }

    /** The folded words of [text], in order. */
    fun words(text: String): List<String> {
        val folded = fold(text)
        val out = ArrayList<String>()
        var start = -1
        var i = 0
        while (i < folded.length) {
            val cp = folded.codePointAt(i)
            if (isSpace(cp)) {
                if (start >= 0) out += folded.substring(start, i)
                start = -1
            } else if (start < 0) {
                start = i
            }
            i += Character.charCount(cp)
        }
        if (start >= 0) out += folded.substring(start)
        return out
    }

    /** The song's name without (feat. …), [From "…"], - Remastered and the like. */
    fun coreTitle(title: String): String {
        val noBrackets = BRACKETS.replace(title, "")
        val beforeDash = noBrackets.split(SPACED_DASH)[0]
        return beforeDash.trim().ifEmpty { title }
    }

    /** The share of [needle]'s words that are also words of [hay], 0 to 1. */
    fun containsWords(needle: String, hay: String): Double {
        val wanted = words(needle)
        if (wanted.isEmpty()) return 0.0
        val have = words(hay).toHashSet()
        return wanted.count { it in have }.toDouble() / wanted.size
    }

    /** Every word of [needle] is in [hay]. */
    fun contains(needle: String, hay: String): Boolean = containsWords(needle, hay) >= 0.99

    /** The first bad word in [videoTitle] that [songTitle] does not have itself, or null. */
    fun badWord(videoTitle: String, songTitle: String): String? {
        if ('@' in videoTitle && '@' !in songTitle) return "@"
        val video = " " + fold(videoTitle) + " "
        val song = " " + fold(songTitle) + " "
        for ((word, folded) in foldedBad) {
            if (folded in video && folded !in song) return word
        }
        return null
    }

    /** Whether the title says it is the song's video: "Official Video", "(Song)", "M/V"… */
    fun hasGoodWord(videoTitle: String): Boolean {
        val video = " " + fold(videoTitle) + " "
        return foldedGood.any { it in video }
    }

    /** The views in "2.1B views" or "641,356,248 views"; 0 when there is no number. */
    fun parseViews(text: String?): Long {
        val match = VIEWS.find(text.orEmpty().replace(",", "")) ?: return 0
        val number = match.groupValues[1].toDoubleOrNull() ?: return 0
        val scale =
            when (match.groupValues[2]) {
                "K" -> 1e3
                "M" -> 1e6
                "B" -> 1e9
                else -> 1.0
            }
        return (number * scale).toLong()
    }

    /** "3:44" or "1:02:03" in seconds; every number in the text counts, as minutes and seconds. */
    fun parseSeconds(text: String?): Int {
        var total = 0L
        for (part in DIGITS.findAll(text.orEmpty())) {
            val value = part.value.toLongOrNull() ?: return 0
            total = (total * 60 + value).coerceAtMost(Int.MAX_VALUE.toLong())
        }
        return total.toInt()
    }

    /**
     * A credit line read as separate names: "Vishal & Sheykhar" and "Arijit Singh, Shilpa Rao"
     * are two people each.
     */
    fun splitArtists(names: List<String>): List<String> =
        names.flatMap { name -> ARTIST_SEPARATORS.split(name).map { it.trim() } }.filter { it.isNotEmpty() }

    // Spaces as Unicode counts them, not only the ASCII ones: titles carry no-break spaces.
    private const val WS = "[\\s\\u0085\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\u001C-\\u001F]"
    private val BRACKETS = Regex("$WS*[(\\[].*?[)\\]]")
    private val SPACED_DASH = Regex("$WS+-$WS+")
    private val VIEWS = Regex("([0-9.,]+)$WS*([KMB]?)")
    private val DIGITS = Regex("[0-9]+")
    private val ARTIST_SEPARATORS = Regex(",|&| and ")

    private fun isWordChar(cp: Int): Boolean {
        if (cp == '_'.code || Character.isLetter(cp)) return true
        return when (Character.getType(cp).toByte()) {
            Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER -> true
            else -> false
        }
    }

    private fun isSpace(cp: Int): Boolean = Character.isWhitespace(cp) || Character.isSpaceChar(cp) || cp == 0x85

    private val combining = ConcurrentHashMap<Int, Boolean>()

    /**
     * A mark that sits on the letter before it (an accent, a dakuten), which is dropped. A mark
     * that is a sound of its own — most Devanagari vowel signs — is not, and becomes a space.
     *
     * Java has no way to ask for a character's combining class directly, so this asks Unicode's
     * own reordering: a mark with a class moves past marks of a lower class, one without does not.
     */
    private fun isCombining(cp: Int): Boolean {
        when (Character.getType(cp).toByte()) {
            Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> Unit
            else -> return false
        }
        return combining.getOrPut(cp) {
            val mark = String(Character.toChars(cp))
            // Class above 1: swaps with a following class-1 overlay (U+0334).
            val above = mark + "̴"
            // Class exactly 1: moves in front of a preceding class-230 accent (U+0301).
            val one = "́" + mark
            Normalizer.normalize(above, Normalizer.Form.NFD) != above ||
                Normalizer.normalize(one, Normalizer.Form.NFD) != one
        }
    }
}
