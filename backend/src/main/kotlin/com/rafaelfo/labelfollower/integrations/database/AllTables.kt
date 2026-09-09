package com.rafaelfo.labelfollower.integrations.database

import org.jetbrains.exposed.v1.core.Table

// Every table this service maps, in dependency order. Single source for the two places that
// need the whole set: MigrationSchemaTest (which diffs it against a migrated database) and the
// `generateMigrationScript` Gradle task. A new *Table object must be added here.
val allTables: Array<Table> = arrayOf(
    LabelTable,
    LabelCopyrightTable,
    TrackTable,
    LabelTrackTable,
    SpotifyAccountTable,
)
