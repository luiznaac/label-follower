package com.rafaelfo.labelfollower.config

import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
        url = "jdbc:mysql://$host:$port/labelfollower",
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password,
    )
}
