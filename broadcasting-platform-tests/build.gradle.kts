dependencies {
    testImplementation(project(":bootstrap"))
    testImplementation(project(":kafka"))
    testImplementation(project(":test-data-generators"))
    testImplementation(project(":domain-api"))

    testImplementation("org.springframework.boot", "spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("io.projectreactor", "reactor-test")
    testImplementation("org.locationtech.jts.io", "jts-io-common")
    testImplementation("ru.belkacar.core.test.tools", "reactor-allure-extentions")
    testImplementation("ru.belkacar.core.test.tools", "allure-annotations")
    testImplementation("org.jetbrains.kotlin", "kotlin-test")
    testImplementation("io.github.serpro69", "kotlin-faker")
    testImplementation("io.projectreactor.kotlin", "reactor-kotlin-extensions")
    testImplementation("org.springframework.kafka", "spring-kafka")
    testImplementation("io.projectreactor.kafka", "reactor-kafka")
    testImplementation("org.n52.jackson", "jackson-datatype-jts")
}
