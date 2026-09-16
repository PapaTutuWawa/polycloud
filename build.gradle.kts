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
        dependsOn(it.tasks.named("spotbugsMain"))
    }
}