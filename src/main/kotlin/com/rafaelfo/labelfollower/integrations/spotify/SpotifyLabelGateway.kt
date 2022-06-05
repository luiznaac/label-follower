package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyTrack
import com.rafaelfo.labelfollower.integrations.spotify.responses.SearchResponse
import com.rafaelfo.labelfollower.models.Label
import org.springframework.stereotype.Component

@Component
class SpotifyLabelGateway(
    private val spotifyAuth: SpotifyAuth,
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) {

    fun findTracksBy(label: Label): Set<SpotifyTrack> {
        val response = rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/search",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf(
                "q" to "label:\"${label.name}\"",
                "type" to "track",
            ),
        ).parsedBody<SearchResponse>()

        return response.tracks.items.toSet()
    }
}
