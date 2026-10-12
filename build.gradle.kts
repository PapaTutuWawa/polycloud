plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.bom) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
}

tasks.register("run-core") {
    description = "Runs the core service"
    dependsOn(project(":core").tasks.named("bootRun"))
}

tasks.register("jar") {
    description = "Packages each subproject into a jar file"
    subprojects.forEach {
        dependsOn(it.tasks.named("jar"))
    }
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
