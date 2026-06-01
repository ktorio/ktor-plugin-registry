plugins {
    kotlin("jvm")
    alias(libs.plugins.testBalloon)
}

dependencies {
    implementation(libs.kotlinx.io.core)
    testImplementation(libs.testBalloon.framework)
    testImplementation(libs.testBalloon.kotestAssertions)
    testImplementation(libs.kastle.core)
    testImplementation(libs.kastle.local)
    testImplementation(libs.kotlin.compiler)
}
