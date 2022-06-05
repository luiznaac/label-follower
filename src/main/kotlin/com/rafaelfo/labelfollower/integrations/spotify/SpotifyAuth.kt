package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.Base64

@Component
class SpotifyAuth(
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
    private val clock: Clock,
) {

    private var token: String? = null
    private var tokenExpiresAt: Instant? = null

    fun getToken(): String {
        if (shouldRequestNewToken()) {
            requestNewToken()
        }

        return token!!
    }

    private fun shouldRequestNewToken(): Boolean {
        if (token == null) {
            println("SpotifyAuth: No token set")
            return true
        }

        if (tokenExpiresAt != null && clock.instant().isAfter(tokenExpiresAt)) {
            println("SpotifyAuth: Token expired")
            return true
        }

        return false
    }

    private fun requestNewToken() {
        println("SpotifyAuth: Requesting new token")
        val response = rafaHttp.post(
            url = spotifyConfig.authUri,
            body = mapOf("grant_type" to "client_credentials"),
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to spotifyConfig.buildAuthorizationHeader(),
            ),
        )

        response.parsedBody<AuthResponse>().also {
            token = it.access_token
            tokenExpiresAt = clock.instant().plusSeconds(it.expires_in.toLong())
        }
    }
}

private data class AuthResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: String,
)

private fun SpotifyConfig.buildAuthorizationHeader(): String {
    val param = Base64.getEncoder().encodeToString(
        "${clientId}:${clientSecret}".toByteArray()
    )

    return "Basic $param"
}
