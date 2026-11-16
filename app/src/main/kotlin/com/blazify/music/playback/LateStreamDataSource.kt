/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * Gives a song's stream its web address when a read reaches the network carrying only the song's
 * ID.
 *
 * A song found in the song cache is handed to the player by its ID alone: there is nothing to
 * fetch. But the cache has a size, and saving the next songs ahead can push out later parts of
 * the one already playing. The player then reaches a part that is gone, the cache asks the
 * network for it, and the network was given an ID, not an address: the ID was read as a file
 * name, the file did not exist, and the song stopped for two or three seconds while it was
 * looked up again from scratch.
 *
 * Here an ID-only read is turned into the stream's address first, from [resolve], and carries on
 * online from the same byte. Reads that already have an address go straight through.
 */
@UnstableApi
class LateStreamDataSource(
    private val upstream: DataSource,
    private val resolve: (mediaId: String) -> Uri?,
) : DataSource {
    class Factory(
        private val upstream: DataSource.Factory,
        private val resolve: (mediaId: String) -> Uri?,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = LateStreamDataSource(upstream.createDataSource(), resolve)
    }

    override fun open(dataSpec: DataSpec): Long {
        // Only a read whose address is exactly the song's ID. A song stored on the phone arrives
        // with its file path, which has no scheme either, and has to reach the file as it is.
        val mediaId = dataSpec.key
        if (dataSpec.uri.scheme != null || mediaId == null || dataSpec.uri.toString() != mediaId) {
            return upstream.open(dataSpec)
        }
        val address =
            resolve(mediaId)
                ?: throw IOException("No stream address for $mediaId")
        return upstream.open(dataSpec.withUri(address))
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = upstream.read(buffer, offset, length)

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() = upstream.close()
}
