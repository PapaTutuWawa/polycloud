tasks.register("run-core") {
    description = "Runs the core service"
    dependsOn(project(":core").tasks["bootRun"])
}

tasks.register("pluginBase") {
    description = "Builds the plugin package into a jar file"
    dependsOn(project(":pluginBase").tasks["jar"])
}

tasks.register("core") {
    description = "Builds the core service into a jar file"
    dependsOn(project(":core").tasks["bootJar"])
}