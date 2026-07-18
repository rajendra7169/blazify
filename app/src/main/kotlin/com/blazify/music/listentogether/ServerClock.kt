package com.blazify.music.listentogether

import kotlin.math.max
import kotlin.math.min

internal class ServerClock(
    private val elapsedRealtime: () -> Long,
) {
    private var serverOffsetMs: Double? = null
    private var bestRoundTripMs = Long.MAX_VALUE

    @Synchronized
    fun reset() {
        serverOffsetMs = null
        bestRoundTripMs = Long.MAX_VALUE
    }

    @Synchronized
    fun recordPong(
        clientTime: Long,
        serverReceiveTime: Long,
        serverSendTime: Long,
    ): Boolean {
        val receivedAt = elapsedRealtime()
        if (clientTime <= 0L || clientTime > receivedAt || receivedAt - clientTime > MAX_SAMPLE_AGE_MS) return false
        if (serverReceiveTime <= 0L || serverSendTime < serverReceiveTime) return false

        val roundTrip = receivedAt - clientTime
        val serverProcessing = serverSendTime - serverReceiveTime
        val networkRoundTrip = max(0L, roundTrip - serverProcessing)
        val sampleOffset = serverSendTime + networkRoundTrip / 2.0 - receivedAt
        val previousOffset = serverOffsetMs

        if (networkRoundTrip < bestRoundTripMs) bestRoundTripMs = networkRoundTrip
        val weight = if (networkRoundTrip <= bestRoundTripMs + GOOD_SAMPLE_MARGIN_MS) 0.25 else 0.05
        serverOffsetMs = previousOffset?.let { it + weight * (sampleOffset - it) } ?: sampleOffset
        return previousOffset == null
    }

    @Synchronized
    fun now(): Long? = serverOffsetMs?.let { (elapsedRealtime() + it).toLong() }

    /**
     * Our best estimate of one-way latency to the server (half the best observed
     * network round-trip), capped. Used to approximate the host→server travel leg
     * that the server's receive timestamp doesn't include. 0 until a sample exists.
     */
    @Synchronized
    fun oneWayLatencyMs(): Long =
        if (bestRoundTripMs == Long.MAX_VALUE) 0L
        else min(bestRoundTripMs / 2, MAX_HOST_LEG_COMPENSATION_MS)

    fun positionAt(
        position: Long,
        effectiveAtServerTime: Long?,
        isPlaying: Boolean,
    ): Long {
        if (!isPlaying || effectiveAtServerTime == null || effectiveAtServerTime <= 0L) return position
        val serverNow = now() ?: return position
        // effectiveAtServerTime is when the SERVER received the host's update, so the
        // host→server travel leg is missing from it — that's why guests trail the host.
        // Approximate that leg with our own one-way latency (host and server are
        // usually comparably close), capped so an asymmetric link can't overshoot.
        val hostLegCompensation = oneWayLatencyMs()
        return position + max(0L, serverNow - effectiveAtServerTime) + hostLegCompensation
    }

    private companion object {
        const val MAX_SAMPLE_AGE_MS = 60_000L
        const val GOOD_SAMPLE_MARGIN_MS = 50L
        const val MAX_HOST_LEG_COMPENSATION_MS = 500L
    }
}
