plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.2")
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.6")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
}