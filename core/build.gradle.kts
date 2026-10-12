import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("me.polynom.common")
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Externalised type definitions
    implementation(project(":pluginBase"))

    // Add all first-party plugins only while developing.
    developmentOnly(project(":authOidc"))
    developmentOnly(project(":files"))
    developmentOnly(project(":stubAuth"))
    developmentOnly(project(":calendar"))

    // Allow generating OpenAPI specs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // Just use PostgreSQL for development
    developmentOnly("org.postgresql:postgresql:42.7.13")

    // SpringBoot
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-kotlinx-serialization-json")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // JWT
    implementation("com.auth0:java-jwt:4.6.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}

tasks.withType<BootRun> {
    // Enable custom profiles while running locally
    systemProperty("spring.profiles.active", "dev,papatutuwawa,ostylk")
}