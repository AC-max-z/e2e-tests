import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
}
dependencies {
    compileOnly("org.slf4j", "slf4j-api")
    compileOnly("org.springframework", "spring-context")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")

    api("org.springframework.kafka", "spring-kafka")
    compileOnly("org.springframework.boot", "spring-boot-starter")
    api("io.projectreactor.kafka", "reactor-kafka")

    implementation("org.n52.jackson", "jackson-datatype-jts")
    implementation(kotlin("stdlib-jdk8"))
}

description = "kafka"
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}