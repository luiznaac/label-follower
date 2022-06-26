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
        name = cleanedLabelName(),
        copyrights = copyrights!!
            .map { it.text.replace(regex = REGEX_FIRST_4_NUMBERS_FOLLOWED_BY_SPACE, replacement = "") }
            .toSet(),
    )

    private fun cleanedLabelName(): String {
        var cleanedName = label!!
        WORDS_TO_IGNORE.forEach { cleanedName = cleanedName.replace(it, "", ignoreCase = true) }
        return cleanedName.replace(Regex("\\s+"), " ").trim()
    }

    companion object {
        private val REGEX_FIRST_4_NUMBERS_FOLLOWED_BY_SPACE = Regex("(^)\\d{4}\\s+")
        private val WORDS_TO_IGNORE = setOf("records", "recordings")
    }
}
