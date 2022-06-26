package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Track
import org.springframework.stereotype.Service

@Service
class Consolidator(
    private val ourInfoGateway: OurInfoGateway,
    private val labelIntrospector: LabelIntrospector,
) {

    fun introspectAllLabelsAndNotify() {
        ourInfoGateway.getLabels()
            .associate { it.name to labelIntrospector.discoverNewTracksFrom(it) }
            .filter { it.value.isNotEmpty() }
            .also { notify(it) }
    }

    private fun notify(newTracks: Map<String, Set<Track>>) {
        newTracks.forEach {
            println(it)
        }
    }
}
