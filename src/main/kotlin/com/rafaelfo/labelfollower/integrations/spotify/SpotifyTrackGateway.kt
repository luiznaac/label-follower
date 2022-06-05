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
                "q" to "isrc:${isrc}",
                "type" to "track",
            ),
        ).parsedBody<SearchResponse>()

        return response.tracks.items.first()
    }
}