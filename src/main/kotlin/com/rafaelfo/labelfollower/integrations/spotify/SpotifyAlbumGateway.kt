package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyAlbum
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyTrack
import org.springframework.stereotype.Component

@Component
class SpotifyAlbumGateway(
    private val spotifyAuth: SpotifyAuth,
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) {

    fun findAlbumBy(track: SpotifyTrack): SpotifyAlbum {
        return rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/albums/${track.album!!.id}",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
        ).parsedBody()
    }

    fun findAlbumsById(albumIds: Set<String>): Set<SpotifyAlbum> {
        return albumIds.chunked(MAX_IDS_PER_REQUEST)
            .flatMap { fetchAlbumsById(it.toSet()) }
            .toSet()
    }

    private fun fetchAlbumsById(albumIds: Set<String>): Set<SpotifyAlbum> {
        return rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/albums",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf("ids" to albumIds.joinToString(separator = ","))
        ).parsedBody<AlbumsResponse>().albums.toSet()
    }

    companion object {
        private const val MAX_IDS_PER_REQUEST = 20
    }
}

private data class AlbumsResponse(
    val albums: List<SpotifyAlbum>,
)
