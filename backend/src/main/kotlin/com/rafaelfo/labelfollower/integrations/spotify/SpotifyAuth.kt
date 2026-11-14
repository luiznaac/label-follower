package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.usecases.telemetry.EventLevel
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import com.rafaelfo.labelfollower.usecases.telemetry.event
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(SpotifyAuth::class.java)

    private var token: String? = null
    private var tokenExpiresAt: Instant? = null

    fun getToken(): String {
        if (shouldRequestNewToken()) {
            requestNewToken()
        }

        return token!!
    }

    private fun shouldRequestNewToken(): Boolean {
        val reason = when {
            token == null -> "missing"
            tokenExpiresAt != null && clock.instant().isAfter(tokenExpiresAt) -> "expired"
            else -> return false
        }
        log.event(
            TelemetryAttributes.AUTH_TOKEN_REFRESHED,
            "auth.reason" to reason,
            level = EventLevel.DEBUG,
        )
        return true
    }

    private fun requestNewToken() {
        val response = rafaHttp.post(
            url = spotifyConfig.authUri,
            formBody = mapOf("grant_type" to "client_credentials"),
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

fun SpotifyConfig.buildAuthorizationHeader(): String {
    val param = Base64.getEncoder().encodeToString(
        "$clientId:$clientSecret".toByteArray()
    )

    return "Basic $param"
}
