package com.rafaelfo.labelfollower.config.migration

import com.rafaelfo.labelfollower.config.mysqlJdbcUrl
import com.rafaelfo.labelfollower.integrations.database.allTables
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

const val MIGRATIONS_DIRECTORY = "src/main/resources/db/migration"

// Writes the SQL that would bring a local database up to what the Exposed tables declare — the
// authoring half of the migration workflow (`./gradlew generateMigrationScript -Pname=...`, see
// backend/CLAUDE.md §7). Exposed only generates the script; Flyway is what applies it. Always
// read the output before committing it: the diff is mechanical and won't, for instance, know
// that a rename is a rename rather than a drop plus an add.
@OptIn(ExperimentalDatabaseMigrationApi::class)
fun main() {
    val name = System.getProperty("migration.name").orEmpty()
    require(name.isNotBlank()) { "pass the migration name: ./gradlew generateMigrationScript -Pname=V2__add_x" }

    val mysql = MysqlEnv.fromEnvironment()
    Database.connect(
        url = mysqlJdbcUrl(mysql.host, mysql.port),
        driver = "com.mysql.cj.jdbc.Driver",
        user = mysql.user,
        password = mysql.password,
    )

    val script = transaction {
        MigrationUtils.generateMigrationScript(
            *allTables,
            scriptDirectory = MIGRATIONS_DIRECTORY,
            scriptName = name,
        )
    }
    println("wrote ${script.absolutePath}")
}
