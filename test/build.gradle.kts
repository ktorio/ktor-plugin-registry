plugins {
    kotlin("jvm")
    alias(libs.plugins.kotest)
}

dependencies {
    implementation(libs.kotlinx.io.core)
    testImplementation(libs.kotest.framework)
    testImplementation(libs.kotest.assertions)
    testImplementation("org.jetbrains:kastle-core:1.0.0-SNAPSHOT")
    testImplementation("org.jetbrains:kastle-local:1.0.0-SNAPSHOT")
    testImplementation(libs.kotlin.compiler)
}
