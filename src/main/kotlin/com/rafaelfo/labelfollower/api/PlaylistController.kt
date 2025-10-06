package com.rafaelfo.labelfollower.api

import com.rafaelfo.labelfollower.usecases.PlaylistIntrospector
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/playlist")
class PlaylistController(
    private val playlistIntrospector: PlaylistIntrospector,
) {

    @GetMapping("/{spotifyPlaylistId}")
    fun getPlaylist(@PathVariable spotifyPlaylistId: String) {
        playlistIntrospector.introspectPlaylist(spotifyPlaylistId)
    }
}
