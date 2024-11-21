/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation


plugins {
    `kotlin-dsl`
    alias(libs.plugins.serialization)
}

val shared = "shared"

configurations {
    create(shared)
}

dependencies {
    shared(kotlin("stdlib"))
    shared(libs.kotlinx.serialization.core)
    shared(libs.maven.artifact)

    implementation(libs.kaml)
    implementation(libs.maven.artifact)

    testImplementation(kotlin("test"))
}

sourceSets {
    val sharedSrc = create(shared) {
        kotlin.srcDir("../shared")
        compileClasspath += configurations[shared]
    }
    main {
        compileClasspath += sharedSrc.output
        runtimeClasspath += sharedSrc.output
    }
    test {
        compileClasspath += sharedSrc.output
        runtimeClasspath += sharedSrc.output
    }
}

val packageShared = tasks.register<Jar>("packageShared") {
    archiveBaseName.set("shared")
    from(sourceSets["shared"].output)
}

// Task to copy output of shared source set to main output
val copySharedOutput = tasks.register<Copy>("copySharedOutput") {
    from(sourceSets["shared"].output)
    into(sourceSets["main"].output.classesDirs.first { it.toString().contains("kotlin") })
    finalizedBy(packageShared)
}

tasks.named("compileKotlin").configure {
    finalizedBy(copySharedOutput)
}

for (task in listOf("jar", "pluginUnderTestMetadata", "validatePlugins").map { tasks.named(it) }) {
    task.configure {
        mustRunAfter(copySharedOutput)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}
