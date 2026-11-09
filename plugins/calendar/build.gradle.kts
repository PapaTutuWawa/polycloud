plugins {
    id("me.polynom.common")
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    // Plugin types
    implementation(project(":pluginBase"))

    // Spring MVC
    compileOnly(libs.spring.boot.starter.webmvc)
    testImplementation(libs.spring.boot.starter.webmvc)

    // Data JPA
    compileOnly(libs.spring.boot.starter.data.jpa)
    testImplementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.hypersistence.utils.hibernate)

    // Serialization
    implementation(libs.jackson.module.kotlin)

    // Mapstruct
    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor)

    // OpenAPI
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)

    // Testing
    testImplementation(libs.bundles.test)
}
