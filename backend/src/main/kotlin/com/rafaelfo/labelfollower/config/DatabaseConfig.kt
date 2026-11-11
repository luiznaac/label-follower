package com.rafaelfo.labelfollower.config

import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val DATABASE_NAME = "labelfollower"

// Shared by the Exposed connection below and by the Flyway migrator, which runs in its own
// process before the app boots (config/migration/Migrator.kt) and so cannot read the Spring
// context. One definition, so the two can't drift apart.
fun mysqlJdbcUrl(host: String, port: String) = "jdbc:mysql://$host:$port/$DATABASE_NAME"

@Configuration
class DatabaseConfig {

    @Bean
    fun databaseConnection(
        @Value("\${mysql.host}") host: String,
        // The DB's role in this project's port block (README's table): 8122 on the host,
        // while the container keeps MySQL's fixed internal 3306. Production compose pins
        // MYSQL_PORT=3306 so the container never inherits the host-side number.
        @Value("\${mysql.port:8122}") port: String,
        @Value("\${mysql.user}") user: String,
        @Value("\${mysql.password}") password: String,
    ) = Database.connect(
        url = mysqlJdbcUrl(host, port),
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password,
    )
}
