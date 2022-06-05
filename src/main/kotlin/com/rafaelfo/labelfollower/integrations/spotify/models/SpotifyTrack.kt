package com.rafaelfo.labelfollower.integrations.spotify.models

import com.rafaelfo.labelfollower.models.Track

data class SpotifyTrack(
    val name: String,
    val artists: Set<SpotifyArtist>,
    val isrc: String,
) {

    fun toTrack() = Track(
        name = name,
        artists = artists.map { it.toArtist() }.toSet(),
        isrc = isrc,
    )
}
