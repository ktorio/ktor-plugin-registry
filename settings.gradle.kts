/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://packages.jetbrains.team/maven/p/kastle/maven")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        maven("https://redirector.kotlinlang.org/maven/kxrpc-grpc")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.jetbrains.kastle") version "0.1.3"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://packages.jetbrains.team/maven/p/kastle/maven")

        // TODO pack repositories should be loaded automatically
        maven("https://packages.confluent.io/maven")
        maven("https://jitpack.io")
        maven("https://redirector.kotlinlang.org/maven/kxrpc-grpc")
    }
    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.4.3")
        }
    }
}

include("test")
