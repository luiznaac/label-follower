package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Track
import org.springframework.stereotype.Service

@Service
class LabelIntrospector(
    private val externalInfoGateway: ExternalInfoGateway,
) {

    fun introspectFrom(track: Track): Set<Track> {
        return externalInfoGateway.getTracksFrom(
            track.getLabel()
        )
    }

    private fun Track.getLabel() = externalInfoGateway.getLabel(isrc)
}
