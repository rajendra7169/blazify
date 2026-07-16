/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.lyrics

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "BetterLyrics" to BetterLyricsProvider,
        "Paxsenix" to PaxsenixLyricsProvider,
        "LrcLib" to LrcLibLyricsProvider,
        "KuGou" to KuGouLyricsProvider,
        "LyricsPlus" to LyricsPlusProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTube" to YouTubeLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) {
            return getDefaultProviderOrder()
        }
        return orderString.split(",").map { it.trim() }.filter { it in providerNames }
    }

    fun serializeProviderOrder(providers: List<String>): String {
        return providers.filter { it in providerNames }.joinToString(",")
    }

    // Ordered by real-world match quality on this catalog:
    // - Paxsenix (Apple Music): scores candidates by duration AND title before
    //   accepting — the most reliable synced source in practice
    // - LrcLib: strict duration match, often timed to the exact video edit
    // - BetterLyrics aggregator (Musixmatch et al., exact video id)
    // - YouTube sources: exact video, subtitles synced / lyrics plain
    // - KuGou (title-verified) and LyricsPlus last
    fun getDefaultProviderOrder(): List<String> = listOf(
        "Paxsenix",
        "LrcLib",
        "BetterLyrics",
        "YouTubeSubtitle",
        "YouTube",
        "KuGou",
        "LyricsPlus",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> {
        val order = deserializeProviderOrder(orderString)
        return order.mapNotNull { getProviderByName(it) }
    }
}
