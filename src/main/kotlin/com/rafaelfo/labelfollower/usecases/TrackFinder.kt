package com.rafaelfo.labelfollower.usecases

import org.springframework.stereotype.Service

@Service
class TrackFinder(
    private val trackGateway: TrackGateway,
) {

    fun findBy(isrc: String) = trackGateway.findTrackBy(isrc)
}
