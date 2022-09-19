dependencies {
    compileOnly("org.slf4j", "slf4j-api")
    compileOnly("org.springframework", "spring-context")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.kafka", "spring-kafka")
    compileOnly("org.springframework.boot", "spring-boot-starter")
    implementation("io.projectreactor.kafka", "reactor-kafka")

    implementation("org.n52.jackson", "jackson-datatype-jts")
}

description = "kafka"
