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
        // Not exposed as a top-level `mysql.*` config key: every real deployment
        // (docker-compose, the sibling projects) talks to MySQL's default port.
        // Only exists so local ad-hoc testing can point at a container published
        // on a different host port without editing this file.
        @Value("\${mysql.port:3306}") port: String,
        @Value("\${mysql.user}") user: String,
        @Value("\${mysql.password}") password: String,
    ) = Database.connect(
        url = mysqlJdbcUrl(host, port),
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password,
    )
}
