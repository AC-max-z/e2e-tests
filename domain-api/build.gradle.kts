dependencies {
    compileOnly("org.springframework.boot", "spring-boot")
    implementation("org.n52.jackson", "jackson-datatype-jts")
    
    api("org.locationtech.jts", "jts-core")
    
    compileOnly("org.springframework.boot", "spring-boot-configuration-processor")
}
