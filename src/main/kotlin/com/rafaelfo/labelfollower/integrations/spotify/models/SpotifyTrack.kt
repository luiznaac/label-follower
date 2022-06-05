package com.rafaelfo.labelfollower.integrations.spotify.models

import com.rafaelfo.labelfollower.models.Track

data class SpotifyTrack(
    val name: String,
    val artists: Set<SpotifyArtist>,
    val album: SpotifyAlbum,
    val external_ids: SpotifyExternalIds,
) {

    fun toTrack() = Track(
        name = name,
        artists = artists.map { it.toArtist() }.toSet(),
        isrc = external_ids.isrc,
    )
}

data class SpotifyExternalIds(
    val isrc: String,
)
