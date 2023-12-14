import java.util.*

val rootProjectProperties = Properties().also {
    file("../gradle.properties").reader().use(it::load)
}
val kamlVersion = rootProjectProperties["kaml.version"]
val semverVersion = rootProjectProperties["semver.version"]

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.charleskorn.kaml:kaml:$kamlVersion")
    implementation("com.vdurmont:semver4j:$semverVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}