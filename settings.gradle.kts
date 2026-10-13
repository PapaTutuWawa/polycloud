rootProject.name = "polycloud"

pluginManagement {
    includeBuild("build-logic")
}

// Add submodules
include("core")

// Plugin types
include("pluginBase")
project(":pluginBase").projectDir = file("plugins/pluginBase")

// OIDC Plugin
include("authOidc")
project(":authOidc").projectDir = file("plugins/authOidc")

// Example auth plugin
include("stubAuth")
project(":stubAuth").projectDir = file("plugins/stubAuth")

// Files
include("files")
project(":files").projectDir = file("plugins/files")

// Calendar
include("calendar")
project(":calendar").projectDir = file("plugins/calendar")