package com.rafaelfo.labelfollower.api

import com.rafaelfo.labelfollower.integrations.spotify.SpotifyGateway
import com.rafaelfo.labelfollower.models.Label
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/playlist")
class PlaylistController(
    private val spotifyGateway: SpotifyGateway,
) {

    @GetMapping("/{spotifyPlaylistId}")
    fun getPlaylist(@PathVariable spotifyPlaylistId: String): Label {
        val label = spotifyGateway.getLabelFromPlaylist(spotifyPlaylistId)
        return label
    }
}
