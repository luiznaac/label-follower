package com.rafaelfo.labelfollower.integrations.database

import com.rafaelfo.labelfollower.models.Track
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.datetime

object TrackTable : IntIdTable("track") {
    val spotifyId = varchar("spotify_id", 64).uniqueIndex()
    val isrc = varchar("isrc", 32).uniqueIndex()
    val name = varchar("name", 255)
    val createdAt = datetime("created_at")
}

class TrackEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TrackEntity>(TrackTable)

    var spotifyId by TrackTable.spotifyId
    var isrc by TrackTable.isrc
    var name by TrackTable.name
    var createdAt by TrackTable.createdAt

    fun toModel() = Track(name = name, isrc = isrc, spotifyId = spotifyId)
}
