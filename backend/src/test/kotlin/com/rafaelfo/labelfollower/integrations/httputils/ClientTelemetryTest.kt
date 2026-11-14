package com.rafaelfo.labelfollower.integrations.httputils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.rafaelfo.labelfollower.usecases.telemetry.MetricCatalog
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class ClientTelemetryTest : StringSpec({

    "propagates the request id on the outbound call" {
        withServer { server, rafaHttp ->
            MDC.put(TelemetryAttributes.REQUEST_ID, "client-request-1")

            rafaHttp.get(origin(server), "service", emptyMap())

            server.takeRequest().getHeader(TelemetryAttributes.REQUEST_ID_HEADER) shouldBe "client-request-1"
        }
        MDC.clear()
    }

    "omits the header when the call is outside a request context" {
        withServer { server, rafaHttp ->
            rafaHttp.get(origin(server), "service", emptyMap())

            server.takeRequest().getHeader(TelemetryAttributes.REQUEST_ID_HEADER).shouldBeNull()
        }
    }

    "records the client duration histogram with the catalog unit and buckets" {
        val reader = InMemoryMetricReader.create()
        withServer(reader) { server, rafaHttp ->
            rafaHttp.get(origin(server), "service", emptyMap())

            val duration = reader.collectAllMetrics().single { it.name == MetricCatalog.HTTP_CLIENT_DURATION }
            duration.unit shouldBe MetricCatalog.SECONDS
            duration.histogramData.points.single().boundaries shouldBe MetricCatalog.HTTP_DURATION_BUCKETS
        }
    }

    "emits a structured DEBUG record that never carries the authorization header" {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val logger = context.getLogger(RafaHttp::class.java)
        logger.level = Level.DEBUG
        val events = ListAppender<ILoggingEvent>()
        events.context = context
        events.start()
        logger.addAppender(events)

        try {
            withServer { server, rafaHttp ->
                rafaHttp.get(origin(server), "service", mapOf("Authorization" to "Bearer top-secret"))

                val event = events.list.single()
                event.level shouldBe Level.DEBUG
                event.message shouldBe TelemetryAttributes.HTTP_REQUEST_SENT
                event.mdcPropertyMap[TelemetryAttributes.HTTP_REQUEST_METHOD] shouldBe "GET"
                event.mdcPropertyMap[TelemetryAttributes.SERVER_ADDRESS] shouldBe "localhost"
                event.mdcPropertyMap[TelemetryAttributes.URL_PATH] shouldBe "/service"
                event.mdcPropertyMap.keys shouldNotContain "authorization"
                event.formattedMessage.contains("top-secret") shouldBe false
            }
        } finally {
            logger.detachAppender(events)
        }
    }
})

private fun origin(server: MockWebServer) = "http://${server.hostName}:${server.port}"

private fun withServer(
    reader: InMemoryMetricReader = InMemoryMetricReader.create(),
    block: (MockWebServer, RafaHttp) -> Unit,
) {
    val server = MockWebServer()
    server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
    server.start()
    try {
        block(server, RafaHttp(client(reader)))
    } finally {
        server.shutdown()
    }
}

private fun client(reader: InMemoryMetricReader): Call.Factory {
    val sdk = OpenTelemetrySdk.builder()
        .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
        .build()
    val delegate = OkHttpClient.Builder().addInterceptor(RequestIdInterceptor()).build()
    return OkHttpTelemetry.create(sdk).createCallFactory(delegate)
}
