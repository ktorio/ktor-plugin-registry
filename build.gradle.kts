@file:Suppress("UnstableApiUsage")

import io.ktor.plugins.registry.*
import java.nio.file.Paths

val targets by lazy { fetchKtorTargets() }

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.21"
}

group = "io.ktor"
version = "1.0-SNAPSHOT"

repositories {
    allRepositories()
}

configurations {
    for (target in targets)
        target.releaseConfigs.forEach(::create)
}

dependencies {
    // each ktor version has its own classpath
    for (target in targets) {
        for ((config, version) in target.releases) {
            config("io.ktor:ktor-${target.name}-core:$version")

            for (dependency in target.allArtifactsForVersion(version))
                config(dependency)
        }
    }
    val latestKtor = targets.first().releases.last().version

    // current ktor dependencies for handling manifests
    implementation("io.ktor:ktor-server-core:$latestKtor")
    implementation("io.ktor:ktor-client-core:$latestKtor")

    // inspection of install blocks
    implementation(libs.kotlin.compiler)

    // serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    // resolving versions
    implementation(libs.semver)

    //logging
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j)


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
            "buildSrc/src/main/kotlin/io/ktor/plugins/registry/PluginReference.kt",
            "buildSrc/src/main/kotlin/io/ktor/plugins/registry/PluginCollector.kt",
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
    register("resolvePlugins") {
        group = "plugins"
        description = "Locate plugin resources from version definitions"
        doLast {
            for (target in targets) {
                val resolvedArtifacts = target.releases.associate { release ->
                    release.version to configurations[release.config].resolvedConfiguration.resolvedArtifacts
                }
                outputReleaseArtifacts(
                    outputFile = Paths.get("build/${target.name}-artifacts.yaml"),
                    configurations = resolvedArtifacts
                )
            }
        }
    }

    // builds the registry for distributing to the project generator
    register<JavaExec>("buildRegistry") {
        group = "plugins"
        description = "Build the registry from plugin resources"
        mainClass = "io.ktor.plugins.registry.BuildRegistryKt"
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