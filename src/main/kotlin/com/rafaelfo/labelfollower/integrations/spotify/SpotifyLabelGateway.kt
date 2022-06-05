package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyAlbum
import com.rafaelfo.labelfollower.integrations.spotify.responses.SearchResponse
import com.rafaelfo.labelfollower.models.Label
import org.springframework.stereotype.Component

@Component
class SpotifyLabelGateway(
    private val spotifyAuth: SpotifyAuth,
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) {

    fun findAlbumsBy(label: Label): Set<SpotifyAlbum> {
        return getAlbums(0, label)
    }

    private fun getAlbums(startingOffset: Int, label: Label): Set<SpotifyAlbum> {
        val response = rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/search",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf(
                "q" to "label:\"${label.name}\"",
                "type" to "album",
                "limit" to LIMIT.toString(),
                "offset" to startingOffset.toString(),
            ),
        ).parsedBody<SearchResponse>()

        if (response.albums.total > startingOffset + LIMIT && startingOffset + LIMIT < 1000) {
            val nextOffset = startingOffset + LIMIT
            return getAlbums(nextOffset, label) + response.albums.items
        }

        return response.albums.items.toSet()
    }

    companion object {
        private const val LIMIT = 50
    }
}
