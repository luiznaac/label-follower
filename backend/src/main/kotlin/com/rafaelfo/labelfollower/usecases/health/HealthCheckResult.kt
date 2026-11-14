package com.rafaelfo.labelfollower.usecases.health

import java.time.LocalDateTime

data class HealthCheckResult(
    val serviceName: String,
    val isHealthy: Boolean,
    val timestamp: LocalDateTime,
)
