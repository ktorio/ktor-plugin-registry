plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.charleskorn.kaml:kaml:0.55.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}