package com.rafaelfo.labelfollower.config.telemetry

import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader

/** The in-memory SDK every telemetry contract test asserts through. */
internal fun telemetryMeterProvider(reader: InMemoryMetricReader): SdkMeterProvider =
    SdkMeterProvider.builder()
        .registerMetricReader(reader)
        .apply {
            for ((selector, view) in TelemetryViews.views) {
                registerView(selector, view)
            }
        }
        .build()

internal fun telemetrySdk(reader: InMemoryMetricReader): OpenTelemetrySdk =
    OpenTelemetrySdk.builder().setMeterProvider(telemetryMeterProvider(reader)).build()
