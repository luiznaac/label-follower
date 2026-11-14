package com.rafaelfo.labelfollower.usecases.health

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.rafaelfo.labelfollower.usecases.telemetry.MetricCatalog
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.slf4j.LoggerFactory

private val errorTypeKey = AttributeKey.stringKey(TelemetryAttributes.ERROR_TYPE)

class HealthTelemetryTest : StringSpec({

    lateinit var reader: InMemoryMetricReader
    lateinit var telemetry: HealthTelemetry
    lateinit var logger: Logger
    lateinit var events: ListAppender<ILoggingEvent>

    beforeTest {
        val metricReader = InMemoryMetricReader.create()
        reader = metricReader
        telemetry = HealthTelemetry(healthTelemetrySdk(metricReader).meterBuilder("test-service").build())

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        logger = context.getLogger(HealthTelemetry::class.java)
        events = ListAppender()
        events.context = context
        events.start()
        logger.addAppender(events)
    }

    afterTest {
        logger.detachAppender(events)
    }

    "reports the last observed status per component as a 0/1 gauge" {
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = true))
        telemetry.record(healthCheckResult("app-health", isHealthy = false))

        healthGaugeValues(reader) shouldBe mapOf("mysql-connection" to 1L, "app-health" to 0L)

        telemetry.record(healthCheckResult("mysql-connection", isHealthy = false))

        healthGaugeValues(reader) shouldBe mapOf("mysql-connection" to 0L, "app-health" to 0L)
    }

    "increments the failure counter once per failed check with the component and error type" {
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = false))
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = false))

        val counter = metric(reader, MetricCatalog.HEALTH_FAILURES)
        counter.unit shouldBe MetricCatalog.CHECKS
        val points = counter.longSumData.points.single()
        points.value shouldBe 2L
        points.attributes.get(healthComponentKey) shouldBe "mysql-connection"
        points.attributes.get(errorTypeKey) shouldBe HealthTelemetry.FAILURE_ERROR_TYPE
    }

    "logs one WARN on the healthy-to-unhealthy transition and one INFO on recovery" {
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = true))
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = false))
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = false))
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = true))

        events.list.map { it.level to it.message } shouldBe listOf(
            Level.WARN to "health.check.failed",
            Level.INFO to "health.check.recovered",
        )
        events.list.first().mdcPropertyMap[TelemetryAttributes.HEALTH_COMPONENT] shouldBe "mysql-connection"
    }

    "never logs a per-check INFO record" {
        telemetry.record(healthCheckResult("app-health", isHealthy = true))
        telemetry.record(healthCheckResult("app-health", isHealthy = true))

        events.list.shouldBeEmpty()
    }

    "the gauge callback never performs I/O — collection only reads the last observation" {
        telemetry.record(healthCheckResult("mysql-connection", isHealthy = true))

        val first = healthGaugeValues(reader)
        val second = healthGaugeValues(reader)

        first shouldBe second
    }
})

private fun metric(reader: InMemoryMetricReader, name: String): MetricData = reader.collectAllMetrics()
    .single { it.name == name }
