package com.rafaelfo.labelfollower.config.migration

import com.rafaelfo.labelfollower.config.mysqlJdbcUrl
import org.flywaydb.core.Flyway

const val MIGRATIONS_LOCATION = "classpath:db/migration"

// Applies every pending db/migration/V*.sql. Called from deploy/entrypoint.sh before the app
// starts, and from MigrationSchemaTest — never from the Spring context, so a failed migration
// stops the container instead of leaving a half-migrated app serving traffic.
fun migrate(host: String, port: String, user: String, password: String) {
    Flyway.configure()
        .dataSource(mysqlJdbcUrl(host, port), user, password)
        .locations(MIGRATIONS_LOCATION)
        // A database that predates migrations (or a local volume still holding the old
        // mysql/init.sql schema) is stamped at V1 rather than having it re-applied; an
        // empty schema just runs V1 normally.
        .baselineOnMigrate(true)
        .baselineVersion("1")
        .load()
        .migrate()
}

fun main() {
    val mysql = MysqlEnv.fromEnvironment()
    migrate(mysql.host, mysql.port, mysql.user, mysql.password)
}
