plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework:spring-web:7.0.9")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")

    // Plugin types
    implementation(project(":pluginBase"))

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // OpenAPI
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // JWT verification
    implementation("com.auth0:java-jwt:4.6.0")
    implementation("com.auth0:jwks-rsa:0.24.1")
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
