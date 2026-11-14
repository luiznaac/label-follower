package com.rafaelfo.labelfollower.config.telemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.exporter.logging.LoggingMetricExporter
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.instrumentation.runtimetelemetry.RuntimeTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import java.util.concurrent.TimeUnit

private const val LOG_RECORD_QUEUE_SIZE = 2048
private const val EXPORT_TIMEOUT_SECONDS = 10L
private const val SHUTDOWN_DEADLINE_SECONDS = 10L
private const val LOGS_PATH = "/v1/logs"
private const val METRICS_PATH = "/v1/metrics"

class TelemetryRuntime(
    val openTelemetry: OpenTelemetrySdk,
    private val runtimeTelemetry: RuntimeTelemetry,
) : AutoCloseable {

    fun shutdown(): Boolean {
        runtimeTelemetry.close()
        return openTelemetry.shutdown().join(SHUTDOWN_DEADLINE_SECONDS, TimeUnit.SECONDS).isSuccess
    }

    override fun close() {
        shutdown()
    }
}

/**
 * Builds the OTel SDK from the resolved settings. No `TracerProvider` and no trace exporter are
 * configured — traces are outside this contract. `docs/observability.md` §1 and §6.
 */
object Telemetry {

    fun build(settings: TelemetrySettings): TelemetryRuntime {
        TelemetryResourceValues.install(settings)
        val resource = TelemetryResource.of(settings)
        val warning = ExportWarning()
        val loggerProvider = buildLoggerProvider(settings, resource, warning)
        val meterProvider = buildMeterProvider(settings, resource, warning)
        val sdk = OpenTelemetrySdk.builder()
            .setLoggerProvider(loggerProvider)
            .setMeterProvider(meterProvider)
            .build()
        return TelemetryRuntime(sdk, RuntimeTelemetry.create(sdk))
    }

    fun meter(openTelemetry: OpenTelemetry, settings: TelemetrySettings): Meter =
        openTelemetry.meterBuilder(settings.serviceName)
            .setInstrumentationVersion(settings.version)
            .build()

    private fun buildLoggerProvider(
        settings: TelemetrySettings,
        resource: Resource,
        warning: ExportWarning,
    ): SdkLoggerProvider {
        val builder = SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(EventNameStampingProcessor())
        val processor = logProcessor(settings, warning)
        if (processor != null) {
            builder.addLogRecordProcessor(processor)
        }
        return builder.build()
    }

    private fun logProcessor(settings: TelemetrySettings, warning: ExportWarning): LogRecordProcessor? {
        val exporter = when (settings.pipelines.logs) {
            TelemetryExporter.OTLP -> WarnOnceLogRecordExporter(otlpLogExporter(settings), warning)
            TelemetryExporter.CONSOLE -> SystemOutLogRecordExporter.create()
            TelemetryExporter.NONE -> null
        } ?: return null
        return if (settings.pipelines.logs == TelemetryExporter.OTLP) {
            batchLogProcessor(exporter, settings)
        } else {
            SimpleLogRecordProcessor.create(exporter)
        }
    }

    private fun buildMeterProvider(
        settings: TelemetrySettings,
        resource: Resource,
        warning: ExportWarning,
    ): SdkMeterProvider {
        val builder = SdkMeterProvider.builder().setResource(resource)
        for ((selector, view) in TelemetryViews.views) {
            builder.registerView(selector, view)
        }
        val exporter = when (settings.pipelines.metrics) {
            TelemetryExporter.OTLP -> WarnOnceMetricExporter(otlpMetricExporter(settings), warning)
            TelemetryExporter.CONSOLE -> LoggingMetricExporter.create()
            TelemetryExporter.NONE -> null
        }
        if (exporter != null) {
            builder.registerMetricReader(
                PeriodicMetricReader.builder(exporter)
                    .setInterval(settings.metricExportIntervalMillis, TimeUnit.MILLISECONDS)
                    .build(),
            )
        }
        return builder.build()
    }

    private fun otlpLogExporter(settings: TelemetrySettings): LogRecordExporter = OtlpHttpLogRecordExporter.builder()
        .setEndpoint(endpoint(settings, LOGS_PATH))
        .setTimeout(EXPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private fun otlpMetricExporter(settings: TelemetrySettings): MetricExporter = OtlpHttpMetricExporter.builder()
        .setEndpoint(endpoint(settings, METRICS_PATH))
        .setTimeout(EXPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .setAggregationTemporalitySelector(AggregationTemporalitySelector.alwaysCumulative())
        .build()

    private fun batchLogProcessor(exporter: LogRecordExporter, settings: TelemetrySettings) =
        BatchLogRecordProcessor.builder(exporter)
            .setScheduleDelay(settings.logScheduleDelayMillis, TimeUnit.MILLISECONDS)
            .setMaxQueueSize(LOG_RECORD_QUEUE_SIZE)
            .setExporterTimeout(EXPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    private fun endpoint(settings: TelemetrySettings, path: String) =
        "${settings.endpoint.orEmpty().trimEnd('/')}$path"
}
