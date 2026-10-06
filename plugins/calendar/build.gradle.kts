plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.jpa")
}

group = "me.polynom"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot:4.1.1")
    testImplementation("org.springframework.boot:spring-boot:4.1.1")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")

    // Serialization
    implementation("tools.jackson.module:jackson-module-kotlin:3.2.+")

    // OpenAPI
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // Data JPA
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa:4.1.1")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.1")
    implementation("io.hypersistence:hypersistence-utils-hibernate-73:3.15.4")

    // Mapstruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    // Plugin types
    implementation(project(":pluginBase"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.1")
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}