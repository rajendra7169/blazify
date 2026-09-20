/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.blazify.innertube.YouTube
import com.blazify.music.constants.AudioQuality
import com.blazify.music.constants.AudioQualityKey
import com.blazify.music.db.MusicDatabase
import com.blazify.music.db.entities.FormatEntity
import com.blazify.music.db.entities.LyricsEntity
import com.blazify.music.db.entities.SongEntity
import com.blazify.music.di.DownloadCache
import com.blazify.music.di.PlayerCache
import com.blazify.music.lyrics.LyricsHelper
import com.blazify.music.models.toMediaMetadata
import com.blazify.music.utils.OfflineCovers
import com.blazify.music.utils.YTPlayerUtils
import com.blazify.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import com.blazify.music.utils.dataStore
import com.blazify.music.constants.DownloadOnWifiOnlyKey
import androidx.media3.exoplayer.scheduler.Requirements
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: Cache,
    @PlayerCache val playerCache: Cache,
    private val lyricsHelper: LyricsHelper,
) {
    private val TAG = "DownloadUtil"
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .proxy(YouTube.proxy)
                            .proxyAuthenticator { _, response ->
                                YouTube.proxyAuth?.let { auth ->
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", auth)
                                        .build()
                                } ?: response.request
                            }
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            // Only a song already whole in the cache can be copied without an address. One played
            // part of the way through is cached only that far, and the rest still has to come from
            // YouTube — checking just its first byte sent those downloads out with no address for
            // the rest, and they failed. That is most of a playlist someone has been listening to.
            val knownLength = runBlocking(Dispatchers.IO) { database.format(mediaId).first()?.contentLength }
            if (knownLength != null &&
                knownLength > dataSpec.position &&
                playerCache.isCached(mediaId, dataSpec.position, knownLength - dataSpec.position)
            ) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    // A download is kept, so Auto means full quality even on mobile data.
                    audioQuality = if (audioQuality == AudioQuality.AUTO) AudioQuality.HIGH else audioQuality,
                    connectivityManager = connectivityManager,
                )
            }.getOrElse { error ->
                // Only a song YouTube will not play at all is given up on. Anything else — a busy
                // moment with a whole playlist asked for at once, a check that clears, a dropped
                // connection — goes back as a network error, which the download waits out and
                // tries again instead of failing for good at the first refusal.
                if (YTPlayerUtils.isSongUnavailable(error)) throw error
                throw IOException("Could not look up $mediaId", error)
            }
            val format = playbackData.format

            // Some songs, uploads above all, arrive without a length. Asking the file itself is
            // worth a try, but not knowing is no reason to give up on the download.
            val actualContentLength = format.contentLength ?: runCatching {
                val client = OkHttpClient.Builder()
                    .proxy(YouTube.proxy)
                    .proxyAuthenticator { _, response ->
                        YouTube.proxyAuth?.let { auth ->
                            response.request.newBuilder()
                                .header("Proxy-Authorization", auth)
                                .build()
                        } ?: response.request
                    }
                    .build()
                val request = okhttp3.Request.Builder()
                    .head()
                    .url(playbackData.streamUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    response.header("Content-Length")?.toLongOrNull()
                }
            }.getOrNull()

            database.query {
                if (actualContentLength != null) {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = actualContentLength,
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                            playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        ),
                    )
                }

                // Metadata registration only — dateDownload is intentionally NOT set here.
                // It belongs solely to onDownloadChanged()'s STATE_COMPLETED branch below,
                // which only fires once the download has actually finished. Setting it here
                // (at URL-resolve time, i.e. the moment the download merely *starts*) would
                // mark the song as "cached" before a single byte is written.
                val existing = getSongByIdBlocking(mediaId)?.song
                val updatedSong = existing ?: SongEntity(
                    id = mediaId,
                    title = playbackData.videoDetails?.title ?: "Unknown",
                    duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                    thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                    dateDownload = null,
                    isDownloaded = false
                )

                upsert(updatedSong)
            }

            val streamUrl = playbackData.streamUrl.let {
                if (actualContentLength != null) "${it}&range=0-${actualContentLength}" else it
            }

            songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L)
            dataSpec.withUri(streamUrl.toUri())
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            // A whole playlist at once can meet a few refusals in a row before YouTube settles;
            // each retry waits a little longer than the one before.
            minRetryCount = 10
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    database.updateDownloadedInfo(download.request.id, true, LocalDateTime.now())
                                    // Its cover too, or it plays offline as a blank square.
                                    OfflineCovers.save(context, database.song(download.request.id).first()?.song?.thumbnailUrl)
                                    // A downloaded song should work fully offline —
                                    // fetch and store its lyrics now, while we still
                                    // have network.
                                    ensureLyricsAvailableOffline(download.request.id)
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download,
                    ) {
                        val downloadId = download.request.id

                        runCatching {
                            database.updateDownloadedInfo(downloadId, false, null)
                        }.onSuccess {
                            downloads.update { map ->
                                map.toMutableMap().apply {
                                    remove(downloadId)
                                }
                            }
                            Timber.tag(TAG).d("Successfully removed download $downloadId from in-memory map")
                        }.onFailure { error ->
                            Timber.tag(TAG).e(error, "Failed to update database for removed download $downloadId, keeping in-memory entry")
                        }
                    }
                }
            )
        }

    init {
        // With Download only on Wi-Fi on, downloads wait for an unmetered network and
        // pick up again by themselves when the phone connects to one.
        scope.launch {
            context.dataStore.data.map { it[DownloadOnWifiOnlyKey] ?: false }
                .distinctUntilChanged()
                .collect { wifiOnly ->
                    val requirements =
                        Requirements(if (wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK)
                    withContext(Dispatchers.Main) { downloadManager.requirements = requirements }
                }
        }

        // Songs downloaded before covers were kept get theirs the next time there is a connection.
        scope.launch {
            database.downloadedSongsByCreateDateAsc().first().forEach { song ->
                OfflineCovers.save(context, song.song.thumbnailUrl)
            }
        }

        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    /**
     * Fetch + cache lyrics for a song that was just downloaded, so they are
     * available offline. Skips when lyrics are already cached or the duration is
     * unknown (fuzzy providers would cache wrong lyrics permanently). NOT_FOUND
     * is never cached — a later online play can still retry.
     */
    private suspend fun ensureLyricsAvailableOffline(songId: String) {
        try {
            if (database.lyrics(songId).first() != null) return
            val song = database.song(songId).first() ?: return
            val mediaMetadata = song.toMediaMetadata()
            if (mediaMetadata.duration <= 0) return
            val fetched = lyricsHelper.getLyrics(mediaMetadata)
            if (fetched.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                database.query {
                    upsert(LyricsEntity(songId, fetched.lyrics, fetched.provider))
                }
                Timber.tag(TAG).d("Cached lyrics offline for downloaded song $songId (${fetched.provider})")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to cache offline lyrics for $songId")
        }
    }

    fun release() {
        scope.cancel()
    }
}
