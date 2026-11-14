package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.telemetry.EventLevel
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import com.rafaelfo.labelfollower.usecases.telemetry.event
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Consolidator(
    private val ourInfoGateway: OurInfoGateway,
    private val labelIntrospector: LabelIntrospector,
    private val userPlaylistGateway: UserInfoGateway,
) {

    private val log = LoggerFactory.getLogger(Consolidator::class.java)

    fun introspectAllLabelsAndNotify() {
        ourInfoGateway.getLabels()
            .associateWith { labelIntrospector.discoverNewTracksFrom(it) }
            .filter { it.value.isNotEmpty() }
            .onEach { userPlaylistGateway.createPlaylistWith(it.key, it.value) }
            .also { notify(it) }
    }

    private fun notify(newTracks: Map<Label, Set<Track>>) {
        for ((label, tracks) in newTracks) {
            log.event(
                TelemetryAttributes.LABELS_CONSOLIDATED,
                "label.name" to label.name,
                "new_tracks.count" to tracks.size,
                level = EventLevel.DEBUG,
            )
        }
    }
}
