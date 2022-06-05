package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Track

class LabelIntrospector(
    private val trackGateway: TrackGateway,
    private val labelGateway: LabelGateway,
) {

    fun introspectFrom(track: Track): Set<Track> {
        return labelGateway.getTracksFrom(
            track.getLabel()
        )
    }

    private fun Track.getLabel() = trackGateway.getLabel(isrc)
}
