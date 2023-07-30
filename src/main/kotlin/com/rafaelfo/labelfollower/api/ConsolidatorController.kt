package com.rafaelfo.labelfollower.api

import com.rafaelfo.labelfollower.usecases.Consolidator
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/consolidate")
class ConsolidatorController(
    private val consolidator: Consolidator,
) {

    @PostMapping
    fun execute(@RequestHeader(HttpHeaders.AUTHORIZATION) token: String) {
        consolidator.introspectAllLabelsAndNotify(token.removePrefix("Bearer "))
    }
}
