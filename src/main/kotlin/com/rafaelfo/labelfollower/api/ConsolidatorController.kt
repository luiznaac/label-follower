package com.rafaelfo.labelfollower.api

import com.rafaelfo.labelfollower.usecases.Consolidator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/consolidate")
class ConsolidatorController(
    private val consolidator: Consolidator,
) {

    @GetMapping
    fun execute() {
        consolidator.introspectAllLabelsAndNotify()
    }
}
