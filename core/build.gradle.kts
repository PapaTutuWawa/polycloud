import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "me.polynom"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Externalised type definitions
    implementation(project(":pluginBase"))

    // Add all first-party plugins only while developing.
    developmentOnly(project(":authOidc"))
    developmentOnly(project(":examplePlugin"))
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

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// Spring's dependency-management plugin force-aligns every configuration (including
// detekt's isolated analysis classpath) to the project's Kotlin stdlib version, but
// detekt ships its own embedded compiler and refuses to run against a mismatched one.
configurations.matching { it.name.startsWith("detekt") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.4.10")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<BootRun> {
    // Enable custom profiles while running locally
    systemProperty("spring.profiles.active", "dev,papatutuwawa,ostylk")
}