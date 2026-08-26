package com.blazify.innertube

import com.blazify.innertube.models.YouTubeClient
import com.blazify.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.Proxy

private class NewPipeDownloaderImpl(
    proxy: Proxy?,
    proxyAuth: String?,
) : Downloader() {
    private fun normalizeResponseBody(
        url: String,
        body: String?,
    ): String? {
        if (!url.contains("returnyoutubedislikeapi.com", ignoreCase = true)) {
            return body
        }

        val trimmed = body?.trimStart().orEmpty()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return body
        }

        return "{\"likes\":0,\"dislikes\":0,\"viewCount\":0}"
    }

    private val client =
        OkHttpClient
            .Builder()
            .proxy(proxy)
            .proxyAuthenticator { _, response ->
                proxyAuth?.let { auth ->
                    response.request
                        .newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }.build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder =
            okhttp3.Request
                .Builder()
                .method(httpMethod, dataToSend?.toRequestBody())
                .url(url)
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val latestUrl = response.request.url.toString()
        val responseBodyToReturn = normalizeResponseBody(latestUrl, response.body.string())
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBodyToReturn,
            responseBodyToReturn?.toByteArray(),
            latestUrl,
        )
    }

    override fun executeAsync(request: Request, callback: AsyncCallback?): CancellableCall {
        TODO("Placeholder")
    }
}

/**
 * Hand the extractor something to fetch with, once, before it is asked anything.
 *
 * It is built around a downloader the host supplies and has none of its own, so
 * every call into it before this has run dies on a null reference deep inside —
 * which surfaces as "signature timestamp unavailable" and an empty list of
 * streams, never as "nothing was ever wired up". That is exactly what happened
 * here: the wiring sat in the initialiser of an object nothing referenced, so
 * it never ran, and the deciphering fallbacks were dead for every song.
 *
 * Called from each entry point rather than from an initialiser, so it cannot go
 * quiet again the moment the object that holds it stops being used. Re-run when
 * the proxy changes, because the downloader takes its route at construction and
 * a route chosen before the change is the wrong one afterwards.
 */
private var wiredFor: Pair<java.net.Proxy?, String?>? = null

@Synchronized
internal fun ensureNewPipeReady() {
    val route = YouTube.proxy to YouTube.proxyAuth
    if (wiredFor == route) return
    NewPipe.init(NewPipeDownloaderImpl(route.first, route.second))
    wiredFor = route
}

object NewPipeUtils {
    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        ensureNewPipeReady()
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            ensureNewPipeReady()
            val url =
                format.url ?: format.signatureCipher?.let { signatureCipher ->
                    val params = parseQueryString(signatureCipher)
                    val obfuscatedSignature =
                        params["s"]
                            ?: throw ParsingException("Could not parse cipher signature")
                    val signatureParam =
                        params["sp"]
                            ?: throw ParsingException("Could not parse cipher signature parameter")
                    val url =
                        params["url"]?.let { URLBuilder(it) }
                            ?: throw ParsingException("Could not parse cipher url")
                    url.parameters[signatureParam] =
                        YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                            videoId,
                            obfuscatedSignature,
                        )
                    url.toString()
                } ?: throw ParsingException("Could not find format url")

            return@runCatching YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url,
            )
        }
}

object NewPipeExtractor {
    fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        return try {
            ensureNewPipeReady()
            val streamInfo =
                StreamInfo.getInfo(
                    NewPipe.getService(0),
                    "https://www.youtube.com/watch?v=$videoId",
                )
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull {
                (it.itagItem?.id ?: return@mapNotNull null) to it.content
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        ensureNewPipeReady()
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    /**
     * Undo the throttle on a URL that already has one.
     *
     * A stream address carries an `n` parameter the site scrambles, and the
     * content server serves a whole song to whoever unscrambles it and a 403 to
     * everyone else. The unscrambling lives in the site's own player script and
     * is rewritten every few weeks, so anything that reads that script by
     * pattern eventually meets a shape it has never seen — at which point it
     * hands the address back untouched, which looks like success and plays like
     * nothing at all.
     *
     * This is the second opinion for that moment: the same job done by a
     * library whose whole purpose is keeping up with the rewrites.
     */
    fun deobfuscateThrottling(videoId: String, url: String): String? = try {
        ensureNewPipeReady()
        YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
    } catch (e: Exception) {
        null
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): String? {
        return try {
            ensureNewPipeReady()
            val url = format.url ?: format.signatureCipher?.let { signatureCipher ->
                val params = parseQueryString(signatureCipher)
                val obfuscatedSignature = params["s"]
                    ?: throw ParsingException("Could not parse cipher signature")
                val signatureParam = params["sp"]
                    ?: throw ParsingException("Could not parse cipher signature parameter")
                val url = params["url"]?.let { URLBuilder(it) }
                    ?: throw ParsingException("Could not parse cipher url")
                url.parameters[signatureParam] =
                    YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                        videoId,
                        obfuscatedSignature
                    )
                url.toString()
            } ?: throw ParsingException("Could not find format url")

            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url,
            )
        } catch (e: Exception) {
            null
        }
    }
}
