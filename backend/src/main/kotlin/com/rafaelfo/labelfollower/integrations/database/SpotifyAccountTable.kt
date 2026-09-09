package com.rafaelfo.labelfollower.integrations.database

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.datetime

// The Spotify user we're permanently authenticated as (Authorization Code, confidential
// client — see SpotifyUserAuth). In practice a single row: this app authenticates as one
// person's Spotify account, not per-viewer.
object SpotifyAccountTable : IntIdTable("spotify_account") {
    val spotifyUserId = varchar("spotify_user_id", 64).uniqueIndex()
    val refreshToken = varchar("refresh_token", 512)
    val scopes = varchar("scopes", 255)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

class SpotifyAccountEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SpotifyAccountEntity>(SpotifyAccountTable)

    var spotifyUserId by SpotifyAccountTable.spotifyUserId
    var refreshToken by SpotifyAccountTable.refreshToken
    var scopes by SpotifyAccountTable.scopes
    var createdAt by SpotifyAccountTable.createdAt
    var updatedAt by SpotifyAccountTable.updatedAt
}
