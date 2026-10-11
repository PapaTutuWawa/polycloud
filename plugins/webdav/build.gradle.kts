plugins {
    kotlin("jvm")
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
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")

    // Plugin types
    implementation(project(":pluginBase"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc:4.1.1")
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
