plugins {
    id("me.polynom.common")
}

dependencies {
    // Plugin types
    implementation(project(":pluginBase"))

    compileOnly(libs.spring.boot.starter.webmvc)
    testImplementation(libs.spring.boot.starter.webmvc)
}
