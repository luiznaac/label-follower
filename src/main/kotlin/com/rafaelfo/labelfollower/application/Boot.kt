package com.rafaelfo.labelfollower.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.rafaelfo.labelfollower"])
class Boot

fun main(args: Array<String>) {
    runApplication<Boot>(*args)
}
