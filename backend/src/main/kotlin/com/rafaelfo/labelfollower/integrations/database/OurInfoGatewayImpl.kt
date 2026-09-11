package com.rafaelfo.labelfollower.integrations.database

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.OurInfoGateway
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class OurInfoGatewayImpl(
    private val clock: Clock,
) : OurInfoGateway {

    // A pure read: a label we never recorded simply has no tracks yet. (It used to upsert the label
    // here, which made "find new tracks" record things as a side effect — see LabelIntrospector.)
    override fun getTracksFrom(label: Label): Set<Track> = transaction {
        val labelEntity = LabelEntity.find { LabelTable.canonicalName eq label.name }.firstOrNull()
            ?: return@transaction emptySet()

        val trackIds = LabelTrackTable
            .selectAll()
            .where { LabelTrackTable.label eq labelEntity.id }
            .map { it[LabelTrackTable.track] }

        TrackEntity.find { TrackTable.id inList trackIds }.map { it.toModel() }.toSet()
    }

    override fun saveTracks(tracks: Set<Track>, label: Label) {
        transaction {
            val labelEntity = upsertLabel(label)

            tracks.forEach { track -> linkTrack(labelEntity.id, upsertTrack(track).id) }
        }
    }

    override fun getLabelBy(labelName: String): Label? = transaction {
        LabelEntity.find { LabelTable.canonicalName eq labelName }.firstOrNull()?.toModel()
    }

    override fun getLabels(): Set<Label> = transaction {
        LabelEntity.all().map { it.toModel() }.toSet()
    }

    private fun upsertLabel(label: Label): LabelEntity {
        val entity = LabelEntity.find { LabelTable.canonicalName eq label.name }.firstOrNull()
            ?: LabelEntity.new {
                canonicalName = label.name
                createdAt = LocalDateTime.now(clock)
            }

        val existingCopyrights = LabelCopyrightEntity.find { LabelCopyrightTable.label eq entity.id }
            .map { it.copyrightText }
            .toSet()

        (label.copyrights - existingCopyrights).forEach { newCopyright ->
            LabelCopyrightEntity.new {
                this.label = entity
                copyrightText = newCopyright
                createdAt = LocalDateTime.now(clock)
            }
        }

        return entity
    }

    private fun upsertTrack(track: Track): TrackEntity {
        return TrackEntity.find { TrackTable.isrc eq track.isrc }.firstOrNull()
            ?: TrackEntity.new {
                spotifyId = track.spotifyId
                isrc = track.isrc
                name = track.name
                createdAt = LocalDateTime.now(clock)
            }
    }

    private fun linkTrack(labelId: EntityID<Int>, trackId: EntityID<Int>) {
        val alreadyLinked = LabelTrackTable
            .selectAll()
            .where { (LabelTrackTable.label eq labelId) and (LabelTrackTable.track eq trackId) }
            .any()

        if (!alreadyLinked) {
            LabelTrackTable.insert {
                it[label] = labelId
                it[track] = trackId
                it[createdAt] = LocalDateTime.now(clock)
            }
        }
    }

    private fun LabelEntity.toModel(): Label {
        val copyrights = LabelCopyrightEntity.find { LabelCopyrightTable.label eq id }
            .map { it.copyrightText }
            .toSet()
        return Label(name = canonicalName, copyrights = copyrights)
    }
}
