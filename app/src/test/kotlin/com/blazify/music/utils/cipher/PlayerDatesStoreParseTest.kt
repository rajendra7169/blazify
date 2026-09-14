package com.blazify.music.utils.cipher

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerDatesStoreParseTest {

    @Test
    fun `reads the day each player was first seen from the registry`() {
        val registry = """
            {"schemaVersion":1,"updatedAt":"2026-09-14T08:47:25Z","players":[
              {"playerHash":"8c3fda2d","firstSeenAt":"2026-09-07T08:47:23.948Z"},
              {"playerHash":"ff8caf21"},
              "not a player"
            ]}
        """.trimIndent()

        assertEquals(mapOf("8c3fda2d" to "2026-09-07"), PlayerDatesStore.parse(registry))
    }

    @Test
    fun `still reads the old flat map a phone may have cached`() {
        assertEquals(
            mapOf("959dabb2" to "2026-06-12"),
            PlayerDatesStore.parse("""{"959dabb2":"2026-06-12","445213fb":3}"""),
        )
    }

    @Test
    fun `anything else gives no dates instead of an error`() {
        assertEquals(emptyMap<String, String>(), PlayerDatesStore.parse("not json"))
    }
}
