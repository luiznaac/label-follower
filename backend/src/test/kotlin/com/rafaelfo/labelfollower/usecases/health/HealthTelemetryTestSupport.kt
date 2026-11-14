package com.rafaelfo.labelfollower.usecases.health

import com.rafaelfo.labelfollower.usecases.telemetry.MetricCatalog
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import java.time.LocalDateTime

internal val healthComponentKey = AttributeKey.stringKey(TelemetryAttributes.HEALTH_COMPONENT)

/** The in-memory SDK every health contract test asserts through. */
internal fun healthTelemetrySdk(reader: InMemoryMetricReader): OpenTelemetrySdk =
    OpenTelemetrySdk.builder()
        .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
        .build()

internal fun healthCheckResult(serviceName: String, isHealthy: Boolean) = HealthCheckResult(
    serviceName = serviceName,
    isHealthy = isHealthy,
    timestamp = LocalDateTime.of(2026, 1, 1, 0, 0),
)

/** Reads the last observed `health.check.status` per component from the in-memory reader. */
internal fun healthGaugeValues(reader: InMemoryMetricReader): Map<String, Long> =
    reader.collectAllMetrics()
        .single { it.name == MetricCatalog.HEALTH_STATUS }
        .longGaugeData.points
        .associate { (it.attributes.get(healthComponentKey) ?: "") to it.value }
