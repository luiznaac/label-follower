package com.rafaelfo.labelfollower.integrations.spotify.models

import com.rafaelfo.labelfollower.integrations.spotify.responses.TrackItems
import com.rafaelfo.labelfollower.models.Label

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val copyrights: List<SpotifyCopyrights>?,
    val tracks: TrackItems?,
    val label: String?,
) {

    fun toLabel() = Label(
        name = label!!,
        copyright = copyrights!!.first().text.split(" ", limit = 2)[1],
    )
}
