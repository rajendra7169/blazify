package com.blazify.music.utils.cipher

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Asks the real refresh path, over real HTTP, what happens when sources go missing. Each source
 * is a path on a local server that serves a table, answers 404, or isn't served at all.
 */
class PlayerConfigStoreFallbackTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: HttpServer
    private val bodies = ConcurrentHashMap<String, String>()
    private val hits = ConcurrentHashMap<String, Int>()

    private fun table(vararg hashes: String) =
        hashes.joinToString(",", """{"schemaVersion":1,"players":{""", "}}") { hash ->
            """"$hash":{"sig":"mP(4,155,INPUT)","nClass":"Yx","sts":20613}"""
        }

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                val name = exchange.requestURI.path.trim('/')
                hits.merge(name, 1, Int::plus)
                val body = bodies[name]?.toByteArray()
                if (body == null) {
                    exchange.sendResponseHeaders(404, -1)
                } else {
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                exchange.close()
            }
            start()
        }
        val base = "http://127.0.0.1:${server.address.port}"
        PlayerConfigStore.sourceUrlsForTest = PlayerConfigStore.Source.entries.associateWith { "$base/${it.name}" }
        PlayerConfigStore.cacheDirForTest = tmp.newFolder("cipher_cache")
        PlayerConfigStore.setTableForTest(emptyMap())
        PlayerConfigStore.armForcedCooldownForTest(0L)
    }

    @After
    fun tearDown() {
        server.stop(0)
        PlayerConfigStore.sourceUrlsForTest = null
        PlayerConfigStore.cacheDirForTest = null
        PlayerConfigStore.setTableForTest(emptyMap())
        PlayerConfigStore.armForcedCooldownForTest(0L)
    }

    @Test
    fun `when our copy and its mirror are gone the registry still teaches the player`() = runBlocking {
        bodies["UPSTREAM"] = table("8c3fda2d")

        assertTrue(PlayerConfigStore.forceRefresh("8c3fda2d"))
        assertEquals(1, hits["OWN"])
        assertEquals(1, hits["MIRROR"])
    }

    @Test
    fun `when our copy is behind the registry is asked and the mirror is not`() = runBlocking {
        bodies["OWN"] = table("aaaa1111")
        bodies["MIRROR"] = table("aaaa1111")
        bodies["UPSTREAM"] = table("aaaa1111", "8c3fda2d")

        assertTrue(PlayerConfigStore.forceRefresh("8c3fda2d"))
        assertEquals(null, hits["MIRROR"])
        assertTrue("aaaa1111" in PlayerConfigStore.knownHashes())
    }

    @Test
    fun `when our copy knows the player nobody else is asked`() = runBlocking {
        bodies["OWN"] = table("8c3fda2d")
        bodies["UPSTREAM"] = table("8c3fda2d")

        assertTrue(PlayerConfigStore.forceRefresh("8c3fda2d"))
        assertEquals(null, hits["MIRROR"])
        assertEquals(null, hits["UPSTREAM"])
    }

    @Test
    fun `a broken download keeps the table and every source being unreachable allows a quick retry`() = runBlocking {
        bodies["OWN"] = table("aaaa1111")
        assertFalse(PlayerConfigStore.forceRefresh("8c3fda2d"))
        assertTrue("aaaa1111" in PlayerConfigStore.knownHashes())

        PlayerConfigStore.armForcedCooldownForTest(0L)
        bodies["OWN"] = "{ not json"
        assertFalse(PlayerConfigStore.forceRefresh("8c3fda2d"))
        assertTrue("a broken download changes nothing", "aaaa1111" in PlayerConfigStore.knownHashes())

        PlayerConfigStore.armForcedCooldownForTest(0L)
        server.stop(0)

        assertFalse(PlayerConfigStore.forceRefresh("8c3fda2d"))
        assertTrue("the cached table survives", "aaaa1111" in PlayerConfigStore.knownHashes())
        assertFalse(
            "no source was reached, so the next song may try again straight away",
            PlayerConfigStore.forcedCooldownActive(System.currentTimeMillis()),
        )
    }
}
