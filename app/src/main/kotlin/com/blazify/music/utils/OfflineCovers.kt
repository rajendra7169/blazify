package com.blazify.music.utils

import android.content.Context
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import com.blazify.innertube.YouTube
import com.blazify.music.ui.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/**
 * Covers kept on the phone for downloaded songs, so a song that plays offline also looks right
 * offline.
 *
 * The image cache alone cannot promise that: it holds only what happened to be shown, at whatever
 * size it was shown, and it throws the oldest away when it fills. A download's cover is saved once,
 * at the player's size, and every screen that asks for that cover — at any size — gets it from here.
 */
object OfflineCovers {
    private const val TAG = "OfflineCovers"

    /** The size the player shows a cover at; every smaller use scales down from it. */
    private const val SAVED_SIZE = 544

    private val client by lazy { OkHttpClient.Builder().proxy(YouTube.proxy).build() }

    private fun dir(context: Context) = File(context.filesDir, "covers")

    /**
     * One name for every size of the same picture: the sizing YouTube's image addresses carry
     * after `=` (`=w544-h544…`, `=s88…`) and any query are left out.
     */
    internal fun key(url: String): String {
        val base =
            if ("googleusercontent.com/" in url || "ggpht.com/" in url) {
                url.substringBefore('=')
            } else {
                url.substringBefore('?')
            }
        val digest = MessageDigest.getInstance("SHA-1").digest(base.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun fileFor(context: Context, url: String): File? =
        File(dir(context), key(url)).takeIf { it.isFile && it.length() > 0 }

    /** Save the cover at [url], unless it already is. Quietly does nothing without a connection. */
    suspend fun save(context: Context, url: String?) {
        if (url.isNullOrBlank() || !url.startsWith("http")) return
        if (fileFor(context, url) != null) return
        withContext(Dispatchers.IO) {
            runCatching {
                val target = File(dir(context).apply { mkdirs() }, key(url))
                val part = File(target.path + ".part")
                client.newCall(Request.Builder().url(url.resize(SAVED_SIZE, SAVED_SIZE)).build()).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val body = response.body ?: error("empty answer")
                    part.outputStream().use { out -> body.byteStream().copyTo(out) }
                }
                part.renameTo(target)
            }.onFailure { Timber.tag(TAG).d("could not save cover: ${it.message}") }
        }
    }

    fun remove(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        File(dir(context), key(url)).delete()
    }

    /**
     * Serves a saved cover in place of the address asked for: straight away, since the picture
     * never changes and it spares the data, and in particular when there is no connection.
     */
    class Serve(private val context: Context) : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val url = chain.request.data as? String ?: return chain.proceed()
            if (!url.startsWith("http")) return chain.proceed()
            val saved = fileFor(context, url) ?: return chain.proceed()
            return chain.withRequest(chain.request.newBuilder().data(saved).build()).proceed()
        }
    }
}
