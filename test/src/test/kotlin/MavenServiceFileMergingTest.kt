/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.registry

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.kastle.PackId
import org.jetbrains.kastle.ProjectDescriptor
import org.jetbrains.kastle.VariableId
import org.jetbrains.kastle.io.export
import org.jetbrains.kastle.io.resolve
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.io.path.listDirectoryEntries

/**
 * The `jar-with-dependencies` assembly descriptor does not merge META-INF/services
 * files, so when multiple dependencies contribute ConfigLoader SPI entries, only
 * one survives in the fat jar.
 * To solve the issue, a custom `fat-jar` assembly descriptor is used.
 * This test verifies that when YAML config support is added alongside the default HOCON loader,
 * both entries are present.
 */
val MavenServiceFileMerging by testSuite("Maven service file merging") {
    val environment = setupTestEnvironment("maven-service-merge")

    test("ConfigLoader service file has both HOCON and YAML entries in fat-jar") {
        val projectDir = environment.outputDir.resolve("project")
        SystemFileSystem.createDirectories(projectDir)

        environment.generator.generate(
            ProjectDescriptor(
                name = "maven-service-merge",
                group = "io.ktor",
                properties = mapOf(
                    VariableId.parse("io.ktor/server-core/configFormat") to "YAML"
                ),
                packs = listOf(
                    PackId("org.apache", "maven"),
                    PackId.parse("io.ktor/server-netty"),
                    PackId.parse("io.ktor/server-config-yaml"),
                ),
            )
        ).export(projectDir)

        val projectPath = Paths.get(projectDir.toString())
        val output = runWrapper(projectPath.resolve("mvnw"), projectPath, "package", "-DskipTests")
        output shouldContain "BUILD SUCCESS"

        val fatJar = projectPath.resolve("target")
            .listDirectoryEntries("*-fat-jar.jar")
            .single()

        val configLoaderEntries = ZipFile(fatJar.toFile()).use { zip ->
            val entry = checkNotNull(
                zip.getEntry("META-INF/services/io.ktor.server.config.ConfigLoader")
            ) { "ConfigLoader service file is missing from $fatJar" }
            zip.getInputStream(entry).bufferedReader().use { it.readText() }
        }
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toList()

        configLoaderEntries shouldHaveSize 2
    }
}
