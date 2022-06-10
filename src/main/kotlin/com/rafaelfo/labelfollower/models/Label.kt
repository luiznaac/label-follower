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
                    thisCopyright.contains(otherCopyright) || otherCopyright.contains(thisCopyright)
                }
            }
            .reduce { acc, b -> acc || b }

        return name == label.name && copyrightMatch
    }
}
