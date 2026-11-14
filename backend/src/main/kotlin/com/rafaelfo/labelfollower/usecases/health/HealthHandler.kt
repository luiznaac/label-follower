package com.rafaelfo.labelfollower.usecases.health

import org.springframework.stereotype.Service

/**
 * Aggregates every registered health component and records each result once, so the controller
 * stays a thin transport boundary. Mirrors the scaffold's `HealthHandler`.
 */
@Service
class HealthHandler(
    private val healthCheckers: Set<HealthChecker>,
    private val healthTelemetry: HealthTelemetry,
) {

    fun check(): List<HealthCheckResult> {
        val results = healthCheckers.map { it.getHealthStatus() }
        for (result in results) {
            healthTelemetry.record(result)
        }
        return results
    }
}
