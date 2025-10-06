package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyTrack
import com.rafaelfo.labelfollower.utils.mapToSet
import org.springframework.stereotype.Component

@Component
class SpotifyPlaylistGateway(
    private val spotifyAuth: SpotifyAuth,
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) {

    fun getTracks(playlistId: String): Set<SpotifyTrack> {
        return getPlaylistTracks(0, playlistId)
    }

    private fun getPlaylistTracks(startingOffset: Int, playlistId: String): Set<SpotifyTrack> {
        if (playlistTracksCache.contains(playlistId)) return playlistTracksCache[playlistId]!!

        val response = rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/playlists/$playlistId/tracks",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf(
                "fields" to "total,items(track(id,name,external_ids,album(id,name,release_date)))",
                "limit" to LIMIT.toString(),
                "offset" to startingOffset.toString(),
            )
        ).parsedBody<PlaylistTracksResponse>()

        val tracks = response.items.mapToSet { it.track }
        if (response.total > startingOffset + LIMIT && startingOffset + LIMIT < 1000) {
            val nextOffset = startingOffset + LIMIT
            return getPlaylistTracks(nextOffset, playlistId) + tracks
        }

        return tracks
            .also { playlistTracksCache[playlistId] = it }
    }

    companion object {
        private const val LIMIT = 50
        private val playlistTracksCache: MutableMap<String, Set<SpotifyTrack>> = mutableMapOf()
    }
}

data class PlaylistTracksResponse(
    val total: Int,
    val items: Set<PlaylistTrackItem>,
)

data class PlaylistTrackItem(
    val track: SpotifyTrack,
)
