package com.rafaelfo.labelfollower.integrations.spotify.models

import com.rafaelfo.labelfollower.models.Artist

data class SpotifyArtist(
    val name: String,
) {

    fun toArtist() = Artist(
        name = name,
    )
}
