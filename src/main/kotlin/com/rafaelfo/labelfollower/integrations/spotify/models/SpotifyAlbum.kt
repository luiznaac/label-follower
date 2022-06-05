package com.rafaelfo.labelfollower.integrations.spotify.models

import com.rafaelfo.labelfollower.models.Label

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val copyrights: List<SpotifyCopyrights>?,
    val label: String?,
) {

    fun toLabel() = Label(
        name = label!!,
        copyright = copyrights!!.first().text.split(" ", limit = 2)[1],
    )
}
