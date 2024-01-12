import java.util.*

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kaml)
    implementation(libs.semver)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}