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
        return getTracks(0, label)
    }

    private fun getTracks(startingOffset: Int, label: Label): Set<SpotifyTrack> {
        val response = rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/search",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf(
                "q" to "label:\"${label.name}\"",
                "type" to "track",
                "limit" to LIMIT.toString(),
                "offset" to startingOffset.toString(),
            ),
        ).parsedBody<SearchResponse>()

        if (response.tracks.total > startingOffset + LIMIT && startingOffset + LIMIT < 1000) {
            val nextOffset = startingOffset + LIMIT
            return getTracks(nextOffset, label) + response.tracks.items
        }

        return response.tracks.items.toSet()
    }

    companion object {
        private const val LIMIT = 50
    }
}
