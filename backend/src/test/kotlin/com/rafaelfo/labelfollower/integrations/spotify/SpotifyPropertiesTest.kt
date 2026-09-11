package com.rafaelfo.labelfollower.integrations.spotify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Properties

// The Docker image runs with SPRING_PROFILES_ACTIVE=production, which layers
// application-production.properties over application.properties. Every Spotify gateway appends
// its own "v1/..." path to spotify.apiUri, so the base must be the bare host in *both* profiles —
// a "/v1/" in the production file once broke every Spotify call made by the image.
class SpotifyPropertiesTest : StringSpec({

    "production profile resolves the same Spotify API base as the default profile" {
        val default = loadProperties("application.properties")
        val production = default + loadProperties("application-production.properties")

        production["spotify.apiUri"] shouldBe default["spotify.apiUri"]
        production["spotify.authUri"] shouldBe default["spotify.authUri"]
        production["spotify.clientId"] shouldBe default["spotify.clientId"]
    }

    "gateway paths land on the v1 API under the resolved base" {
        val production = loadProperties("application.properties") +
            loadProperties("application-production.properties")

        val searchUrl = production.getValue("spotify.apiUri").toHttpUrl()
            .newBuilder()
            .addPathSegments("v1/search")
            .build()

        searchUrl.toString() shouldBe "https://api.spotify.com/v1/search"
    }
})

private fun loadProperties(resource: String): Map<String, String> {
    val stream = checkNotNull(SpotifyPropertiesTest::class.java.classLoader.getResourceAsStream(resource)) {
        "$resource not found on the classpath"
    }
    return stream.use { Properties().apply { load(it) } }
        .entries
        .associate { (key, value) -> key.toString() to value.toString() }
}
