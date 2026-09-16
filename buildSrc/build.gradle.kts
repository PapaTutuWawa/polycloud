plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.2")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.5.11")
}