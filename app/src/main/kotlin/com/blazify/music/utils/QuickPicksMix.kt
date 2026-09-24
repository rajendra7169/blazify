/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.utils

import kotlin.random.Random

/**
 * Keep a ranked list roughly in its order while still letting it change.
 *
 * The list is cut into bands of [bandSize] and shuffled only inside each band.
 * The strongest items stay near the top, and a refresh still shows a different
 * arrangement. Shuffling the whole list did the second and lost the first: the
 * best match for someone's taste was as likely to land twentieth as first.
 */
fun <T> List<T>.shuffledWithinBands(
    bandSize: Int,
    random: Random = Random.Default,
): List<T> {
    require(bandSize > 0) { "bandSize must be positive, was $bandSize" }
    return chunked(bandSize).flatMap { it.shuffled(random) }
}

/**
 * Put the Quick picks shelf together from its three sources.
 *
 * [related] arrives ranked, most connected to what someone plays first. The
 * strongest of those lead, then songs YouTube finds similar to what was just
 * played — the only source that can bring music they have not heard yet —
 * then a few old favourites, and whatever is left fills the rest. Each source
 * gets a place near the top instead of the largest one crowding out the others.
 */
fun <T, K> mixQuickPicks(
    related: List<T>,
    similar: List<T>,
    forgotten: List<T>,
    id: (T) -> K,
    size: Int = 20,
    bandSize: Int = 5,
    random: Random = Random.Default,
): List<T> {
    val leading = related.take(8) + similar.take(8) + forgotten.take(4)
    val rest = related.drop(8) + similar.drop(8) + forgotten.drop(4)
    return (leading + rest)
        .distinctBy(id)
        .take(size)
        .shuffledWithinBands(bandSize, random)
}
