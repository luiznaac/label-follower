package com.rafaelfo.labelfollower.config.migration

import com.rafaelfo.labelfollower.integrations.database.allTables
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.testcontainers.containers.MySQLContainer

// The guard that keeps db/migration/ and the Exposed tables from drifting apart — the failure
// mode that made `mysql/init.sql` unmaintainable. Migrates a throwaway MySQL from scratch, then
// asks Exposed what would still have to change for the database to match `allTables`. Anything
// non-empty means someone edited a *Table without writing the migration, or vice versa.
//
// Needs a Docker daemon. This is the only test in the repo that does.
@OptIn(ExperimentalDatabaseMigrationApi::class)
class MigrationSchemaTest : StringSpec({

    "migrations produce exactly the schema the Exposed tables declare" {
        val mysql = MySQLContainer("mysql:9.4.0")
            .withDatabaseName("labelfollower")
            // testcontainers-java bundles its own my.cnf for MySQLContainer that still sets
            // innodb_log_file_size, removed in MySQL 8.0.30+ — without this override the
            // container fails to initialize against mysql:9.4.0. See mysql-test-conf/my.cnf.
            .withConfigurationOverride("mysql-test-conf")

        mysql.use {
            it.start()

            migrate(
                host = it.host,
                port = it.getMappedPort(MySQLContainer.MYSQL_PORT).toString(),
                user = it.username,
                password = it.password,
            )

            val db = Database.connect(
                url = it.jdbcUrl,
                driver = "com.mysql.cj.jdbc.Driver",
                user = it.username,
                password = it.password,
            )
            transaction(db) {
                MigrationUtils.statementsRequiredForDatabaseMigration(*allTables).shouldBeEmpty()
            }
        }
    }
})
