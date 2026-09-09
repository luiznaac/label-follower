package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import org.springframework.stereotype.Service

@Service
class Consolidator(
    private val ourInfoGateway: OurInfoGateway,
    private val labelIntrospector: LabelIntrospector,
    private val userPlaylistGateway: UserInfoGateway,
) {

    fun introspectAllLabelsAndNotify() {
        ourInfoGateway.getLabels()
            .associateWith { labelIntrospector.discoverNewTracksFrom(it) }
            .filter { it.value.isNotEmpty() }
            .onEach { userPlaylistGateway.createPlaylistWith(it.key, it.value) }
            .also { notify(it) }
    }

    private fun notify(newTracks: Map<Label, Set<Track>>) {
        newTracks.forEach {
            println(it)
        }
    }
}
