@file:Suppress("UnstableApiUsage")

import io.ktor.plugins.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readLines

val latestKtor = project.properties["ktor.version"]
val kamlVersion = project.properties["kaml.version"]
val serializationVersion = project.properties["serialization.version"]
val reflectionsVersion = project.properties["reflections.version"]
val semverVersion = project.properties["semver.version"]
val kotlinLoggingVersion = project.properties["kotlin.logging.version"]
val slf4jVersion = project.properties["slf4j.version"]

val ktorReleases = Paths.get("ktor_releases").readLines()
val pluginsDir: Path = project.rootDir.toPath().resolve("plugins")
val pluginsOutputDir: Path = project.rootDir.toPath().resolve("generated/plugins")

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
}

group = "io.ktor"
version = "1.0-SNAPSHOT"

repositories {
    allRepositories()
}

configurations {
    ktorReleases.forEach(::create)
}

dependencies {
    // each ktor version has its own classpath
    for (ktorRelease in ktorReleases) {
        ktorRelease("io.ktor:ktor-server-core:$ktorRelease")

        pluginsDir.readPluginFiles().allArtifactsForVersion(ktorRelease).forEach { dependency ->
            ktorRelease(dependency)
        }
    }

    // reflections
    implementation("org.reflections:reflections:$reflectionsVersion")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("com.charleskorn.kaml:kaml:$kamlVersion")

    // resolving versions
    implementation("com.vdurmont:semver4j:$semverVersion")

    //logging
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")

    testImplementation(kotlin("test"))
}

// include relevant copied classes from buildSrc module
sourceSets {
    main {
        kotlin {
            srcDir("build/copied")
        }
    }
}

tasks {
    // use junit 5
    test {
        useJUnitPlatform()
    }

    /**
     *  We copy this shared source file with the parent project because there is otherwise
     *  a chicken/egg problem with building the project which confuses the IDEA.
     */
    register<Copy>("copyPluginTypes") {
        group = "build"
        from(
            "buildSrc/src/main/kotlin/io/ktor/plugins/PluginReference.kt",
            "buildSrc/src/main/kotlin/io/ktor/plugins/PluginCollector.kt",
        )
        into("build/copied")
    }

    // download all sources
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-XdownloadSources=true")
        }
        dependsOn("copyPluginTypes")
    }

    // resolving plugin jars from custom classpaths
    register<DefaultTask>("resolvePlugins") {
        group = "plugins"
        description = "Locate plugin resources from version definitions"
        doLast {
            outputReleaseArtifacts(
                outputDir = Paths.get("build"),
                configurations = ktorReleases.associateWith { release ->
                    configurations[release].resolvedConfiguration.resolvedArtifacts
                }
            )
        }
    }

    // builds the registry for distributing to the project generator
    register<JavaExec>("buildRegistry") {
        group = "plugins"
        description = "Build the registry from plugin resources"
        mainClass = "io.ktor.plugins.BuildRegistryKt"
        classpath = sourceSets["main"].runtimeClasspath
        dependsOn("resolvePlugins")
    }

    // compresses registry output into a tar file
    register<Tar>("packageRegistry") {
        group = "plugins"
        description = "Compresses registry to tar file for distribution"
        archiveFileName.set("registry.tar.gz")
        destinationDirectory.set(file("build/distributions"))
        compression = Compression.GZIP
        from("build/registry")
        dependsOn("buildRegistry")
    }
}