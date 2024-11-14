/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")

import io.ktor.plugins.registry.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorReleases = getKtorReleases(logger)
val latestKtor = ktorReleases.last()
val pluginConfigs by lazy { collectPluginConfigs(logger, ktorReleases) }

plugins {
    alias(libs.plugins.serialization)
    alias(libs.plugins.jvm)
    alias(libs.plugins.detekt)
}

group = "io.ktor"
version = "1.0-SNAPSHOT"

// create build config for each valid release-plugin-target triple
configurations {
    // create plugin build configs
    for (pluginConfig in pluginConfigs) {
        create(pluginConfig.name) {
            pluginConfig.parent?.let { parent ->
                extendsFrom(get(parent))
            }
        }
    }
}

repositories {
    mavenCentral()

    for (repositoryUrl in pluginConfigs.flatMap { it.repositories }.distinct()) {
        maven(repositoryUrl)
    }
}

sourceSets {
    // include all the plugins as source paths, using the latest valid ktor release for each
    for (pluginConfig in pluginConfigs.latestByPath()) {
        create(pluginConfig.name) {
            kotlin.srcDir("plugins/${pluginConfig.path}")
            compileClasspath += configurations[pluginConfig.name]
            pluginConfig.parent?.let { parent ->
                compileClasspath += configurations[parent]
                compileClasspath += sourceSets[parent].output
            }
        }
    }
}

dependencies {
    // create a build config for every plugin-release-module combination
    for (pluginConfig in pluginConfigs) {
        val type = pluginConfig.type
        val release = pluginConfig.release
        val config = pluginConfig.name

        // common dependencies
        config(kotlin("stdlib"))
        config(kotlin("test"))
        config(kotlin("test-junit"))
        config("io.ktor:ktor-$type-core:$release")
        when(type) {
            "client" -> config("io.ktor:ktor-client-mock:$release")
            "server" -> config("io.ktor:ktor-server-test-host:$release")
        }

        // artifacts for the specific plugin version
        for ((group, name, version) in pluginConfig.artifacts)
            config("$group:$name:${version.resolvedString}")
    }

    // shared sources used in buildSrc
    implementation(files("buildSrc/build/libs/shared.jar"))

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

detekt {
    toolVersion = libs.versions.detekt.version.get()
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
}

tasks {
    // use JUnit 5 for all test tasks
    withType<Test> {
        useJUnitPlatform()
    }

    // download all sources
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xcontext-receivers",
                "-XdownloadSources=true"
            )
        }
    }

    // resolving plugin jars from custom classpaths
    val resolvePlugins by registering {
        group = "plugins"
        description = "Locate plugin resources from version definitions"
        doLast {
            writeResolvedPluginConfigurations(pluginConfigs) { configName ->
                configurations[configName].resolvedConfiguration
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

    // compiles ALL build targets before building the registry
    val compileAll by registering {
        group = "build"
        description = "Compile all source sets"
        dependsOn(withType<KotlinCompile>())
    }

    // builds the registry for distributing to the project generator
    val buildRegistry by registering(JavaExec::class) {
        group = "plugins"
        description = "Build the registry from plugin resources"
        mainClass = "io.ktor.plugins.registry.BuildRegistryKt"
        classpath = sourceSets["main"].runtimeClasspath
        dependsOn(resolvePlugins, compileAll)
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
