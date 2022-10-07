
dependencies {
    implementation(project(":domain-api"))
    implementation(project(":kafka"))
    
    
    
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin") //todo: remove
}