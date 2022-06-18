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
            copyrights.flatMap { thisCopyright ->
                label.copyrights.map { otherCopyright ->
                    thisCopyright matches otherCopyright
                }
            }
            .reduce { acc, b -> acc || b }

        return name == label.name && copyrightMatch
    }

}

private infix fun String.matches(other: String) =
    contains(other, ignoreCase = true) || other.contains(this, ignoreCase = true)
