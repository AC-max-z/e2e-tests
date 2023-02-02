import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
}
dependencies {
    implementation(project(":bootstrap"))
//    testImplementation(project(":kafka"))
    testImplementation(project(":test-data-generators"))
//    testImplementation(project(":domain-api"))
    testImplementation(project(":grpc"))
    
    testImplementation(project(":geofencing"))
    testImplementation(project(":broadcasting-platform"))
    testImplementation(project(":test-annotations"))
    testImplementation(project(":test-matchers"))
    testImplementation(project(":test-helpers"))
    

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
    testImplementation("com.fasterxml.jackson.module", "jackson-module-kotlin")
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