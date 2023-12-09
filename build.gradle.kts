import io.ktor.plugins.*
import java.nio.file.Paths

val ktorVersion = project.properties["ktor.version"]

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    application
}

group = "io.ktor"
version = "1.0-SNAPSHOT"

application {
    mainClass = "io.ktor.plugins.GeneratePluginRegistryKt"
}

repositories {
    allRepositories()
}

val pluginsClasspath = "pluginsClasspath"

configurations {
    create(pluginsClasspath)
}

dependencies {
    pluginsClasspath("io.ktor:ktor-server-auth:2.3.4")
    for (plugin in pluginFileReferences()) {
        for (artifact in plugin.artifacts.values)
            pluginsClasspath("${plugin.group}:${artifact.name}:${artifact.version}")
    }

    implementation("org.reflections:reflections:0.10.2")
    implementation("com.charleskorn.kaml:kaml:0.55.0")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
                .include("io/ktor/plugins/GeneratePluginRegistry.kt")
            // Share the data types for reading plugin references
            srcDir("buildSrc/src/main/kotlin")
                .include("io/ktor/plugins/PluginReference.kt")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<DefaultTask>("resolvePlugins") {
    group = "build"
    doLast {
        resolvePlugins(
            pluginDir = Paths.get("plugins"),
            outputDir = Paths.get("build/plugins"),
            configuration = configurations[pluginsClasspath]
        )
    }
}