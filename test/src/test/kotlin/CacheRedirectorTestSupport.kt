/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.registry

import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private val useCacheRedirector: Boolean by lazy { System.getProperty("ktorbuild.useCacheRedirector").toBoolean() }
private val cacheRedirectorInitScript: String by lazy { System.getProperty("ktorbuild.cacheRedirectorInitScript") }

fun prepareGradleCacheRedirectorArguments(): List<String> = when {
    useCacheRedirector -> listOf("--init-script", cacheRedirectorInitScript)
    else -> emptyList()
}

fun prepareMavenCacheRedirectorArguments(workingDirectory: Path): List<String> {
    if (!useCacheRedirector) return emptyList()

    val settingsFile = generateMavenSettings(workingDirectory)
    return listOf("--settings", settingsFile.toString())
}

private fun generateMavenSettings(workingDirectory: Path): Path {
    val settingsFile = workingDirectory.resolve(".mvn/cache-redirector-settings.xml").absolute().normalize()
    settingsFile.parent.createDirectories()
    settingsFile.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
            <mirrors>
                <mirror>
                    <id>cache-redirector-maven-central</id>
                    <name>JetBrains Cache Redirector for Maven Central</name>
                    <url>https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2</url>
                    <mirrorOf>central</mirrorOf>
                </mirror>
            </mirrors>
        </settings>
        """.trimIndent(),
    )
    return settingsFile
}
