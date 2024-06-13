package com.rafaelfo.labelfollower.integrations.spotify.models

import com.rafaelfo.labelfollower.integrations.spotify.responses.TrackItems
import com.rafaelfo.labelfollower.models.Label

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val copyrights: List<SpotifyCopyrights>?,
    val tracks: TrackItems?,
    val label: String?,
    val release_date: String,
) {

    fun toLabel() = Label(
        name = cleanedLabelName(),
        copyrights = copyrights!!
            .map {
                it.text
                    .replace(regex = REGEX_ANY_NUMBER_STARTING_WITH_20, replacement = "")
                    .replace(regex = Regex("\\s+"), replacement = " ")
                    .lowercase()
                    .trim()
            }
            .toSet(),
    )

    private fun cleanedLabelName(): String {
        var cleanedName = label!!
        WORDS_TO_IGNORE.forEach { cleanedName = cleanedName.replace(it, "", ignoreCase = true) }
        return cleanedName.replace(Regex("\\s+"), " ").lowercase().trim()
    }

    companion object {
        private val REGEX_ANY_NUMBER_STARTING_WITH_20 = Regex("20[0-9]{2}") // to match years - e.g. 2019
        private val WORDS_TO_IGNORE = setOf("records", "recordings")
    }
}
