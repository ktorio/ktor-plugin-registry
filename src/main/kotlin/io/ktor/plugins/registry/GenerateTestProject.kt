/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import io.ktor.client.*
import io.ktor.plugins.registry.utils.*
import io.ktor.plugins.registry.utils.Terminal.argsToMap
import io.ktor.plugins.registry.utils.Terminal.ktorScriptHeader
import io.ktor.plugins.registry.utils.Terminal.tree
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

private const val GENERATE_URL = "https://start-ktor-io.labs.jb.gg/project/generate"
private const val COMPARE_BRANCH = "main"
private const val BUILD_DIR = "build"
private const val OUTPUT_DIR = "test-project"

/**
 * Connects to the project generator back-end and creates a new Ktor project using the
 * plugins introduced in the local project when compared to main.
 */
fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("GenerateTestProject")
    logger.info(ktorScriptHeader())

    val argsMap = argsToMap(mapOf(
        "url" to GENERATE_URL,
        "branch" to COMPARE_BRANCH,
        "build-dir" to BUILD_DIR,
    ), args)

    val pluginIds = argsMap["plugins"]?.split(",")
        ?: GitSupport.getChangedPluginIds(mainBranchName = argsMap["branch"]!!)

    if (pluginIds.isEmpty()) {
        logger.info("No plugins changed or provided for test project generation")
        return
    }

    logger.info("Creating project from plugins:\n${pluginIds.joinToString("\n") { " - $it" }}")
    val registryFiles = RegistryOutputFiles(argsMap["build-dir"]!!)
    val latestRelease = registryFiles.ktorReleases.last()

    val generatorBackendUrl = argsMap["url"]!!
    val projectPath = argsMap["path"] ?: OUTPUT_DIR
    logger.info("Extracting new project archive from $generatorBackendUrl into $projectPath")
    val generatorClient = ProjectGeneratorClient(generatorBackendUrl, HttpClient {})
    runBlocking {
        generatorClient.generate {
            ktorVersion = latestRelease
            features = registryFiles.getAllRequiredPluginIds(latestRelease, pluginIds)
            featureOverrides = pluginIds.map { registryFiles.readManifest(latestRelease, it) }
            outputDir = projectPath
        }
    }

    logger.info("Project generation successful!")
    logger.info("Explore the new project files:\n\n${tree(projectPath)}")
    logger.info("To run the new project:\n  cd $projectPath\n  ./gradlew run")
}

/**
 * Get all transitive dependencies by recursing through the plugin manifest dependency tree.
 */
private fun RegistryOutputFiles.getAllRequiredPluginIds(ktorRelease: String, pluginIds: List<String>): List<String> {
    return pluginIds + pluginIds.flatMap { id ->
        getAllRequiredPluginIds(ktorRelease, readManifest(ktorRelease, id).requiredPluginIds())
    }
}

private fun JsonObject.requiredPluginIds() =
    this["required_feature_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

