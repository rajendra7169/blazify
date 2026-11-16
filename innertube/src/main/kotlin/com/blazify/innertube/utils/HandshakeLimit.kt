package com.blazify.innertube.utils

import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * A time limit on the secure handshake alone, so a connection that will never finish it gives
 * way to the next address in seconds instead of a minute.
 *
 * Some networks have IPv6 that half works: a connection to YouTube over IPv6 opens, then the
 * secure handshake on it never completes, while the same server over IPv4 answers at once. The
 * connection library tries IPv6 first, sees it open, and then waits on the handshake for as long
 * as a read may take, which for the catalogue is a minute, before it moves on to IPv4. Home sat
 * on placeholders for that minute. Each handshake read now has [MILLIS], the library's own
 * default; once the connection is up, the client's own read limit takes over again.
 *
 * The socket factory and trust manager are the platform's defaults, the same ones the library
 * builds when none is given, so nothing about which servers are trusted changes.
 */
object HandshakeLimit {
    const val MILLIS = 10_000

    private val trustManager: X509TrustManager by lazy {
        TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null as KeyStore?) }
            .trustManagers
            .first { it is X509TrustManager } as X509TrustManager
    }

    private val socketFactory: SSLSocketFactory by lazy {
        Limited(
            SSLContext
                .getInstance("TLS")
                .apply { init(null, arrayOf(trustManager), null) }
                .socketFactory,
        )
    }

    /** Applies the limit to [builder]. */
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder = builder.sslSocketFactory(socketFactory, trustManager)

    private class Limited(
        private val base: SSLSocketFactory,
    ) : SSLSocketFactory() {
        private fun Socket.limited(): Socket = apply { soTimeout = MILLIS }

        override fun getDefaultCipherSuites(): Array<String> = base.defaultCipherSuites

        override fun getSupportedCipherSuites(): Array<String> = base.supportedCipherSuites

        override fun createSocket(s: Socket, host: String?, port: Int, autoClose: Boolean): Socket =
            base.createSocket(s, host, port, autoClose).limited()

        override fun createSocket(): Socket = base.createSocket().limited()

        override fun createSocket(host: String?, port: Int): Socket = base.createSocket(host, port).limited()

        override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
            base.createSocket(host, port, localHost, localPort).limited()

        override fun createSocket(host: InetAddress?, port: Int): Socket = base.createSocket(host, port).limited()

        override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
            base.createSocket(address, port, localAddress, localPort).limited()
    }
}
