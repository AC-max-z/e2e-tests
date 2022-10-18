plugins {
    id("org.springframework.boot")
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
}
