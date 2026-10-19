plugins {
    id("me.polynom.common")
}

dependencies {
    // Plugin types
    implementation(project(":pluginBase"))

    // Spring
    compileOnly(libs.spring.boot.starter)

    // JSON
    implementation(libs.kotlinx.serialization.json)

    // OpenAPI
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)

    // JWT verification
    implementation(libs.java.jwt)
    implementation(libs.jwks.rsa)
}
