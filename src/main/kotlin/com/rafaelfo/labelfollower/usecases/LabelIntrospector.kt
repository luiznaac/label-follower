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

        return labelTracks.filterNot { ourTracks.contains(it.isrc) }.toSet().also {
            ourInfoGateway.saveTracks(it, label)
        }
    }

    fun introspectFrom(track: Track): Set<Track> {
        return externalInfoGateway.getTracksFrom(
            track.getLabel()
        )
    }

    private fun Track.getLabel(): Label {
        val trackLabel = externalInfoGateway.getLabel(isrc)

        return ourInfoGateway.getLabelBy(trackLabel.name)?.run {
            copy(copyrights = copyrights + trackLabel.copyrights)
        } ?: trackLabel
    }
}
