package com.blazify.innertube.pages

import com.blazify.innertube.models.Album
import com.blazify.innertube.models.AlbumItem
import com.blazify.innertube.models.Artist
import com.blazify.innertube.models.ArtistItem
import com.blazify.innertube.models.MusicResponsiveListItemRenderer
import com.blazify.innertube.models.MusicTwoRowItemRenderer
import com.blazify.innertube.models.PlaylistItem
import com.blazify.innertube.models.SongItem
import com.blazify.innertube.models.YTItem
import com.blazify.innertube.models.oddElements
import com.blazify.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
