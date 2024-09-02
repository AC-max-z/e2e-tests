import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
}
dependencies {
    implementation(project(":domain-api"))
    implementation(project(":geofencing"))
    
    
    implementation("org.locationtech.jts", "jts-core")
    implementation("org.locationtech.jts.io", "jts-io-common")
    implementation("io.github.serpro69", "kotlin-faker")
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