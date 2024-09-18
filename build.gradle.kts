/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")

import io.ktor.plugins.registry.*
import java.nio.file.Paths

val targets by lazy { fetchKtorTargets(logger) }

plugins {
    alias(libs.plugins.serialization)
    alias(libs.plugins.jvm)
    alias(libs.plugins.detekt)
}

group = "io.ktor"
version = "1.0-SNAPSHOT"

configurations {
    for (target in targets)
        target.releaseConfigs.forEach(::create)
}

dependencies {
    // each ktor version has its own classpath
    for (target in targets) {
        for ((config, version) in target.releases) {
            config("io.ktor:ktor-${target.name}-core:$version")
            config(kotlin("stdlib"))

            // test imports for test_function template
            if (target.name == "server") {
                config(kotlin("test"))
                config(kotlin("test-junit"))
                config("io.ktor:ktor-server-test-host:$version")
            }

            for (dependency in target.allArtifactsForVersion(version))
                config(dependency)
        }
    }
    val latestKtor = targets.first().releases.last().version

    // current ktor dependencies for handling manifests
    implementation("io.ktor:ktor-server-core:$latestKtor")
    implementation("io.ktor:ktor-client-core:$latestKtor")
    implementation("io.ktor:ktor-client-cio:$latestKtor")

    // inspection of install blocks
    implementation(libs.kotlin.compiler)

    // serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    // resolving versions
    implementation(libs.maven.artifact)

    // logging
    implementation(libs.logback.classic)

    // finding changed files
    implementation(libs.jgit)

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

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
}

tasks {
    // use JUnit 5 for all test tasks
    withType<Test> {
        useJUnitPlatform()
    }

    /**
     *  We copy this shared source file with the parent project because there is otherwise
     *  a chicken/egg problem with building the project which confuses the IDEA.
     */
    val copyPluginTypes by registering(Copy::class) {
        group = "build"
        from(
            "buildSrc/src/main/kotlin/io/ktor/plugins/registry/PluginReference.kt",
            "buildSrc/src/main/kotlin/io/ktor/plugins/registry/PluginCollector.kt",
        )
        into("build/copied")
    }

    // download all sources
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xcontext-receivers",
                "-XdownloadSources=true"
            )
        }
        dependsOn(copyPluginTypes)
    }

    // resolving plugin jars from custom classpaths
    val resolvePlugins by registering {
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

    // generates the appropriate directory structure with some templates for a new plugin
    val createPlugin by registering(JavaExec::class) {
        group = "plugins"
        description = "Creates a skeleton for a new plugin"
        mainClass = "io.ktor.plugins.registry.CreatePluginKt"
        classpath = sourceSets["main"].runtimeClasspath
        standardInput = System.`in`
    }

    // builds the registry for distributing to the project generator
    val buildRegistry by registering(JavaExec::class) {
        group = "plugins"
        description = "Build the registry from plugin resources"
        mainClass = "io.ktor.plugins.registry.BuildRegistryKt"
        classpath = sourceSets["main"].runtimeClasspath
        dependsOn(resolvePlugins)
    }

    // generates a test project using the modified plugins in the repository
    val buildTestProject by registering(JavaExec::class) {
        group = "plugins"
        description = "Generates a test project from the newly registered plugins"
        mainClass = "io.ktor.plugins.registry.GenerateTestProjectKt"
        classpath = sourceSets["main"].runtimeClasspath
    }

    // compresses registry output into a tar file
    val packageRegistry by registering(Tar::class) {
        group = "plugins"
        description = "Compresses registry to tar file for distribution"
        archiveFileName.set("registry.tar.gz")
        destinationDirectory.set(file("build/distributions"))
        compression = Compression.GZIP
        from("build/registry")
        dependsOn(buildRegistry)
    }
}
