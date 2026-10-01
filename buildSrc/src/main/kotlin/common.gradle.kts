import dev.detekt.gradle.Detekt

plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("dev.detekt")
}

spotless {
    kotlin {
        ktlint()
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required = true
        html.outputLocation = file("$buildDir/reports/detekt.html")
    }
}