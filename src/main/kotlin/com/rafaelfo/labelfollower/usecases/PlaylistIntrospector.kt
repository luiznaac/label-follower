package com.rafaelfo.labelfollower.usecases

import org.springframework.stereotype.Service

@Service
class PlaylistIntrospector(
    private val externalInfoGateway: ExternalInfoGateway,
    private val ourInfoGateway: OurInfoGateway,
) {

    fun introspectPlaylist(spotifyPlaylistId: String) {
        ourInfoGateway.saveTracks(
            externalInfoGateway.getTracksFromPlaylist(spotifyPlaylistId),
            externalInfoGateway.getLabelFromPlaylist(spotifyPlaylistId),
        )
    }
}
