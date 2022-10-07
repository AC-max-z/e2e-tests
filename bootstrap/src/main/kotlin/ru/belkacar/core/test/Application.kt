package ru.belkacar.core.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["ru.belkacar.core", "ru.belkacar.telematics"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
