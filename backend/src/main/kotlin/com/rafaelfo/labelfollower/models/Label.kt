package com.rafaelfo.labelfollower.models

data class Label(
    val name: String,
    val copyrights: Set<String>,
) {
    override fun toString(): String {
        return "$name - ${copyrights.joinToString(" / ")}"
    }

    fun matches(label: Label): Boolean {
        val copyrightMatch =
            copyrights.isEmpty() ||
                label.copyrights.isEmpty() ||
                copyrights.any { thisCopyright ->
                    label.copyrights.any { otherCopyright -> thisCopyright matches otherCopyright }
                }

        return name == label.name && copyrightMatch
    }
}

private infix fun String.matches(other: String) =
    contains(other, ignoreCase = true) || other.contains(this, ignoreCase = true)
