package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track

interface ExternalInfoGateway {

    fun getLabel(isrc: String): Label

    fun findTrackBy(isrc: String): Track

    fun getTracksFrom(label: Label): Set<Track>
}
