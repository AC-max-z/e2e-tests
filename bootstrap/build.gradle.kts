plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":kafka"))

    implementation("org.springframework.boot", "spring-boot-starter")
}
