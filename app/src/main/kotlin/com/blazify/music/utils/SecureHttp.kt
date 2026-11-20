package com.blazify.music.utils

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * A client for the few plain requests that are not made through Ktor.
 *
 * It exists instead of HttpURLConnection on purpose: the extractor library
 * (BlazifyExtractor, inherited from PipePipe) replaces the process-wide default
 * TLS settings of HttpsURLConnection with ones that accept any certificate
 * every time it is initialised, which happens on the first song played. A
 * request made that way afterwards, such as the one carrying the Discord token,
 * could be read or altered by anyone on the same network. OkHttp keeps its own
 * TLS settings and is not touched by that.
 */
object SecureHttp {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
