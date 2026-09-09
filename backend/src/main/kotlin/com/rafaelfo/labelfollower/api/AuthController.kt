package com.rafaelfo.labelfollower.api

import com.rafaelfo.labelfollower.integrations.spotify.SpotifyUserAuth
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// Backend-side Spotify login: the frontend still drives the browser redirect to Spotify's
// consent screen (same registered redirect URI as before, see frontend/src/lib/spotifyAuth.ts),
// but the authorization code lands here instead of being exchanged client-side with PKCE. That
// lets the backend hold the resulting refresh token and stay authenticated across restarts,
// without needing a browser in the loop for background work.
@RestController
@RequestMapping("/auth/spotify")
class AuthController(
    private val spotifyUserAuth: SpotifyUserAuth,
) {

    @GetMapping("/status")
    fun status() = SpotifyStatusResponse(connected = spotifyUserAuth.isConnected())

    @PostMapping("/exchange")
    fun exchange(@RequestBody request: ExchangeCodeRequest) {
        spotifyUserAuth.connect(request.code, request.redirectUri)
    }

    @DeleteMapping
    fun disconnect() {
        spotifyUserAuth.disconnect()
    }
}

data class ExchangeCodeRequest(val code: String, val redirectUri: String)
data class SpotifyStatusResponse(val connected: Boolean)
