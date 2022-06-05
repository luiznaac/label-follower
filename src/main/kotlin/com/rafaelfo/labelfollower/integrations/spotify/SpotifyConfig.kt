package com.rafaelfo.labelfollower.integrations.spotify

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.get

@Configuration
class SpotifyConfiguration(
    private val environment: Environment,
) {

    @Bean
    fun spotifyConfig() = SpotifyConfig(
        clientId = environment["spotify.clientId"]!!,
        clientSecret = environment["spotify.clientSecret"]!!,
        authUri = environment["spotify.authUri"]!!,
        apiUri = environment["spotify.apiUri"]!!,
    )
}

data class SpotifyConfig(
    val clientId: String,
    val clientSecret: String,
    val authUri: String,
    val apiUri: String,
)
