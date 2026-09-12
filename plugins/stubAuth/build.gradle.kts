plugins {
    kotlin("jvm") version "2.4.10"
}

group = "me.polynom.polycloud.apps.auth.stub"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot:4.1.1")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")

    // Plugin types
    implementation(project(":pluginBase"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(26)
}

tasks.test {
    useJUnitPlatform()
}