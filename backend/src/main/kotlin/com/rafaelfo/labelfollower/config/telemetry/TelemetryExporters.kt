package com.rafaelfo.labelfollower.config.telemetry

import com.rafaelfo.labelfollower.usecases.telemetry.EventLevel
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import com.rafaelfo.labelfollower.usecases.telemetry.event
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stamps `event.name` from the body when a plain framework log carries none, so framework
 * records are still routable by event name. `docs/observability.md` §2 (Bridge, not replace).
 */
class EventNameStampingProcessor : LogRecordProcessor {

    private val eventNameKey = AttributeKey.stringKey(TelemetryAttributes.EVENT_NAME)

    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        if (logRecord.getAttribute(eventNameKey) != null) {
            return
        }
        val body = logRecord.getBodyValue()?.asString()
        if (!body.isNullOrBlank()) {
            logRecord.setAttribute(eventNameKey, body)
        }
    }

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun forceFlush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun close() = Unit
}

private const val SIGNAL_ATTRIBUTE = "signal"
private const val COLLECTOR_UNREACHABLE = "collector_unreachable"

/**
 * One WARN per export outage — a configured-but-down Collector degrades, it never fails a
 * request — and the flag re-arms after a successful export. `docs/observability.md` §1.
 */
class ExportWarning(
    private val log: Logger = LoggerFactory.getLogger(ExportWarning::class.java),
) {

    private val warned = AtomicBoolean(false)

    fun report(result: CompletableResultCode, signal: String) {
        if (result.isSuccess) {
            warned.set(false)
        } else if (warned.compareAndSet(false, true)) {
            log.event(
                TelemetryAttributes.TELEMETRY_EXPORT_FAILED,
                TelemetryAttributes.ERROR_TYPE to COLLECTOR_UNREACHABLE,
                SIGNAL_ATTRIBUTE to signal,
                level = EventLevel.WARN,
            )
        }
    }
}

class WarnOnceLogRecordExporter(
    private val delegate: LogRecordExporter,
    private val warning: ExportWarning,
) : LogRecordExporter {

    override fun export(logRecords: Collection<LogRecordData>): CompletableResultCode {
        val result = delegate.export(logRecords)
        result.whenComplete { warning.report(result, "logs") }
        return result
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()
}

class WarnOnceMetricExporter(
    private val delegate: MetricExporter,
    private val warning: ExportWarning,
) : MetricExporter {

    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        val result = delegate.export(metrics)
        result.whenComplete { warning.report(result, "metrics") }
        return result
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()

    override fun getAggregationTemporality(instrumentType: InstrumentType) =
        delegate.getAggregationTemporality(instrumentType)

    override fun getDefaultAggregation(instrumentType: InstrumentType) = delegate.getDefaultAggregation(instrumentType)

    override fun getMemoryMode() = delegate.getMemoryMode()
}
