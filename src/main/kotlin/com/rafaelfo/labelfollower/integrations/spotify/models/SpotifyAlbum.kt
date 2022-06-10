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
        copyrights = copyrights!!
            .map { it.text.replace(regex = REGEX_FIRST_4_NUMBERS_FOLLOWED_BY_SPACE, replacement = "") }
            .toSet(),
    )

    companion object {
        private val REGEX_FIRST_4_NUMBERS_FOLLOWED_BY_SPACE = Regex("(^)\\d{4}\\s+")
    }
}
