package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track

interface OurInfoGateway {

    fun getTracksFrom(label: Label): Set<Track>

    fun saveTracks(tracks: Set<Track>, label: Label)

    fun getLabels(): Set<Label>
}
