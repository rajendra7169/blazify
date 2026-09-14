package com.blazify.music.utils.cipher

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The table is read from more than one place, and each place keeps its own cached copy. A place
 * that is behind must never take away a player another place already taught, our own copy wins
 * when two places disagree, and every cached copy comes back after a restart.
 */
class PlayerConfigStoreSourcesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun table(vararg players: Pair<String, String>) =
        players.joinToString(",", """{"schemaVersion":1,"players":{""", "}}") { (hash, sig) ->
            """"$hash":{"sig":"$sig","nClass":"Yx","sts":20613}"""
        }

    private fun apply(json: String, source: PlayerConfigStore.Source) {
        val configs = (PlayerConfigParser.parse(json) as PlayerConfigParser.ParseResult.Success).configs
        PlayerConfigStore.applyRemote(configs, json, "", source)
    }

    @Before
    fun setUp() {
        PlayerConfigStore.cacheDirForTest = tmp.newFolder("cipher_cache")
        PlayerConfigStore.setTableForTest(emptyMap())
    }

    @After
    fun tearDown() {
        PlayerConfigStore.cacheDirForTest = null
        PlayerConfigStore.setTableForTest(emptyMap())
    }

    @Test
    fun `a source that is behind keeps the players another source taught`() {
        apply(table("aaaa1111" to "mP(4,155,INPUT)"), PlayerConfigStore.Source.OWN)
        apply(table("aaaa1111" to "mP(4,155,INPUT)", "bbbb2222" to "Tl(48,5831,INPUT)"), PlayerConfigStore.Source.UPSTREAM)
        apply(table("aaaa1111" to "mP(4,155,INPUT)"), PlayerConfigStore.Source.OWN)

        assertTrue("bbbb2222" in PlayerConfigStore.knownHashes())
    }

    @Test
    fun `our own copy wins when two sources disagree`() {
        apply(table("aaaa1111" to "Tl(48,5831,INPUT)"), PlayerConfigStore.Source.OWN)
        apply(table("aaaa1111" to "mP(4,155,INPUT)"), PlayerConfigStore.Source.UPSTREAM)

        assertEquals("Tl(48,5831,INPUT)", PlayerConfigStore.get("aaaa1111")?.sigJsExpression)
    }

    @Test
    fun `every cached source comes back after a restart`() {
        apply(table("aaaa1111" to "mP(4,155,INPUT)"), PlayerConfigStore.Source.OWN)
        apply(table("bbbb2222" to "Tl(48,5831,INPUT)"), PlayerConfigStore.Source.UPSTREAM)

        PlayerConfigStore.setTableForTest(emptyMap())
        PlayerConfigStore.applyCachedOverlay()

        assertEquals(setOf("aaaa1111", "bbbb2222"), PlayerConfigStore.knownHashes())
    }
}
