package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class SpotifyAuth(
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) {

    fun getToken(): String {
        val response = rafaHttp.post(
            url = spotifyConfig.authUri,
            body = mapOf("grant_type" to "client_credentials"),
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to spotifyConfig.buildAuthorizationHeader(),
            ),
        )

        return response.parsedBody<AuthResponse>().access_token
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
