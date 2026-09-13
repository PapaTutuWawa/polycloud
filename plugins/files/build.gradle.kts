plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.20"
}

group = "me.polynom"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot:4.1.1")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")

    // Plugin types
    implementation(project(":pluginBase"))

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}
