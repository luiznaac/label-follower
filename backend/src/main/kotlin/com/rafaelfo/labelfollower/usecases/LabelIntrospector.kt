package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import org.springframework.stereotype.Service

@Service
class LabelIntrospector(
    private val externalInfoGateway: ExternalInfoGateway,
    private val ourInfoGateway: OurInfoGateway,
) {

    // "Discover new tracks" from the Explorer screen: find them and record them straight away —
    // there is nothing downstream that could fail and lose them.
    fun discoverNewTracksFrom(track: Track): Set<Track> {
        val label = track.getLabel()
        return findNewTracksFrom(label).also { markAsKnown(it, label) }
    }

    // Which tracks of the label's recent catalogue aren't recorded yet. Records nothing, so a
    // caller that still has work to do with them (Consolidator creating a playlist) can mark them
    // as known only once that work has succeeded.
    fun findNewTracksFrom(label: Label): Set<Track> {
        val labelTracks = externalInfoGateway.getTracksFrom(label)
        val ourTracks = ourInfoGateway.getTracksFrom(label).map { it.isrc }

        return labelTracks.filterNot { ourTracks.contains(it.isrc) }.toSet()
    }

    // Always records the label itself (and any copyright it just learned), even with no new tracks.
    fun markAsKnown(tracks: Set<Track>, label: Label) {
        ourInfoGateway.saveTracks(tracks, label)
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
