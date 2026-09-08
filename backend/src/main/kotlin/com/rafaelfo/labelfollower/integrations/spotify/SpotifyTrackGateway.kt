package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyTrack
import com.rafaelfo.labelfollower.integrations.spotify.responses.SearchResponse
import org.springframework.stereotype.Component

@Component
class SpotifyTrackGateway(
    private val spotifyAuth: SpotifyAuth,
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) {

    fun findTrackBy(isrc: String): SpotifyTrack {
        val response = rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/search",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf(
                "q" to "isrc:$isrc",
                "type" to "track",
            ),
        ).parsedBody<SearchResponse>()

        return response.tracks.items.first()
    }

    fun findTracksById(trackIds: Set<String>): Set<SpotifyTrack> {
        return trackIds.chunked(MAX_IDS_PER_REQUEST)
            .flatMap { fetchTracksById(it.toSet()) }
            .toSet()
    }

    private fun fetchTracksById(albumIds: Set<String>): Set<SpotifyTrack> {
        return rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/tracks",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf("ids" to albumIds.joinToString(separator = ","))
        ).parsedBody<TracksResponse>().tracks.toSet()
    }

    companion object {
        private const val MAX_IDS_PER_REQUEST = 20
    }
}

private data class TracksResponse(
    val tracks: List<SpotifyTrack>,
)
