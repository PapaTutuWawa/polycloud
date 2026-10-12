plugins {
    id("me.polynom.common")
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot:4.1.1")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")

    // Plugin types
    implementation(project(":pluginBase"))

    testImplementation(kotlin("test"))
}
