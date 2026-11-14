package com.rafaelfo.labelfollower.integrations.database

import com.rafaelfo.labelfollower.usecases.health.HealthCheckResult
import com.rafaelfo.labelfollower.usecases.health.HealthChecker
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

/**
 * The MySQL component the public health endpoint reports. Every failure mode — refused
 * connection, bad credentials, a wedged server — must degrade the report, never the endpoint,
 * so the catch stays generic on purpose.
 */
@Component
class MySqlConnectionHealthCheck(
    private val database: Database,
    private val clock: Clock,
) : HealthChecker {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun getHealthStatus() = HealthCheckResult(
        serviceName = "mysql-connection",
        isHealthy = try {
            executeQuery()
            true
        } catch (e: Exception) {
            false
        },
        timestamp = LocalDateTime.now(clock),
    )

    private fun executeQuery() {
        transaction(database) {
            exec("SELECT 1") { it.next() }
        }
    }
}
