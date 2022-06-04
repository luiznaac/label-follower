package com.rafaelfo.labelfollower.usecases

class TrackFinder(
    private val trackGateway: TrackGateway,
) {

    fun findBy(isrc: String) = trackGateway.findTrackBy(isrc)
}
