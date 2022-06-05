package com.rafaelfo.labelfollower.api

import com.rafaelfo.labelfollower.usecases.LabelIntrospector
import com.rafaelfo.labelfollower.usecases.TrackFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/introspect")
class IntrospectController(
    private val trackFinder: TrackFinder,
    private val labelIntrospector: LabelIntrospector,
) {

    @GetMapping("/fromTrack/{isrc}")
    fun execute(@PathVariable isrc: String) =
        labelIntrospector.introspectFrom(
            trackFinder.findBy(isrc)
        )
}
