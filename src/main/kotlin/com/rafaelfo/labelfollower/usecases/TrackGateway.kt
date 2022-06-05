package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track

interface TrackGateway {

    fun getLabel(track: Track): Label

    fun findTrackBy(isrc: String): Track
}
