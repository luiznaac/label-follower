package com.rafaelfo.labelfollower.integrations.spotify

import com.google.gson.GsonBuilder
import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.Response
import okhttp3.ResponseBody
import java.util.Base64

class SpotifyAuthTest : StringSpec({

    val spotifyConfig = mockk<SpotifyConfig>()
    val rafaHttp = mockk<RafaHttp>()
    val auth = SpotifyAuth(spotifyConfig, rafaHttp)

    val gson = GsonBuilder().create()

    "should correctly get token" {
        val spotifyUri = "http://this.is.an.uri/auth/"
        val clientId = "client_id_123"
        val clientSecret = "secret!!!"
        coEvery { spotifyConfig.authUri } returns spotifyUri
        coEvery { spotifyConfig.clientId } returns clientId
        coEvery { spotifyConfig.clientSecret } returns clientSecret

        val expectedToken = "THIS_IS_MY_TOKEN"
        val response = mockk<Response>()
        val body = mockk<ResponseBody>()
        coEvery { response.body } returns body
        coEvery { body.string() } returns gson.toJson(
            AuthResponseForTest(
                access_token = expectedToken,
                token_type = "AA",
                expires_in = "3600",
            )
        )
        coEvery { rafaHttp.post(any(), any(), any()) } returns response

        auth.getToken() shouldBe expectedToken
        coVerify(exactly = 1) {
            rafaHttp.post(
                url = spotifyUri,
                body = mapOf("grant_type" to "client_credentials"),
                headers = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Authorization" to buildAuthHeader(clientId, clientSecret),
                ),
            )
        }
    }
})

private data class AuthResponseForTest(
    val access_token: String,
    val token_type: String,
    val expires_in: String,
)

private fun buildAuthHeader(clientId: String, clientSecret: String): String {
    val param = Base64.getEncoder().encodeToString(
        "${clientId}:${clientSecret}".toByteArray()
    )

    return "Basic $param"
}
