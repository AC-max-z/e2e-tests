import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
}
dependencies {
    compileOnly("org.springframework.boot", "spring-boot")
    implementation("org.n52.jackson", "jackson-datatype-jts")
    
    api("org.locationtech.jts", "jts-core")
    
    compileOnly("org.springframework.boot", "spring-boot-configuration-processor")
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