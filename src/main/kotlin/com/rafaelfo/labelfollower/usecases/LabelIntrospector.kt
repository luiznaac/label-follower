package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Track

class LabelIntrospector(
    private val trackGateway: TrackGateway,
) {

    fun introspectFrom(track: Track): Set<Track> {
        return trackGateway.getTracksFrom(
            track.getLabel()
        )
    }

    private fun Track.getLabel() = trackGateway.getLabel(this)
}
