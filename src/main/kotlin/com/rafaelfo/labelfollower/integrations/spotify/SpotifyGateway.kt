package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.TrackGateway
import org.springframework.stereotype.Component

@Component
class SpotifyGateway(
    private val spotifyTrackGateway: SpotifyTrackGateway,
) : TrackGateway {

    override fun getLabel(track: Track): Label {
        TODO("Not yet implemented")
    }

    override fun findTrackBy(isrc: String): Track {
        return spotifyTrackGateway.findTrackBy(isrc).toTrack()
    }
}
