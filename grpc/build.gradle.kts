import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.google.protobuf")
    kotlin("jvm") version "1.8.10"
}

dependencies {
//    protobuf("proto.belka.telematics.geofences:telematics-geofences-ops-proto")
    api("com.google.protobuf", "protobuf-java")
    api("com.google.protobuf", "protobuf-kotlin")
    api("io.grpc", "grpc-protobuf")
    api("io.grpc", "grpc-kotlin-stub")
    api("io.grpc","grpc-stub")
    implementation("io.grpc:grpc-netty-shaded")
    
//    api("org.jetbrains.kotlinx", "kotlinx-coroutines-reactor")

    compileOnly("org.springframework", "spring-context")

//    testImplementation("ru.belkacar.core.test.tools", "reactor-allure-extentions")
//    testImplementation("ru.belkacar.core.test.tools", "allure-annotations")
    implementation(kotlin("stdlib-jdk8"))
}

//sourceSets {
//    main {
//        proto {
//            srcDirs("src/main")
//        }
//    }
//}
//
//protobuf {
//    val protobufVersion: String by rootProject.extra
//    val grpcVersion: String by rootProject.extra
//    val grpcKotlinVersion: String by rootProject.extra
//
//    protoc {
//        artifact = "com.google.protobuf:protoc:$protobufVersion"
//    }
//
//    plugins {
//        id("grpc") {
//            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
//        }
//
//        id("grpckt") {
//            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
//        }
//    }
//
//    generateProtoTasks {
//        all().forEach {
//            it.builtins {
//                id("kotlin") // generates kotlin DLS for given .proto
//                // proto files generated to Java as usual, Kotlin used for inline builders for more clean code
//            }
//
//            it.plugins {
//                id("grpc") // used for well known rpc services generation
//                id("grpckt")
//            }
//        }
//    }
//}
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