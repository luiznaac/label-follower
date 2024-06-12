package com.rafaelfo.labelfollower.integrations.spotify

import com.google.gson.GsonBuilder
import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.Response
import okhttp3.ResponseBody
import java.time.Clock
import java.time.Instant.now
import java.util.Base64

class SpotifyAuthTest : StringSpec({

    val spotifyConfig = spotifyConfig()
    val rafaHttp = mockk<RafaHttp>()
    val clock = mockk<Clock>().also {
        coEvery { it.instant() } returns now()
    }
    var auth = SpotifyAuth(spotifyConfig, rafaHttp, clock)

    val gson = GsonBuilder().create()

    beforeEach {
        clearMocks(rafaHttp)
        auth = SpotifyAuth(spotifyConfig, rafaHttp, clock)
    }

    "should correctly get token" {
        val expectedToken = "THIS_IS_MY_TOKEN"
        val response = mockk<Response>()
        val body = mockk<ResponseBody>()
        coEvery { response.body } returns body
        coEvery { body.string() } returns gson.toJson(authResponse(expectedToken))
        coEvery { rafaHttp.post(any(), any(), any(), any()) } returns response

        auth.getToken() shouldBe expectedToken
        coVerify(exactly = 1) {
            rafaHttp.post(
                url = spotifyConfig.authUri,
                body = mapOf("grant_type" to "client_credentials"),
                headers = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Authorization" to buildAuthHeader(spotifyConfig.clientId, spotifyConfig.clientSecret),
                ),
            )
        }
    }

    "should not get token if token is already set" {
        val expectedToken = "THIS_IS_MY_TOKEN_2"
        val response = mockk<Response>()
        val body = mockk<ResponseBody>()
        coEvery { response.body } returns body
        coEvery { body.string() } returns gson.toJson(authResponse(expectedToken))
        coEvery { rafaHttp.post(any(), any(), any(), any()) } returns response

        auth.getToken() shouldBe expectedToken
        auth.getToken() shouldBe expectedToken

        coVerify(exactly = 1) {
            rafaHttp.post(
                url = spotifyConfig.authUri,
                body = mapOf("grant_type" to "client_credentials"),
                headers = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Authorization" to buildAuthHeader(spotifyConfig.clientId, spotifyConfig.clientSecret),
                ),
            )
        }
    }

    "should get new token if token is expired" {
        val expectedToken1 = "THIS_IS_MY_TOKEN_1"
        val expectedToken2 = "THIS_IS_MY_TOKEN_2"
        val response = mockk<Response>()
        val body = mockk<ResponseBody>()
        coEvery { response.body } returns body
        coEvery { body.string() } returnsMany listOf(
            gson.toJson(authResponse(expectedToken1)),
            gson.toJson(authResponse(expectedToken2)),
        )
        coEvery { rafaHttp.post(any(), any(), any(), any()) } returns response

        auth.getToken() shouldBe expectedToken1

        coEvery { clock.instant() } returns now().plusSeconds(3601)
        auth.getToken() shouldBe expectedToken2

        coVerify(exactly = 2) {
            rafaHttp.post(
                url = spotifyConfig.authUri,
                body = mapOf("grant_type" to "client_credentials"),
                headers = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Authorization" to buildAuthHeader(spotifyConfig.clientId, spotifyConfig.clientSecret),
                ),
            )
        }
    }
})

private fun authResponse(expectedToken: String) = AuthResponseForTest(
    access_token = expectedToken,
    token_type = "AA",
    expires_in = "3600",
)

private data class AuthResponseForTest(
    val access_token: String,
    val token_type: String,
    val expires_in: String,
)

private fun buildAuthHeader(clientId: String, clientSecret: String): String {
    val param = Base64.getEncoder().encodeToString(
        "$clientId:$clientSecret".toByteArray()
    )

    return "Basic $param"
}

private fun spotifyConfig() = SpotifyConfig(
    clientId = "CLIENT_ID",
    clientSecret = "SECRET!!!",
    authUri = "http://this.is.an.uri/auth/",
    apiUri = "http://this.is.an.uri/api/",
)
