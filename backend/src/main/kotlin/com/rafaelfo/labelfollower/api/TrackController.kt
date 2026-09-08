package com.rafaelfo.labelfollower.api

import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.TrackFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/track")
class TrackController(
    private val trackFinder: TrackFinder,
) {

    @GetMapping("/{isrc}")
    fun execute(@PathVariable isrc: String): Track = trackFinder.findBy(isrc)
}
