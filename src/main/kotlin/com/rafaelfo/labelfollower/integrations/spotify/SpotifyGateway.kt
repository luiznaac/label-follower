package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.TrackGateway
import org.springframework.stereotype.Component

@Component
class SpotifyGateway(
    private val spotifyTrackGateway: SpotifyTrackGateway,
    private val spotifyAlbumGateway: SpotifyAlbumGateway,
) : TrackGateway {

    override fun getLabel(isrc: String): Label {
        return spotifyTrackGateway.findTrackBy(isrc).run {
            spotifyAlbumGateway.findAlbumBy(this).toLabel()
        }
    }

    override fun findTrackBy(isrc: String): Track {
        return spotifyTrackGateway.findTrackBy(isrc).toTrack()
    }
}
