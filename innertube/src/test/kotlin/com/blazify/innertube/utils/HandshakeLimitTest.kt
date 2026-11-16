package com.blazify.innertube.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class HandshakeLimitTest {
    /** A server that takes the connection and then says nothing, as half-working IPv6 does. */
    private fun silentServer(): ServerSocket =
        ServerSocket(0).also { server ->
            thread(isDaemon = true) {
                val held = mutableListOf<java.net.Socket>()
                runCatching { while (true) held += server.accept() }
            }
        }

    @Test
    fun aHandshakeThatNeverFinishesGivesUpInSecondsNotTheReadLimit() {
        silentServer().use { server ->
            val client =
                HandshakeLimit
                    .apply(OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).retryOnConnectionFailure(false))
                    .build()
            val started = System.nanoTime()
            try {
                client.newCall(Request.Builder().url("https://127.0.0.1:${server.localPort}/").build()).execute().close()
                fail("the silent server should never complete a handshake")
            } catch (e: SocketTimeoutException) {
                // expected
            }
            val seconds = (System.nanoTime() - started) / 1e9
            assertTrue("gave up after $seconds s", seconds in 8.0..20.0)
        }
    }
}
