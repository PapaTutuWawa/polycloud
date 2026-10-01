plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") version "2.4.20" apply false
    kotlin("plugin.spring") version "2.4.20" apply false
    kotlin("plugin.jpa") version "2.4.20" apply false
    id("org.jetbrains.kotlin.kapt") apply false
}

allprojects {
    apply(plugin = "common")
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
