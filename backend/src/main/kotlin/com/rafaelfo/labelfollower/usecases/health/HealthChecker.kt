package com.rafaelfo.labelfollower.usecases.health

interface HealthChecker {

    fun getHealthStatus(): HealthCheckResult
}
