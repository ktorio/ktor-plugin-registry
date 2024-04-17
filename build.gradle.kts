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

repositories {
    allRepositories()
}

configurations {
    for (target in targets)
        target.releaseConfigs.forEach(::create)
}

kotlin {
    jvmToolchain {
        check(this is JavaToolchainSpec)
        languageVersion.set(JavaLanguageVersion.of(11))
    }
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
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.simple)

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
    toolVersion = libs.versions.detekt.version.get()
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
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

    // generates the appropriate directory structure with some templates for a new plugin
    register<JavaExec>("createPlugin") {
        group = "plugins"
        description = "Creates a skeleton for a new plugin"
        mainClass = "io.ktor.plugins.registry.CreatePluginKt"
        classpath = sourceSets["main"].runtimeClasspath
        standardInput = System.`in`
    }

    // builds the registry for distributing to the project generator
    register<JavaExec>("buildRegistry") {
        group = "plugins"
        description = "Build the registry from plugin resources"
        mainClass = "io.ktor.plugins.registry.BuildRegistryKt"
        classpath = sourceSets["main"].runtimeClasspath
        dependsOn("resolvePlugins")
    }

    // generates a test project using the modified plugins in the repository
    register<JavaExec>("buildTestProject") {
        group = "plugins"
        description = "Generates a test project from the newly registered plugins"
        mainClass = "io.ktor.plugins.registry.GenerateSampleProjectKt"
        classpath = sourceSets["main"].runtimeClasspath
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
