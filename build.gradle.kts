plugins {
    kotlin("jvm") version "2.4.21" apply false
    kotlin("kapt") version "2.4.21" apply false
    id("dev.detekt") version "2.0.0-alpha.6" apply false
    id("com.diffplug.spotless") version "8.10.2" apply false
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("plugin.spring") version "2.4.21" apply false
    kotlin("plugin.jpa") version "2.4.21" apply false
}

tasks.register("run-core") {
    description = "Runs the core service"
    dependsOn(project(":core").tasks.named("bootRun"))
}

tasks.register("pluginBase") {
    description = "Builds the plugin package into a jar file"
    dependsOn(project(":pluginBase").tasks.named("jar"))
}

tasks.register("core") {
    description = "Builds the core service into a jar file"
    dependsOn(project(":core").tasks.named("jar"))
}

tasks.register("lint") {
    description = "Runs the linter and formatter on all projects"
    subprojects.forEach {
        dependsOn(it.tasks.named("spotlessApply"))
        dependsOn(it.tasks.named("detekt"))
    }
}

tasks.register("ci") {
    description = "Runs the linter and formatting checks on all projects without modifying files"
    subprojects.forEach {
        dependsOn(it.tasks.named("spotlessCheck"))
        dependsOn(it.tasks.named("detekt"))
        dependsOn(it.tasks.named("test"))
    }
}
