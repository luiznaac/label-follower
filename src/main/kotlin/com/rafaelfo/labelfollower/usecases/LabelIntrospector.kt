package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import org.springframework.stereotype.Service

@Service
class LabelIntrospector(
    private val externalInfoGateway: ExternalInfoGateway,
    private val ourInfoGateway: OurInfoGateway,
) {

    fun discoverNewTracksFrom(track: Track): Set<Track> {
        return discoverNewTracksFrom(track.getLabel())
    }

    fun discoverNewTracksFrom(label: Label): Set<Track> {
        val labelTracks = externalInfoGateway.getTracksFrom(label)
        val ourTracks = ourInfoGateway.getTracksFrom(label).map { it.isrc }

        return labelTracks.filter { !ourTracks.contains(it.isrc) }.toSet().also {
            ourInfoGateway.saveTracks(it, label)
        }
    }

    fun introspectFrom(track: Track): Set<Track> {
        return externalInfoGateway.getTracksFrom(
            track.getLabel()
        )
    }

    private fun Track.getLabel() = externalInfoGateway.getLabel(isrc)
}
