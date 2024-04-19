/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import io.ktor.plugins.registry.utils.CLIUtils.colored
import io.ktor.plugins.registry.utils.CLIUtils.ktorScriptHeader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess

const val TEMPLATES = "templates"
const val DEFAULT_TYPE = "server"
const val DEFAULT_KTOR_VERSION_RANGE = "\"[2.0,)\""
const val DEFAULT_KTOR_VERSION = "2.0"
const val CYAN = 14

/**
 * Prompts contributor with a series of questions to populate a new plugin from templates.
 */
fun main() {
    val logger = LoggerFactory.getLogger("CreatePlugin")
    logger.info(ktorScriptHeader())
    logger.info("Thank you for contributing to the Ktor plugin registry!\n")
    logger.info("Let's start with a couple questions...")

    val mavenArtifact = askQuestion(
        "What is your latest maven artifact?    (${colored("io.ktor:ktor-server-core:2.3.10", CYAN)})"
    )
    val vcsUrl = askQuestion(
        "Where can I find your source code?     (${colored("https://github.com/ktorio/ktor", CYAN)})"
    )

    val (group, artifact, version) = try {
        mavenArtifact.split(":", limit = 3)
    } catch (e: Exception) {
        logger.error("Failed to parse maven artifact: $mavenArtifact")
        logger.error("${e.message}")
        exitProcess(1)
    }
    val latestPatchVersion = if (Regex(".*?\\.[0-9]+$").matches(version))
        version.replaceAfterLast('.', "+")
    else version

    val isGit = "github.com" in vcsUrl

    val pluginId = if (isGit)
        vcsUrl.trimEnd('/').split('/').last()
    else artifact


    val groupDir = Paths.get("plugins/$DEFAULT_TYPE/$group")
    val groupDomain = group.split('.').reversed().joinToString(".")
    val versionPath = groupDir.resolve("$pluginId/$DEFAULT_KTOR_VERSION")

    writeFromText(groupDir.resolve("group.ktor.yaml"), """
        name: ${group.substringAfterLast('.').titleCase()}
        url: https://$groupDomain
        email: contact@$groupDomain
    """.trimIndent())

    writeFromText(groupDir.resolve("$pluginId/versions.ktor.yaml"), """
        # This file maps ktor versions to the required artifacts
        # See templates/versions.ktor.yaml for more details
        $DEFAULT_KTOR_VERSION_RANGE: $group:$artifact:$latestPatchVersion
    """.trimIndent())

    writeFromText(versionPath.resolve("manifest.ktor.yaml"), """
        # This file contains all the information needed for including your plugin.
        # See templates/manifest.ktor.yaml for more details
        name: ${artifact.titleCase()}
        description: A one-liner of what this does
        vcsLink: $vcsUrl
        license: Apache 2.0
        # Pick one of: Administration, Databases, HTTP, Monitoring, Routing, Security, Serialization, Sockets, Templating
        category: Routing
    """.trimIndent())

    val templatesDir = Paths.get(TEMPLATES)
    writeFromTemplate(templatesDir, versionPath, "install.kt")
    writeFromTemplate(templatesDir, versionPath, "documentation.md")

    logger.info("""
        
        We've populated some metadata for your project, but you'll need to edit these files with your plugin details:
        
        plugins
        └── $DEFAULT_TYPE
            └── $group
                ├── group.ktor.yaml
                └── $pluginId
                    ├── versions.ktor.yaml
                    └── $DEFAULT_KTOR_VERSION
                        ├── manifest.ktor.yaml
                        ├── install,kt
                        └── documentation.md
                                     
        Use the information in the README.md, and examples under ./templates and ./plugins, for help with editing.
        
        When you're done editing the files, don't forget to run `./gradlew buildRegistry` to ensure that your plugin compiles.
    """.trimIndent())
}

private fun writeFromTemplate(source: Path, destination: Path, filename: String) {
    val destFile = destination.resolve(filename)
    Files.createDirectories(destFile.parent)
    val inputText = source.resolve(filename).readText()
    destFile.writeText(inputText)
}

private fun writeFromText(destination: Path, text: String) {
    Files.createDirectories(destination.parent)
    destination.writeText(text)
}

private fun askQuestion(query: String): String {
    println("\n$query")
    var input = readlnOrNull().orEmpty().trim()
    while (input.isEmpty()) {
        input = readlnOrNull().orEmpty().trim()
    }
    return input
}

private fun String.titleCase() = split("\\W+").joinToString(" ") {
    it[0].uppercaseChar() + it.substring(1)
}
