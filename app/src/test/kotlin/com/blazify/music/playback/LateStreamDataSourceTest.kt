package com.blazify.music.playback

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class LateStreamDataSourceTest {
    private class Recorder : DataSource {
        var opened: DataSpec? = null
        override fun open(dataSpec: DataSpec): Long { opened = dataSpec; return 10 }
        override fun read(buffer: ByteArray, offset: Int, length: Int) = -1
        override fun addTransferListener(transferListener: TransferListener) {}
        override fun getUri(): Uri? = opened?.uri
        override fun close() {}
    }

    private val address = Uri.parse("https://example.googlevideo.com/videoplayback?id=abc")

    @Test
    fun anIdOnlyReadGoesOnlineFromTheSameByte() {
        val up = Recorder()
        var asked: String? = null
        LateStreamDataSource(up) { asked = it; address }
            .open(DataSpec.Builder().setUri("5vOid-7uBdQ").setKey("5vOid-7uBdQ").setPosition(3_000_000).build())
        assertEquals("5vOid-7uBdQ", asked)
        assertEquals(address, up.opened!!.uri)
        assertEquals(3_000_000L, up.opened!!.position)
        assertEquals("5vOid-7uBdQ", up.opened!!.key)
    }

    @Test
    fun readsWithAnAddressOrAFilePathPassStraightThrough() {
        val up = Recorder()
        var asked = false
        val source = LateStreamDataSource(up) { asked = true; address }
        source.open(DataSpec.Builder().setUri("https://x.googlevideo.com/a").setKey("id1").build())
        source.open(DataSpec.Builder().setUri("/storage/emulated/0/Music/song.mp3").setKey("local:42").build())
        assertEquals(false, asked)
        assertEquals("/storage/emulated/0/Music/song.mp3", up.opened!!.uri.toString())
    }

    @Test(expected = IOException::class)
    fun noAddressFoundFailsLikeAnyNetworkError() {
        LateStreamDataSource(Recorder()) { null }.open(DataSpec.Builder().setUri("abc").setKey("abc").build())
    }

    @Test
    fun aReadWithNoKeyIsLeftAlone() {
        val up = Recorder()
        LateStreamDataSource(up) { address }.open(DataSpec.Builder().setUri("abc").build())
        assertNull(up.opened!!.key)
        assertEquals("abc", up.opened!!.uri.toString())
    }
}
