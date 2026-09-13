package com.rafaelfo.labelfollower.integrations.httputils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Properties

class RafaHttpTest : StringSpec({

    "should build URL from a bare origin and a path" {
        val requestUrl = buildRequestUrl(
            url = "https://api.spotify.com",
            path = "v1/me",
            queryParameters = emptyMap(),
        )

        requestUrl.toString() shouldBe "https://api.spotify.com/v1/me"
    }

    "should append query parameters" {
        val requestUrl = buildRequestUrl(
            url = "https://api.spotify.com",
            path = "v1/search",
            queryParameters = mapOf("q" to "abba", "type" to "album"),
        )

        requestUrl.toString() shouldBe "https://api.spotify.com/v1/search?q=abba&type=album"
    }

    "should reject a base URL that carries a path" {
        shouldThrow<IllegalArgumentException> {
            buildRequestUrl(
                url = "https://api.spotify.com/v1/",
                path = "v1/me",
                queryParameters = emptyMap(),
            )
        }
    }

    "production spotify.apiUri should be a bare origin" {
        val properties = Properties()
        val resource = checkNotNull(javaClass.getResourceAsStream("/application-production.properties"))
        resource.use { properties.load(it) }

        val apiUri = checkNotNull(properties.getProperty("spotify.apiUri"))
        val requestUrl = buildRequestUrl(apiUri, "v1/me", emptyMap())

        requestUrl.encodedPath shouldBe "/v1/me"
    }
})
