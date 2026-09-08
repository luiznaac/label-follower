package com.rafaelfo.labelfollower.integrations.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

// Which tracks are already known to belong to which label — what
// LabelIntrospector diffs a label's live Spotify catalogue against. Pure
// association table (no domain identity of its own), so no IntEntity here.
object LabelTrackTable : Table("label_track") {
    val label = reference("label_id", LabelTable)
    val track = reference("track_id", TrackTable)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(label, track)
}
