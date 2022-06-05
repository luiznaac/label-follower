package com.rafaelfo.labelfollower.integrations.spotify.responses

import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyAlbum
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyTrack

data class SearchResponse(
    val tracks: TrackItems,
    val albums: AlbumItems,
)

data class TrackItems(
    val items: List<SpotifyTrack>,
    val limit: Int?,
    val offset: Int?,
    val total: Int?,
)

data class AlbumItems(
    val items: List<SpotifyAlbum>,
    val limit: Int,
    val offset: Int,
    val total: Int,
)
