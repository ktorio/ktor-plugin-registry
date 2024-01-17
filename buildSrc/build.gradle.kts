/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

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
    implementation(libs.maven.artifact)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
