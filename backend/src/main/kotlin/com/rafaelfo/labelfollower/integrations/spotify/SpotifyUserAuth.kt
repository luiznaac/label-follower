package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.database.SpotifyAccountEntity
import com.rafaelfo.labelfollower.integrations.database.SpotifyAccountTable
import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

// The backend's own, permanent Spotify login — separate from SpotifyAuth (client-credentials,
// used for the app's own catalogue reads). This one holds a user access/refresh token pair so
// features like /consolidate can run without a browser in the loop: connect() is called once
// (from the OAuth callback), the refresh token is persisted, and getFreshAccessToken() mints new
// access tokens from it forever after — mirrors SpotifyAuth's in-memory token cache shape.
@Component
class SpotifyUserAuth(
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
    private val clock: Clock,
) {

    private var accessToken: String? = null
    private var accessTokenExpiresAt: Instant? = null

    fun isConnected(): Boolean = transaction { !SpotifyAccountEntity.all().empty() }

    fun getSpotifyUserId(): String = transaction {
        SpotifyAccountEntity.all().firstOrNull()?.spotifyUserId ?: noAccountConnected()
    }

    fun connect(code: String, redirectUri: String) {
        val response = requestToken(
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to redirectUri,
            )
        )
        val spotifyUserId = fetchSpotifyUserId(response.access_token)
        val refreshToken = response.refresh_token
            ?: error("Spotify did not return a refresh token for the authorization_code grant")

        saveAccount(spotifyUserId, refreshToken, response.scope.orEmpty())
        cacheAccessToken(response)
    }

    fun disconnect() {
        transaction { SpotifyAccountEntity.all().forEach { it.delete() } }
        accessToken = null
        accessTokenExpiresAt = null
    }

    fun getFreshAccessToken(): String {
        if (shouldRequestNewToken()) {
            requestNewAccessToken()
        }
        return accessToken!!
    }

    private fun shouldRequestNewToken(): Boolean {
        if (accessToken == null) return true
        return accessTokenExpiresAt?.let { clock.instant().isAfter(it) } ?: false
    }

    private fun requestNewAccessToken() {
        val storedRefreshToken = transaction {
            SpotifyAccountEntity.all().firstOrNull()?.refreshToken
        } ?: noAccountConnected()

        val response = requestToken(
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to storedRefreshToken,
            )
        )
        cacheAccessToken(response)

        // Confidential clients aren't guaranteed to get a new refresh token on every refresh —
        // persist it only when Spotify actually sends one, keep the stored one otherwise.
        response.refresh_token?.let { rotated ->
            transaction {
                SpotifyAccountEntity.all().firstOrNull()?.refreshToken = rotated
            }
        }
    }

    private fun requestToken(formBody: Map<String, String>): SpotifyTokenResponse {
        return rafaHttp.post(
            url = spotifyConfig.authUri,
            formBody = formBody,
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to spotifyConfig.buildAuthorizationHeader(),
            ),
        ).parsedBody()
    }

    private fun fetchSpotifyUserId(userAccessToken: String): String {
        return rafaHttp.get(
            url = spotifyConfig.apiUri,
            path = "v1/me",
            headers = mapOf("Authorization" to "Bearer $userAccessToken"),
        ).parsedBody<SpotifyMeResponse>().id
    }

    private fun saveAccount(spotifyUserId: String, refreshToken: String, scopes: String) {
        transaction {
            val now = LocalDateTime.now(clock)
            val existing = SpotifyAccountEntity
                .find { SpotifyAccountTable.spotifyUserId eq spotifyUserId }
                .firstOrNull()

            if (existing != null) {
                existing.refreshToken = refreshToken
                existing.scopes = scopes
                existing.updatedAt = now
            } else {
                SpotifyAccountEntity.new {
                    this.spotifyUserId = spotifyUserId
                    this.refreshToken = refreshToken
                    this.scopes = scopes
                    createdAt = now
                    updatedAt = now
                }
            }
        }
    }

    private fun cacheAccessToken(response: SpotifyTokenResponse) {
        accessToken = response.access_token
        accessTokenExpiresAt = clock.instant().plusSeconds(response.expires_in.toLong())
    }

    private fun noAccountConnected(): Nothing =
        error("No Spotify account connected — connect one from the Consolidate screen first")
}

private data class SpotifyTokenResponse(
    val access_token: String,
    val token_type: String,
    val scope: String?,
    val expires_in: String,
    val refresh_token: String?,
)

private data class SpotifyMeResponse(val id: String)
