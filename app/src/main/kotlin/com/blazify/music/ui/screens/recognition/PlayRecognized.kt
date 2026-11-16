/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens.recognition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import com.blazify.music.playback.PlayerConnection
import androidx.navigation.NavController
import com.blazify.innertube.models.WatchEndpoint
import com.blazify.music.LocalPlayerConnection
import com.blazify.music.playback.queues.YouTubeQueue
import com.blazify.music.utils.SearchRoutes
import com.blazify.music.utils.SpotifyImport
import com.blazify.music.utils.SpotifyPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays a song that was recognised, instead of only searching for it.
 *
 * "Play on Blazify" used to open the search results for the title and artist, leaving the song
 * to be found and tapped. Now the same song is looked for on YouTube Music the way playlist
 * imports do it (the same title and one of the same artists, so not a cover or a karaoke
 * version), else the video the recognition service links to, and it starts at once, with its
 * radio after it. Only when neither is found do the search results open, as before.
 */
class RecognizedPlayer internal constructor(
    private val scope: CoroutineScope,
    private val playerConnection: PlayerConnection?,
    private val navController: NavController,
) {
    /** True while the song is being looked for; the button shows that it is working. */
    var finding by mutableStateOf(false)
        private set

    fun play(title: String, artist: String, videoId: String?) {
        if (finding) return
        finding = true
        scope.launch {
            val found =
                withContext(Dispatchers.IO) {
                    runCatching { SpotifyImport.search(SpotifyPlaylist.Track(title, artist, 0))?.id }.getOrNull()
                } ?: videoId
            finding = false
            if (found != null && playerConnection != null) {
                playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = found)))
            } else {
                navController.navigate(SearchRoutes.resultRoute("$title $artist"))
            }
        }
    }
}

@Composable
fun rememberRecognizedPlayer(navController: NavController): RecognizedPlayer {
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    return remember(playerConnection, navController) { RecognizedPlayer(scope, playerConnection, navController) }
}
