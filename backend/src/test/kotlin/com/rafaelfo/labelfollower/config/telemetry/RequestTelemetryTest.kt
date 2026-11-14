package com.rafaelfo.labelfollower.config.telemetry

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.rafaelfo.labelfollower.usecases.telemetry.MetricCatalog
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.SpringWebMvcTelemetry
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver

private const val TEST_SERVICE = "test-service"

class RequestTelemetryTest : DescribeSpec({

    lateinit var events: ListAppender<ILoggingEvent>
    lateinit var logger: Logger

    beforeTest {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        logger = context.getLogger(RequestTelemetryFilter::class.java)
        events = ListAppender()
        events.context = context
        events.start()
        logger.addAppender(events)
    }

    afterTest {
        logger.detachAppender(events)
    }

    describe("the servlet telemetry boundary") {

        it("emits one WARN per mapped 4xx with no exception semconv") {
            val harness = harness()

            harness.mockMvc.perform(get("/status/404"))

            val event = events.list.single()
            event.level shouldBe Level.WARN
            event.message shouldBe TelemetryAttributes.HTTP_REQUEST_FAILED
            event.throwableProxy.shouldBeNull()
            event.mdcPropertyMap[TelemetryAttributes.HTTP_REQUEST_METHOD] shouldBe "GET"
            event.mdcPropertyMap[TelemetryAttributes.HTTP_RESPONSE_STATUS_CODE] shouldBe "404"
            event.mdcPropertyMap[TelemetryAttributes.ERROR_TYPE] shouldBe TelemetryAttributes.RESOURCE_NOT_FOUND
        }

        it("maps every stable 4xx transport status to its bounded error code") {
            val harness = harness()

            val expected = mapOf(
                "400" to TelemetryAttributes.INVALID_ARGUMENT,
                "401" to TelemetryAttributes.UNAUTHENTICATED,
                "403" to TelemetryAttributes.PERMISSION_DENIED,
                "404" to TelemetryAttributes.RESOURCE_NOT_FOUND,
                "409" to TelemetryAttributes.CONFLICT,
                "422" to TelemetryAttributes.INVALID_ARGUMENT,
            )
            for ((status, _) in expected) {
                harness.mockMvc.perform(get("/status/$status"))
            }

            events.list.map {
                it.mdcPropertyMap[TelemetryAttributes.HTTP_RESPONSE_STATUS_CODE] to
                    it.mdcPropertyMap[TelemetryAttributes.ERROR_TYPE]
            } shouldBe expected.map { it.key to it.value }
        }

        it("emits one ERROR with exception semconv for a 5xx") {
            val harness = harness()

            harness.mockMvc.perform(get("/boom"))

            val event = events.list.single()
            event.level shouldBe Level.ERROR
            event.message shouldBe TelemetryAttributes.HTTP_REQUEST_FAILED
            event.throwableProxy.shouldNotBeNull().className shouldBe "java.lang.IllegalStateException"
            event.mdcPropertyMap[TelemetryAttributes.ERROR_TYPE] shouldBe "IllegalStateException"
            event.mdcPropertyMap[TelemetryAttributes.HTTP_RESPONSE_STATUS_CODE] shouldBe "500"
        }

        it("emits no per-request record for a success") {
            val harness = harness()

            harness.mockMvc.perform(get("/ok"))

            events.list.shouldBeEmpty()
        }

        it("echoes a valid inbound id and replaces an invalid one") {
            val harness = harness()

            harness.mockMvc.perform(
                get("/ok").header(TelemetryAttributes.REQUEST_ID_HEADER, "valid-client-id-1"),
            ).andReturn().response.getHeader(TelemetryAttributes.REQUEST_ID_HEADER) shouldBe "valid-client-id-1"

            harness.mockMvc.perform(
                get("/status/404").header(TelemetryAttributes.REQUEST_ID_HEADER, "short"),
            ).andReturn().response.getHeader(TelemetryAttributes.REQUEST_ID_HEADER) shouldMatch
                RequestId.GENERATED_REQUEST_ID
        }

        it("puts request.id on the boundary record and echoes it on the error response") {
            val harness = harness()

            val response = harness.mockMvc.perform(
                get("/status/404").header(TelemetryAttributes.REQUEST_ID_HEADER, "client-request-1"),
            ).andReturn().response

            response.getHeader(TelemetryAttributes.REQUEST_ID_HEADER) shouldBe "client-request-1"
            events.list.single().mdcPropertyMap[TelemetryAttributes.REQUEST_ID] shouldBe "client-request-1"
        }

        it("leaves http.route absent on an unmatched 404 and falls back to url.path") {
            val harness = harness()

            harness.mockMvc.perform(get("/unknown")).andReturn()

            val event = events.list.single()
            event.mdcPropertyMap.containsKey(TelemetryAttributes.HTTP_ROUTE) shouldBe false
            event.mdcPropertyMap[TelemetryAttributes.URL_PATH] shouldBe "/unknown"
        }

        it("records the matched route pattern on a known endpoint") {
            val harness = harness()

            harness.mockMvc.perform(get("/status/404"))

            events.list.single().mdcPropertyMap[TelemetryAttributes.HTTP_ROUTE] shouldBe "/status/{code}"
        }

        it("returns the in-flight gauge to zero and keeps request.id off every metric") {
            val harness = harness()

            harness.mockMvc.perform(get("/ok"))

            val metrics = harness.reader.collectAllMetrics()
            val active = metrics.single { it.name == MetricCatalog.HTTP_SERVER_ACTIVE_REQUESTS }
            active.unit shouldBe MetricCatalog.REQUESTS
            active.longSumData.points.sumOf { it.value } shouldBe 0L
            metrics.allAttributeKeys().shouldNotContain(TelemetryAttributes.REQUEST_ID)
            metrics.map { it.name }.shouldNotContain(MetricCatalog.DROPPED_HTTP_SERVER_COUNT)
        }

        it("emits the server duration histogram with the catalog unit and buckets") {
            val harness = harness()

            harness.mockMvc.perform(get("/ok"))

            val duration = harness.reader.collectAllMetrics()
                .single { it.name == MetricCatalog.HTTP_SERVER_DURATION }
            duration.unit shouldBe MetricCatalog.SECONDS
            duration.histogramData.points.single().boundaries shouldBe MetricCatalog.HTTP_DURATION_BUCKETS
            // The route pattern reaches the framework's handler mapping in a real servlet
            // context; the standalone harness has no DispatcherServlet mapping to resolve, so
            // the log-record test above asserts the same attribute the metric reads.
            duration.histogramData.points.single().count shouldBe 1L
        }

        it("does not expose a /metrics endpoint") {
            val harness = harness()

            harness.mockMvc.perform(get("/metrics")).andReturn().response.status shouldBe 404
        }
    }
})

private class Harness(val mockMvc: MockMvc, val reader: InMemoryMetricReader)

private fun harness(): Harness {
    val reader = InMemoryMetricReader.create()
    val sdk = telemetrySdk(reader)
    val boundary = BoundaryTelemetry()
    val otelFilter = SpringWebMvcTelemetry.create(sdk).createServletFilter()
    val requestFilter = RequestTelemetryFilter(sdk.meterBuilder(TEST_SERVICE).build())
    val builder = MockMvcBuilders.standaloneSetup(TelemetryTestController())
    builder.addFilters<StandaloneMockMvcBuilder>(otelFilter, requestFilter)
    builder.setHandlerExceptionResolvers(boundary, DefaultHandlerExceptionResolver())
    val mockMvc = builder.build()
    return Harness(mockMvc, reader)
}

@RestController
private class TelemetryTestController {

    @GetMapping("/ok")
    fun ok() = ResponseEntity.ok("ok")

    @GetMapping("/boom")
    fun boom(): ResponseEntity<String> = throw IllegalStateException("boom")

    @GetMapping("/status/{code}")
    fun status(@PathVariable code: Int) = ResponseEntity.status(HttpStatus.valueOf(code)).build<String>()
}

private fun Collection<MetricData>.allAttributeKeys(): List<String> = flatMap { metric ->
    metric.data.points.flatMap { point -> point.attributes.asMap().keys.map { it.key } }
}
