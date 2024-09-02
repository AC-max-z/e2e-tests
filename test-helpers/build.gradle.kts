import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
}
dependencies {
    implementation(project(":bootstrap"))
    implementation(project(":test-data-generators"))

    implementation(project(":geofencing"))
    implementation(project(":broadcasting-platform"))
    implementation(project(":test-matchers"))

    implementation("io.projectreactor", "reactor-test")
    implementation("ru.belkacar.core.test.tools", "reactor-allure-extentions")
    implementation("org.jetbrains.kotlin", "kotlin-test")
    implementation("io.github.serpro69", "kotlin-faker")
    implementation("io.projectreactor.kotlin", "reactor-kotlin-extensions")
    implementation("org.springframework.kafka", "spring-kafka")
    implementation(kotlin("stdlib-jdk8"))
}
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