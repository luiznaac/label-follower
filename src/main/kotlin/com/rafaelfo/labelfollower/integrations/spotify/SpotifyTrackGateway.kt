package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.models.Artist
import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.TrackGateway
import org.springframework.stereotype.Component

@Component
class SpotifyTrackGateway(
    private val spotifyAuth: SpotifyAuth,
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) : TrackGateway {

    override fun getLabel(track: Track): Label {
        TODO("Not yet implemented")
    }

    override fun findTrackBy(isrc: String): Track {
        val response = rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/search",
            headers = mapOf("Authorization" to "Bearer ${spotifyAuth.getToken()}"),
            queryParameters = mapOf(
                "q" to "isrc:${isrc}",
                "type" to "track",
            ),
        ).parsedBody<SearchResponse>()

        return response.tracks.items.first().toTrack()
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

    fun toTrack() = Track(
        name = name,
        artists = artists.map { it.toArtist() }.toSet(),
        isrc = external_ids.isrc,
    )
}

private data class ArtistResponse(
    val name: String,
) {

    fun toArtist() = Artist(
        name = name,
    )
}

private data class ExternalIds(
    val isrc: String,
)
