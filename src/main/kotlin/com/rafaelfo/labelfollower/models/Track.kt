package com.rafaelfo.labelfollower.models

data class Track(
    val name: String,
    val artists: Set<Artist>,
    val isrc: String,
)
