package com.blazify.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text.trim() == "•") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}

fun List<Run>.splitArtistsByConjunction(): List<Run> {
    val result = mutableListOf<Run>()
    val words = ArtistConjunctions.conjunctions
    val conjunctionPattern = Regex(
        if (words.isNotEmpty()) " (${words.joinToString("|") { Regex.escape(it) }}) | & "
        else " & ",
        RegexOption.IGNORE_CASE
    )
    forEach { run ->
        val text = run.text
        if (text.contains(conjunctionPattern)) {
            val parts = text.split(conjunctionPattern)
            parts.forEachIndexed { index, part ->
                if (part.isNotBlank()) {
                    result.add(Run(part.trim(), if (index == 0) run.navigationEndpoint else null))
                }
            }
        } else if (text.trim().equals("&", ignoreCase = true) ||
                text.trim().equals("•") ||
                words.any { text.trim().equals(it, ignoreCase = true) }
        ) {
        } else {
            result.add(run)
        }
    }
    // Names only: a ", " between names, or a line holding just a song's length ("Song • 4:41")
    // under a top search result, came through as artists called "," or "4:41".
    return result.filter { it.isArtistName() }
}

object ArtistConjunctions {
    var conjunctions: List<String> = listOf("and")
}

fun List<List<Run>>.clean(): List<List<Run>> {
    val firstGroup = getOrNull(0) ?: return this
    val hasArtistSignals = firstGroup.any { it.navigationEndpoint != null } ||
        firstGroup.any { it.text.contains(" & ") } ||
        ArtistConjunctions.conjunctions.any { conj ->
            firstGroup.any { it.text.trim().equals(conj, ignoreCase = true) }
        }
    return if (hasArtistSignals) this else drop(1)
}

/**
 * Whether [name] can be an artist's name rather than what YouTube puts between or after
 * names: a comma, an "&", an "and", a song's length ("4:41") or a year ("2026"). A name
 * has a letter in it, and it is not the word that joins the last two names.
 */
fun looksLikeArtistName(name: String): Boolean {
    val trimmed = name.trim()
    return trimmed.any { it.isLetter() } &&
        ArtistConjunctions.conjunctions.none { trimmed.equals(it, ignoreCase = true) }
}

/** An artist run: a name, or anything linking to an artist's page, as a band called "112" does. */
fun Run.isArtistName(): Boolean =
    navigationEndpoint?.browseEndpoint != null || looksLikeArtistName(text)

/**
 * The artists in a line where names and separators take turns (name, ", ", name, " & ", name).
 * Every other piece is a name, and anything that is not one is left out: a line holding only
 * a length, as a song under a top search result does ("Song • 5:00"), gives no artist at all
 * instead of an artist called "5:00".
 */
fun List<Run>.oddElements() =
    filterIndexed { index, _ ->
        index % 2 == 0
    }.filter { it.isArtistName() }
