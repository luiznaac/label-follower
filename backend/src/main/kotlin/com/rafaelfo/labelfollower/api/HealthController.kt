package com.rafaelfo.labelfollower.api

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.rafaelfo.labelfollower.usecases.health.HealthCheckResult
import com.rafaelfo.labelfollower.usecases.health.HealthHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

// The minimal health surface the observability standard assumes: `GET /health` aggregates every
// registered checker and records each result, `GET /health/internal` is the cheap liveness probe.
// The wire shape is the scaffold's snake_case contract (docs/observability.md §8), unlike the
// rest of this camelCase API, so the family's probes read the same body everywhere.
@RestController
@RequestMapping("/health")
class HealthController(
    private val healthHandler: HealthHandler,
) {

    @GetMapping
    fun health(): List<HealthCheckResponse> =
        healthHandler.check().map(HealthCheckResponse::fromDomain)

    @GetMapping("/internal")
    fun internal(): HealthInternalResponse = HealthInternalResponse(isHealthy = true)
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class HealthCheckResponse(
    val serviceName: String,
    val isHealthy: Boolean,
    val timestamp: LocalDateTime,
) {

    companion object {
        fun fromDomain(result: HealthCheckResult) = HealthCheckResponse(
            serviceName = result.serviceName,
            isHealthy = result.isHealthy,
            timestamp = result.timestamp,
        )
    }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class HealthInternalResponse(val isHealthy: Boolean)
