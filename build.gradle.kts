import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "ru.belkacar.core.test"
version = "1.0-SNAPSHOT"

plugins {
    id("org.springframework.boot") version "2.6.7" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21" apply false
    kotlin("plugin.spring") version "1.6.21" apply false
    kotlin("kapt") version "1.6.21" apply false
    id("io.qameta.allure") version "2.9.6" apply false
    id("com.google.protobuf") version "0.8.17" apply false
    idea
    kotlin("jvm") version "1.8.10"
}

val protobufVersion by extra { "3.21.1" }
val grpcVersion by extra { "1.47.0" }
val grpcKotlinVersion by extra { "1.3.0" }

subprojects {
    apply {
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.jetbrains.kotlin.kapt")
        plugin("io.qameta.allure")
        plugin("idea")
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://jfrog.belkacar.ru/artifactory/belkacar-maven-local")
            metadataSources {
                gradleMetadata()
                mavenPom()
                artifact()
            }
        }
    }

    val protobufVersion: String by rootProject.extra
    val grpcVersion: String by rootProject.extra
    val grpcKotlinVersion: String by rootProject.extra

    dependencyManagement {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2021.0.2")
        }
        dependencies {
            /* platform and language dependencies */
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.2")

            dependency("org.locationtech.jts:jts-core:1.18.2")
            dependency("org.locationtech.jts.io:jts-io-common:1.18.2")
            dependency("org.n52.jackson:jackson-datatype-jts:1.2.10")
            dependency("com.github.jasync-sql:jasync-r2dbc-mysql:2.0.6")

            /* gRPC dependencies */
            dependency("com.google.protobuf:protobuf-java:$protobufVersion")
            dependency("io.grpc:grpc-stub:$grpcVersion")
            dependency("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
            dependency("io.grpc:grpc-protobuf:$grpcVersion")
            dependency("com.google.protobuf:protobuf-kotlin:$protobufVersion")
            dependency("io.grpc:grpc-netty-shaded:$grpcVersion")

            /* spring */
            dependency("io.github.lognet:grpc-spring-boot-starter:4.8.1")

            /* gRPC proto */
            dependency("proto.belka.telematics.geofences:telematics-geofences-ops-proto:0.0.9")

            /* test dependencies */
            dependency("org.locationtech.jts.io:jts-io-common:1.18.2")
            dependency("ru.belkacar.core.test.tools:allure-annotations:2021.2")
            dependency("ru.belkacar.core.test.tools:reactor-allure-extentions:2021.1")
            dependency("io.github.serpro69:kotlin-faker:1.11.0")
        }
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
                sourceCompatibility = "11"
                jvmTarget = "11"
            }
        }

        withType<Test> {
            doFirst {
                systemProperty("allure.label.layer", "e2e")
                systemProperty("allure.results.directory", "${rootProject.projectDir}/allure-results")
            }
            testLogging {
                showExceptions = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                showCauses = true
                showStackTraces = true
                showStandardStreams = true
                outputs.upToDateWhen { false }
            }
        }

        withType(Test::class.java).configureEach {
            val iTags = System.getProperty("includeTags")
            val eTags = System.getProperty("excludeTags")
            useJUnitPlatform {
                if (!iTags.isNullOrEmpty()) {
                    includeTags(iTags)
                } else if (!eTags.isNullOrEmpty()) {
                    excludeTags(eTags)
                }
            }
        }

        register("geofence-tests", Test::class.java) {
            useJUnitPlatform {
                includeTags("geofence-services")
            }
        }

        register("broadcasting-tests", Test::class.java) {
            useJUnitPlatform {
                includeTags("broadcasting-services")
            }
        }

        register("detector-tests", Test::class.java) {
            useJUnitPlatform {
                includeTags("detector")
            }
        }

        register("hash-preprocessor-tests", Test::class.java) {
            useJUnitPlatform {
                includeTags("hash-preprocessor")
            }
        }

        register("e2e-tests", Test::class.java) {
            doFirst {
                systemProperty("spring.profiles.active", "e2e-tests")
            }
            useJUnitPlatform {
                includeTags("e2e")
            }
        }

        register<Delete>("deleteAllureStuff") {
            delete("${projectDir.path}/build/allure-results")
            delete("${projectDir.path}/build/reports")
        }
    }

}
dependencies {
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