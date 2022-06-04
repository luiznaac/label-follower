package com.rafaelfo.labelfollower.models

data class Album(
    val name: String,
    val label: Label,
    val tracks: Set<Track>,
)
