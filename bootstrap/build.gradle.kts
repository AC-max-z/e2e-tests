import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    kotlin("jvm") version "1.8.10"
}

dependencies {
    implementation(project(":domain-api"))
    
    implementation(project(":kafka"))
    implementation(project(":grpc"))
    
    implementation(project(":geofencing"))
    implementation(project(":broadcasting-platform"))

    api("org.springframework.boot", "spring-boot-starter")
    kapt("org.springframework.boot", "spring-boot-configuration-processor")
    
//    testImplementation(project(":geofence-services-tests"))
//
//    testImplementation("org.springframework.boot", "spring-boot-starter-test") {
//        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
//        exclude(module = "mockito-core")
//    }
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