package com.rafaelfo.labelfollower.config.telemetry

import java.net.InetAddress
import java.util.Properties
import java.util.UUID

enum class TelemetryExporter { OTLP, CONSOLE, NONE }

data class TelemetryPipelines(
    val logs: TelemetryExporter,
    val metrics: TelemetryExporter,
) {
    val traces: TelemetryExporter get() = TelemetryExporter.NONE
}

/**
 * The resolved telemetry configuration: one object built from the Spring environment and the
 * standard `OTEL_*` variables, so the pipeline decision is testable without a Spring context.
 */
data class TelemetrySettings(
    val serviceName: String,
    val endpoint: String?,
    val console: Boolean,
    val environment: String,
    val version: String,
    val hostName: String,
    val instanceId: String = UUID.randomUUID().toString(),
    val resourceAttributes: Map<String, String> = emptyMap(),
    val logScheduleDelayMillis: Long = DEFAULT_LOG_SCHEDULE_DELAY_MILLIS,
    val metricExportIntervalMillis: Long = DEFAULT_METRIC_EXPORT_INTERVAL_MILLIS,
    val logExporter: TelemetryExporter? = null,
    val metricExporter: TelemetryExporter? = null,
) {

    val pipelines: TelemetryPipelines
        get() {
            if (environment == TEST_ENVIRONMENT) {
                return TelemetryPipelines(TelemetryExporter.NONE, TelemetryExporter.NONE)
            }
            if (console) {
                return TelemetryPipelines(TelemetryExporter.CONSOLE, TelemetryExporter.CONSOLE)
            }
            return TelemetryPipelines(
                logExporter ?: TelemetryExporter.OTLP,
                metricExporter ?: TelemetryExporter.OTLP,
            )
        }

    companion object {
        const val PROD_ENVIRONMENT = "prod"
        const val DEV_ENVIRONMENT = "dev"
        const val TEST_ENVIRONMENT = "test"
        const val DEV_DEFAULT_ENDPOINT = "http://localhost:4318"
        const val SERVICE_NAMESPACE = "luiznaac"
        private const val DEFAULT_LOG_SCHEDULE_DELAY_MILLIS = 5000L
        private const val DEFAULT_METRIC_EXPORT_INTERVAL_MILLIS = 60000L

        fun resolve(
            activeProfiles: List<String>,
            serviceName: String,
            endpoint: String?,
            console: Boolean,
            environment: Map<String, String> = System.getenv(),
            version: String = buildVersion(),
        ): TelemetrySettings {
            val environmentName = environmentName(activeProfiles)
            require(!(environmentName == PROD_ENVIRONMENT && console)) {
                "TELEMETRY_CONSOLE is a dev-only switch and cannot be enabled in prod"
            }
            val tracesExporter = environment["OTEL_TRACES_EXPORTER"] ?: "none"
            require(tracesExporter == "none") {
                "traces are not part of this contract; OTEL_TRACES_EXPORTER must be none"
            }
            val resolvedEndpoint = endpoint?.takeIf { it.isNotBlank() }
                ?: resolveDevEndpoint(environmentName)
            val logExporter = exporterOverride(environment["OTEL_LOGS_EXPORTER"], "OTEL_LOGS_EXPORTER")
            val metricExporter = exporterOverride(environment["OTEL_METRICS_EXPORTER"], "OTEL_METRICS_EXPORTER")
            require(!(environmentName == PROD_ENVIRONMENT && logExporter == TelemetryExporter.CONSOLE)) {
                "console exporters are dev-only"
            }
            require(!(environmentName == PROD_ENVIRONMENT && metricExporter == TelemetryExporter.CONSOLE)) {
                "console exporters are dev-only"
            }
            return TelemetrySettings(
                serviceName = environment["OTEL_SERVICE_NAME"] ?: serviceName,
                endpoint = resolvedEndpoint,
                console = console,
                environment = environmentName,
                version = environment["SERVICE_VERSION"] ?: version,
                hostName = environment["HOSTNAME"] ?: localHostName(),
                resourceAttributes = parseResourceAttributes(environment["OTEL_RESOURCE_ATTRIBUTES"]),
                logScheduleDelayMillis = positiveLong(environment["OTEL_BLRP_SCHEDULE_DELAY"])
                    ?: DEFAULT_LOG_SCHEDULE_DELAY_MILLIS,
                metricExportIntervalMillis = positiveLong(environment["OTEL_METRIC_EXPORT_INTERVAL"])
                    ?: DEFAULT_METRIC_EXPORT_INTERVAL_MILLIS,
                logExporter = logExporter,
                metricExporter = metricExporter,
            )
        }

        fun environmentName(activeProfiles: List<String>): String = when (val first = activeProfiles.firstOrNull()) {
            null -> DEV_ENVIRONMENT
            "prod", "production" -> PROD_ENVIRONMENT
            else -> first
        }

        private fun resolveDevEndpoint(environmentName: String): String? {
            require(environmentName != PROD_ENVIRONMENT) {
                "OTEL_EXPORTER_OTLP_ENDPOINT is required under the prod profile"
            }
            return DEV_DEFAULT_ENDPOINT
        }

        private fun parseResourceAttributes(raw: String?): Map<String, String> = raw
            ?.split(',')
            ?.mapNotNull { entry ->
                val separator = entry.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    entry.substring(0, separator).trim() to entry.substring(separator + 1).trim()
                }
            }
            ?.toMap()
            .orEmpty()

        private fun positiveLong(raw: String?): Long? = raw?.toLongOrNull()?.takeIf { it > 0 }

        private fun exporterOverride(raw: String?, name: String): TelemetryExporter? = when (raw?.lowercase()) {
            null -> null
            "otlp" -> TelemetryExporter.OTLP
            "console" -> TelemetryExporter.CONSOLE
            "none" -> TelemetryExporter.NONE
            else -> throw IllegalArgumentException("$name has an unsupported value: $raw")
        }

        private fun localHostName(): String = runCatching { InetAddress.getLocalHost().hostName }
            .getOrDefault("unknown")

        fun buildVersion(): String = runCatching {
            val stream = TelemetrySettings::class.java.getResourceAsStream("/build-info.properties")
                ?: return@runCatching null
            stream.use { Properties().apply { load(it) }.getProperty("version") }
        }.getOrNull() ?: "dev"
    }
}
