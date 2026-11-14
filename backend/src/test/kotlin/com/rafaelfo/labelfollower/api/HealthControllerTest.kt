package com.rafaelfo.labelfollower.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.rafaelfo.labelfollower.usecases.health.HealthChecker
import com.rafaelfo.labelfollower.usecases.health.HealthHandler
import com.rafaelfo.labelfollower.usecases.health.HealthTelemetry
import com.rafaelfo.labelfollower.usecases.health.healthCheckResult
import com.rafaelfo.labelfollower.usecases.health.healthGaugeValues
import com.rafaelfo.labelfollower.usecases.health.healthTelemetrySdk
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

private const val TEST_SERVICE = "test-service"

class HealthControllerTest : DescribeSpec({

    describe("the health endpoints") {

        it("reports each component with the documented body and records the result") {
            val harness = harness(checker("mysql-connection", isHealthy = true))

            val response = harness.mockMvc.perform(get("/health"))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().response
            val component = harness.mapper.readTree(response.contentAsString)[0]

            component["service_name"].asText() shouldBe "mysql-connection"
            component["is_healthy"].asBoolean() shouldBe true
            component["timestamp"].asText() shouldBe "2026-01-01T00:00:00"
            harness.gaugeValues() shouldBe mapOf("mysql-connection" to 1L)
        }

        it("reports an unhealthy component without changing the endpoint status code") {
            val harness = harness(checker("mysql-connection", isHealthy = false))

            val response = harness.mockMvc.perform(get("/health"))
                .andExpect(status().isOk)
                .andReturn().response

            harness.mapper.readTree(response.contentAsString)[0]["is_healthy"].asBoolean() shouldBe false
            harness.gaugeValues() shouldBe mapOf("mysql-connection" to 0L)
        }

        it("reports every registered component independent of set order") {
            val harness = harness(
                checker("mysql-connection", isHealthy = true),
                checker("app-health", isHealthy = false),
            )

            val response = harness.mockMvc.perform(get("/health"))
                .andExpect(status().isOk)
                .andReturn().response
            val statuses = harness.mapper.readTree(response.contentAsString)
                .associate { it["service_name"].asText() to it["is_healthy"].asBoolean() }

            statuses shouldBe mapOf("mysql-connection" to true, "app-health" to false)
            harness.gaugeValues() shouldBe mapOf("mysql-connection" to 1L, "app-health" to 0L)
        }

        it("answers the internal liveness probe with is_healthy true") {
            val harness = harness()

            harness.mockMvc.perform(get("/health/internal"))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""{"is_healthy":true}"""))
        }
    }
})

private class HealthHarness(
    val mockMvc: MockMvc,
    val mapper: ObjectMapper,
    private val reader: InMemoryMetricReader,
) {

    fun gaugeValues(): Map<String, Long> = healthGaugeValues(reader)
}

private fun harness(vararg checkers: HealthChecker): HealthHarness {
    val reader = InMemoryMetricReader.create()
    val telemetry = HealthTelemetry(healthTelemetrySdk(reader).meterBuilder(TEST_SERVICE).build())
    val controller = HealthController(HealthHandler(checkers.toSet(), telemetry))
    // Boot's auto-configuration writes dates as ISO strings; standalone MockMvc defaults to
    // epoch arrays, so mirror the application's Jackson behavior here.
    val mapper: ObjectMapper = Jackson2ObjectMapperBuilder.json()
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()
    val mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setMessageConverters(MappingJackson2HttpMessageConverter(mapper))
        .build()
    return HealthHarness(mockMvc, mapper, reader)
}

private fun checker(serviceName: String, isHealthy: Boolean) = object : HealthChecker {
    override fun getHealthStatus() = healthCheckResult(serviceName, isHealthy)
}
