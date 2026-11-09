import dev.detekt.gradle.Detekt
import org.gradle.kotlin.dsl.withType

// Plugin Configuration
plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("dev.detekt")
    id("com.diffplug.spotless")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

// GroupId and Version
group = "me.polynom.polycloud"
version = "0.0.1-SNAPSHOT"

// Toolchain Config
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// Repositories and Dependencies shared everywhere
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
}

// JUnit tests if applicable
tasks.withType<Test> {
    useJUnitPlatform()
}

// Linter & Static Analyzer
spotless {
    kotlin {
        ktlint()
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("gradle/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required = true
        html.outputLocation = layout.buildDirectory.file("reports/detekt.html")
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
