package com.rafaelfo.labelfollower.integrations.httputils

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.slf4j.LoggerFactory

class RafaHttpTest : StringSpec({

    val http = RafaHttp()
    lateinit var server: MockWebServer

    beforeEach {
        server = MockWebServer().apply { start() }
    }

    afterEach {
        server.shutdown()
    }

    "get resolves the path under a base URL that has a port and a path of its own" {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        val result = http.get(
            url = server.url("/api/").toString(),
            path = "v1/search",
            headers = emptyMap(),
            queryParameters = mapOf("q" to "isrc:GBEWA2205680", "type" to "track"),
        )

        result.status shouldBe 200
        result.body shouldBe """{"ok":true}"""
        server.takeRequest().path shouldBe "/api/v1/search?q=isrc%3AGBEWA2205680&type=track"
    }

    "get works with a bare-host base URL, the shape spotify.apiUri has" {
        server.enqueue(MockResponse().setBody("{}"))

        http.get(url = server.url("").toString().trimEnd('/'), path = "v1/albums/abc", headers = emptyMap())

        server.takeRequest().path shouldBe "/v1/albums/abc"
    }

    "post sends a JSON body with an application/json content type" {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"p1"}"""))

        val result = http.post(
            url = server.url("/").toString(),
            path = "v1/users/me/playlists",
            body = mapOf("name" to "novidades"),
            headers = mapOf("Authorization" to "Bearer user-token"),
        )

        result.parsedBody<Map<String, String>>() shouldBe mapOf("id" to "p1")
        val request = server.takeRequest()
        request.path shouldBe "/v1/users/me/playlists"
        request.getHeader("Content-Type")!! shouldStartWith "application/json"
        request.getHeader("Authorization") shouldBe "Bearer user-token"
        request.body.readUtf8() shouldBe """{"name":"novidades"}"""
    }

    "post sends a form body when there is no JSON body" {
        server.enqueue(MockResponse().setBody("{}"))

        http.post(
            url = server.url("/api/token/").toString(),
            formBody = mapOf("grant_type" to "client_credentials"),
            headers = emptyMap(),
        )

        val request = server.takeRequest()
        request.path shouldBe "/api/token/"
        request.getHeader("Content-Type")!! shouldStartWith "application/x-www-form-urlencoded"
        request.body.readUtf8() shouldBe "grant_type=client_credentials"
    }

    "a non-2xx response throws ExternalServiceException carrying status and body" {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"not found"}"""))

        val error = shouldThrow<ExternalServiceException> {
            http.get(url = server.url("/").toString(), path = "v1/tracks/x", headers = emptyMap())
        }

        error.status shouldBe 404
        error.responseBody shouldBe """{"error":"not found"}"""
        error.method shouldBe "GET"
        error.message!! shouldContain "HTTP 404"
    }

    "429 is retried honouring Retry-After and then succeeds" {
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setBody("done"))

        val result = http.get(url = server.url("/").toString(), path = "v1/search", headers = emptyMap())

        result.body shouldBe "done"
        server.requestCount shouldBe 3
    }

    "429 gives up after three retries" {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "0")) }

        val error = shouldThrow<ExternalServiceException> {
            http.get(url = server.url("/").toString(), path = "v1/search", headers = emptyMap())
        }

        error.status shouldBe 429
        server.requestCount shouldBe 4
    }

    "a GET is retried on a transient 502 — Spotify returns those under load" {
        server.enqueue(
            MockResponse().setResponseCode(502).addHeader("Retry-After", "0")
                .setBody("""{"error": {"status": 502, "message": "An unexpected error occurred."}}"""),
        )
        server.enqueue(MockResponse().setBody("recovered"))

        val result = http.get(url = server.url("/").toString(), path = "v1/search", headers = emptyMap())

        result.body shouldBe "recovered"
        server.requestCount shouldBe 2
    }

    "a POST is never replayed on a 502, since the first attempt may have created something" {
        server.enqueue(MockResponse().setResponseCode(502).addHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setBody("""{"id":"duplicate"}"""))

        val error = shouldThrow<ExternalServiceException> {
            http.post(
                url = server.url("/").toString(),
                path = "v1/users/me/playlists",
                body = mapOf("name" to "x"),
                headers = emptyMap(),
            )
        }

        error.status shouldBe 502
        server.requestCount shouldBe 1
    }

    "logs method, URL and status, never header values" {
        val logger = LoggerFactory.getLogger(RafaHttp::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        server.enqueue(MockResponse().setBody("{}"))

        try {
            http.post(
                url = server.url("/api/token/").toString(),
                formBody = mapOf("grant_type" to "client_credentials"),
                headers = mapOf("Authorization" to "Basic c2VjcmV0LWNsaWVudDpzZWNyZXQ="),
            )
        } finally {
            logger.detachAppender(appender)
        }

        val lines = appender.list.map { it.formattedMessage }
        lines.shouldNotBeEmpty()
        lines.forEach {
            it shouldContain "POST"
            it shouldContain "-> 200"
            it shouldNotContain "Basic"
            it shouldNotContain "c2VjcmV0LWNsaWVudDpzZWNyZXQ="
        }
    }
})
