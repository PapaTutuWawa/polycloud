plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
    kotlin("plugin.jpa")
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
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation(kotlin("test"))
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
