/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI

// Copied from the Kotlin project
// Source: https://github.com/JetBrains/kotlin/blob/v2.3.10/repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts

/**
 *  The list of repositories supported by cache redirector should be synced with the "Table of redirects" at https://cache-redirector.jetbrains.com
 *  To add a repository to the list, create an issue in the ADM project (example issue https://youtrack.jetbrains.com/issue/IJI-149)
 *  Or send a merge request to https://jetbrains.team/p/iji/repositories/Cache-Redirector/files/64b69490c54a2a900bb3dd21471f942270289a12/images/config-gen/src/main/kotlin/Config.kt
 */
val cacheMap: Map<String, String> = mapOf(
    "https://repo.maven.apache.org/maven2" to "https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2",
    "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org/m2",
    "https://dl.google.com/dl/android/maven2" to "https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2",
    "https://redirector.kotlinlang.org/maven/ktor-eap" to "https://cache-redirector.jetbrains.com/redirector.kotlinlang.org/maven/ktor-eap",
    "https://redirector.kotlinlang.org/maven/dev" to "https://cache-redirector.jetbrains.com/redirector.kotlinlang.org/maven/dev",
    "https://packages.confluent.io/maven" to "https://cache-redirector.jetbrains.com/packages.confluent.io/maven",
    "https://jitpack.io" to "https://cache-redirector.jetbrains.com/jitpack.io",
    "https://services.gradle.org/distributions" to "https://cache-redirector.jetbrains.com/services.gradle.org/distributions",
    "https://registry.yarnpkg.com" to "https://cache-redirector.jetbrains.com/registry.yarnpkg.com",
    "https://nodejs.org/dist" to "https://cache-redirector.jetbrains.com/nodejs.org/dist",
)

val aliases = mapOf(
    "https://repo1.maven.org/maven2" to "https://repo.maven.apache.org/maven2",
    "https://maven.google.com" to "https://dl.google.com/dl/android/maven2",
)

fun String.maybeRedirect(): String {
    val url = this.trimEnd('/')
    val deAliasedUrl = aliases.getOrDefault(url, url)
    val cacheUrlEntry = cacheMap.entries.find { (origin, _) -> deAliasedUrl.startsWith(origin) } ?: return this

    val cacheUrl = cacheUrlEntry.value
    val originRestPath = deAliasedUrl.substringAfter(cacheUrlEntry.key, "")
    return "$cacheUrl$originRestPath"
}

fun URI.maybeRedirect(): URI = URI(toString().maybeRedirect())

fun RepositoryHandler.redirect() = configureEach {
    when (this) {
        is MavenArtifactRepository -> url = url.maybeRedirect()
        is IvyArtifactRepository -> @Suppress("SENSELESS_COMPARISON") if (url != null) {
            url = url.maybeRedirect()
        }
    }
}

// Native compiler download url override section

fun Project.overrideNativeCompilerDownloadUrl() {
    logger.info("Redirecting Kotlin/Native compiler download url")
    extensions.extraProperties["kotlin.native.distribution.baseDownloadUrl"] =
        "https://cache-redirector.jetbrains.com/download.jetbrains.com/kotlin/native/builds"
}

// Main configuration

gradle.beforeSettings {
    extensions.extraProperties["ktorbuild.cacheRedirectorEnabled"] = true

    logger.info("Redirecting repositories for settings in ${settingsDir.absolutePath}")
    pluginManagement.repositories.redirect()
    dependencyResolutionManagement.repositories.redirect()
    buildscript.repositories.redirect()
}

gradle.beforeProject {
    buildscript.repositories.redirect()
    repositories.redirect()
    overrideNativeCompilerDownloadUrl()
}
