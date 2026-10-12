plugins {
    id("me.polynom.common")
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot:4.1.1")
    testImplementation("org.springframework.boot:spring-boot:4.1.1")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
    compileOnly("org.springframework.boot:spring-boot-starter-validation:4.1.1")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")

    // Plugin types
    implementation(project(":pluginBase"))

    // Data JPA
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa:4.1.1")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.1")
    implementation("io.hypersistence:hypersistence-utils-hibernate-73:3.15.4")

    // Mapstruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    // Serialization
    implementation("tools.jackson.module:jackson-module-kotlin:3.2.+")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.1")
}
