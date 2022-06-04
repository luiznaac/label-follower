package com.rafaelfo.labelfollower.gateways

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.profiles.Development
import com.rafaelfo.labelfollower.usecases.TrackGateway
import org.springframework.stereotype.Component

@Component
@Development
class DevelopmentTrackGateway(
    private var tracks: Set<Track>,
) : TrackGateway {

    init {
        tracks = setOf(
            Track(name = "Track 1", artists = emptySet(), isrc = "123AAA"),
            Track(name = "Track 2", artists = emptySet(), isrc = "987CCC"),
            Track(name = "Track 3", artists = emptySet(), isrc = "456BBB"),
        )
    }

    override fun getLabel(track: Track): Label {
        TODO("Not yet implemented")
    }

    override fun getTracksFrom(label: Label): Set<Track> {
        TODO("Not yet implemented")
    }

    override fun findTrackBy(isrc: String): Track {
        return tracks.find { it.isrc == isrc }!!
    }
}
