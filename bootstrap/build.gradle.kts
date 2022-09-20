plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":kafka"))
    implementation(project(":grpc"))

    implementation("org.springframework.boot", "spring-boot-starter")
}
