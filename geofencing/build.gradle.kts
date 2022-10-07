import com.google.protobuf.gradle.builtins
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf")
}

dependencies {
    api(project(":domain-api"))
    implementation(project(":grpc"))
    implementation(project(":kafka"))
    
    protobuf("proto.belka.telematics.geofences:telematics-geofences-ops-proto")
    
    implementation("org.springframework", "spring-context")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-reactor")
    
    implementation("com.fasterxml.jackson.core", "jackson-databind")
    
    api("com.google.protobuf", "protobuf-java")
    api("com.google.protobuf", "protobuf-kotlin")
    compileOnly("io.grpc", "grpc-protobuf")
    compileOnly("io.grpc", "grpc-kotlin-stub")
    compileOnly("io.grpc","grpc-stub")
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
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
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
                id("grpckt")
            }
        }
    }
}
