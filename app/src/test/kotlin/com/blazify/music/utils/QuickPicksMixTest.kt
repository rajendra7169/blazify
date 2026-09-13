package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Quick picks were the whole list shuffled: the song most connected to what someone
 * plays was as likely to land last as first, and YouTube's suggestions for new music
 * could be crowded out entirely. These pin the replacement: the strongest stay on top,
 * every source gets a place, and a refresh reorders without changing what is offered.
 */
class QuickPicksMixTest {

    private val related = (1..100).map { "related$it" }
    private val similar = (1..10).map { "similar$it" }
    private val forgotten = (1..10).map { "forgotten$it" }

    private fun mix(
        related: List<String> = this.related,
        similar: List<String> = this.similar,
        forgotten: List<String> = this.forgotten,
        seed: Int = 7,
    ) = mixQuickPicks(related, similar, forgotten, id = { it }, random = Random(seed))

    @Test
    fun `shuffling within bands keeps every item inside its own band`() {
        val ranked = (0 until 23).toList()
        val shuffled = ranked.shuffledWithinBands(5, Random(3))

        assertEquals(ranked.sorted(), shuffled.sorted())
        shuffled.forEachIndexed { position, item ->
            assertEquals("item $item left its band", position / 5, item / 5)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a band size of zero is refused`() {
        listOf(1, 2, 3).shuffledWithinBands(0)
    }

    @Test
    fun `the strongest related songs lead the list`() {
        assertEquals(related.take(5).toSet(), mix().take(5).toSet())
    }

    @Test
    fun `new suggestions reach the list even when related songs are plentiful`() {
        assertEquals(8, mix().count { it.startsWith("similar") })
    }

    @Test
    fun `old favourites keep a few places`() {
        assertEquals(4, mix().count { it.startsWith("forgotten") })
    }

    @Test
    fun `the list is twenty songs at most with no repeats`() {
        val overlapping = related.take(5) + similar
        val result = mix(similar = overlapping)

        assertTrue(result.size <= 20)
        assertEquals(result.size, result.toSet().size)
    }

    @Test
    fun `someone with no history still gets YouTube's suggestions`() {
        assertEquals(similar.toSet(), mix(related = emptyList(), forgotten = emptyList()).toSet())
    }

    @Test
    fun `a refresh reorders the shelf but offers the same songs`() {
        assertEquals(mix(seed = 1).toSet(), mix(seed = 2).toSet())
    }
}
