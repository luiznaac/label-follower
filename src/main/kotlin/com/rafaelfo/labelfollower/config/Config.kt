package com.rafaelfo.labelfollower.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Config {

    @Bean
    fun clock() = Clock.systemDefaultZone()
}
