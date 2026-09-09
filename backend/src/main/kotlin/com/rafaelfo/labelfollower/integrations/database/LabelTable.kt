package com.rafaelfo.labelfollower.integrations.database

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.datetime

object LabelTable : IntIdTable("label") {
    val canonicalName = varchar("canonical_name", 255).uniqueIndex()
    val createdAt = datetime("created_at")
}

class LabelEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LabelEntity>(LabelTable)

    var canonicalName by LabelTable.canonicalName
    var createdAt by LabelTable.createdAt
}

object LabelCopyrightTable : IntIdTable("label_copyright") {
    val label = reference("label_id", LabelTable)
    val copyrightText = varchar("copyright_text", 255)
    val createdAt = datetime("created_at")
}

class LabelCopyrightEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LabelCopyrightEntity>(LabelCopyrightTable)

    var label by LabelEntity referencedOn LabelCopyrightTable.label
    var copyrightText by LabelCopyrightTable.copyrightText
    var createdAt by LabelCopyrightTable.createdAt
}
