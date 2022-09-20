import com.google.protobuf.gradle.*

plugins {
    id("com.google.protobuf")
}

dependencies {
    protobuf("proto.belka.telematics.geofences:telematics-geofences-ops-proto")
    compileOnly("com.google.protobuf", "protobuf-java")
    compileOnly("com.google.protobuf", "protobuf-kotlin")
    api("io.grpc", "grpc-protobuf")
    api("io.grpc", "grpc-kotlin-stub")
    api("io.grpc","grpc-stub")
    implementation("io.grpc:grpc-netty-shaded")

    compileOnly("org.springframework", "spring-context")

    testImplementation("ru.belkacar.core.test.tools", "reactor-allure-extentions")
    testImplementation("ru.belkacar.core.test.tools", "allure-annotations")
}

sourceSets {
    main {
        proto {
            srcDirs("src/main")
        }
    }
}

protobuf {
    val protobufVersion: String by rootProject.extra
    val grpcVersion: String by rootProject.extra
    val grpcKotlinVersion: String by rootProject.extra

    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }

        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.builtins {
                id("kotlin") // generates kotlin DLS for given .proto
                // proto files generated to Java as usual, Kotlin used for inline builders for more clean code
            }

            it.plugins {
                id("grpc") // used for well known rpc services generation
            }
        }
    }
}
