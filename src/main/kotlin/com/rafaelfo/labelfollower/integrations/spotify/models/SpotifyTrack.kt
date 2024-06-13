package com.rafaelfo.labelfollower.integrations.spotify.models

import com.rafaelfo.labelfollower.models.Track

data class SpotifyTrack(
    val id: String,
    val name: String,
//    val artists: Set<SpotifyArtist>,
    val album: SpotifyAlbum?,
    val external_ids: SpotifyExternalIds?,
) {

    fun toTrack() = Track(
        name = name,
        isrc = external_ids!!.isrc,
        spotifyId = id,
    )
}

data class SpotifyExternalIds(
    val isrc: String,
)
