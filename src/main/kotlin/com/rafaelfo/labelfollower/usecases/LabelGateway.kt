package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track

interface LabelGateway {

    fun getTracksFrom(label: Label): Set<Track>
}
