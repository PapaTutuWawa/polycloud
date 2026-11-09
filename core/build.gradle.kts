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

    // SpringBoot
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)

    // JSON
    implementation(libs.kotlinx.serialization.json)

    // Allow generating OpenAPI specs
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Just use PostgreSQL for development
    developmentOnly(libs.postgresql)

    // JWT
    implementation(libs.java.jwt)

    // Testing
    testImplementation(libs.bundles.test)
}

tasks.withType<BootRun> {
    // Enable custom profiles while running locally
    systemProperty("spring.profiles.active", "dev,papatutuwawa,ostylk")
}