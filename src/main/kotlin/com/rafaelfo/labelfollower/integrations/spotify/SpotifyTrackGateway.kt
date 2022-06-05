package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyArtist
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyTrack
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

        return response.tracks.items.first().toSpotifyTrack()
    }
}

private data class SearchResponse(
    val tracks: TrackItems,
)

private data class TrackItems(
    val items: List<TrackResponse>,
)

private data class TrackResponse(
    val name: String,
    val artists: List<ArtistResponse>,
    val external_ids: ExternalIds,
) {

    fun toSpotifyTrack() = SpotifyTrack(
        name = name,
        artists = artists.map { it.toSpotifyArtist() }.toSet(),
        isrc = external_ids.isrc,
    )
}

private data class ArtistResponse(
    val name: String,
) {

    fun toSpotifyArtist() = SpotifyArtist(
        name = name,
    )
}

private data class ExternalIds(
    val isrc: String,
)
