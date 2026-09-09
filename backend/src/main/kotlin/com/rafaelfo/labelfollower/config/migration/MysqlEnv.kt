package com.rafaelfo.labelfollower.config.migration

// Connection details for the migration tooling, which runs outside the Spring context and so
// can't read application.properties. The defaults deliberately mirror the ones in that file:
// `docker compose -f backend/docker-compose.yml up -d mysql` with no extra setup.
internal data class MysqlEnv(
    val host: String,
    val port: String,
    val user: String,
    val password: String,
) {
    companion object {
        fun fromEnvironment() = MysqlEnv(
            host = System.getenv("MYSQL_HOST") ?: "localhost",
            port = System.getenv("MYSQL_PORT") ?: "3306",
            user = System.getenv("MYSQL_USER") ?: "root",
            password = System.getenv("MYSQL_PASSWORD") ?: "",
        )
    }
}
