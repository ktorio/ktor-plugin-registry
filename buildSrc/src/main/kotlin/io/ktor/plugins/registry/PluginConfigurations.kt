/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.*
import org.slf4j.Logger
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.relativeTo

const val KTOR_MAVEN_REPO = "https://repo1.maven.org/maven2/io/ktor/ktor-server/maven-metadata.xml"
val DEPRECATED_VERSIONS = setOf(1)
// presets for inter-module dependencies
val moduleParents: Map<String, String> = mapOf(
    "client" to "core",
    "server" to "core",
)

/**
 * Referenced from build script - get a list of all plugin-release-module combinations for builds.
 */
fun collectPluginConfigs(
    log: Logger,
    ktorReleaseStrings: List<String>,
    rootPath: String = ".",
    filter: (String) -> Boolean = { true }
): List<PluginConfiguration> {
    log.info("Reading plugin configurations...")
    val ktorReleases = ktorReleaseStrings.map { ArtifactVersion.parse(it) }
    val pluginsRoot = Paths.get(rootPath, "plugins")
    val versionProperties: Map<String, String> = pluginsRoot.readVersionProperties()
    val pluginConfigs = pluginConfigCombinations(pluginsRoot, ktorReleases).flatMap { (type, release, pluginDir, modules) ->
        if (!filter(pluginDir.fileName.toString()))
            return@flatMap emptySequence()
        try {
            readPluginConfigs(
                pluginsRoot,
                type,
                modules,
                release,
                pluginDir,
                versionProperties
            )
        } catch (e: Exception) {
            log.error("Failed to read plugin $pluginDir", e)
            throw e
        }
    }.sortedBy {
        // ensure roots appear first
        it.parent
    }.toList()

    log.info("${pluginConfigs.size} plugin configurations found")

    return pluginConfigs
}

private fun pluginConfigCombinations(pluginsRoot: Path, releases: List<ArtifactVersion>): Sequence<PluginConfigurationStub> {
    val pluginIds = mutableMapOf<String, Path>()
    return sequence {
        for (type in listOf("server", "client")) {
            for (pluginDir in folders("$pluginsRoot/$type/*/*")) {
                pluginIds.put(pluginDir.fileName.toString(), pluginDir)?.let { previouslyFound ->
                    throw IllegalArgumentException("Duplicate plugins found: $previouslyFound, $pluginDir")
                }
                for (release in releases) {
                    val modules = (pluginDir.moduleReferences() + type).distinct()
                    yield(PluginConfigurationStub(type, release, pluginDir, modules))
                }
            }
        }
    }
}

private data class PluginConfigurationStub(
    val type: String,
    val release: ArtifactVersion,
    val pluginDir: Path,
    val modules: List<String>,
)

private fun readPluginConfigs(
    pluginsRoot: Path,
    type: String,
    modules: List<String>,
    release: ArtifactVersion,
    pluginDir: Path,
    versionProperties: Map<String, String>,
): Sequence<PluginConfiguration> {
    val pluginId = pluginDir.fileName.toString()
    val groupId = pluginDir.parent.fileName.toString()
    val artifactsMap = pluginDir.resolve(VERSIONS_FILE).readYamlMap()
        ?.readArtifacts(groupId, pluginId, versionProperties)
        .orEmpty()
    val (versionRange, artifacts) = artifactsMap.entries.lastOrNull {
        it.key.contains(release)
    } ?: return emptySequence()
    val pluginSourceDir = pluginDir
        .resolve(versionRange.safeName)
    val manifest = pluginSourceDir
        .resolve(MANIFEST_FILE).readYamlMap()
    val prerequisites = manifest?.get<YamlList>("prerequisites")?.items?.map {
        it.yamlScalar.content
    }.orEmpty()
    val repositories = manifest?.get<YamlMap>("gradle")?.get<YamlList>("repositories")?.items?.mapNotNull {
        it.yamlMap.get<YamlScalar>("url")?.content
    }.orEmpty()

    // recursively find all prerequisite artifacts
    val prerequisiteArtifactsByModule = prerequisites.flatMap { prerequisiteId ->
        val prerequisiteDir = folders("${pluginDir.parent.parent}/*/$prerequisiteId").firstOrNull()
        require(prerequisiteDir != null) {
            "Prerequisite plugin $prerequisiteId for $pluginId not found"
        }
        readPluginConfigs(
            pluginsRoot,
            type,
            modules,
            release,
            prerequisiteDir,
            versionProperties,
        )
    }.associate {
        it.module to it.artifacts
    }

    return modules.asSequence().map { module ->
        val prerequisiteArtifacts = prerequisiteArtifactsByModule[module] ?: emptyList()
        val requiredArtifacts = (artifacts + prerequisiteArtifacts).asSequence().filter {
            it.module == module || (it.module == null && module == type)
        }.map {
            when (it.version) {
                is KtorVersion -> it.resolve(release)
                else -> it
            }
        }.toList()

        val kotlinSourcePath =
            if (modules.size > 1)
                pluginSourceDir.resolve(module)
            else pluginSourceDir

        val parent = moduleParents[module]?.takeIf { it in modules }?.let { parent ->
            "$pluginId.${parent}.$release"
        }

        PluginConfiguration(
            kotlinSourcePath.relativeTo(pluginsRoot).toString(),
            pluginId,
            type,
            release.toString(),
            module,
            versionRange.safeName,
            requiredArtifacts,
            repositories,
            parent,
        )
    }
}

private fun readArtifacts(
    artifacts: YamlNode,
    groupId: String,
    versionVariables: Map<String, String>,
): List<ArtifactReference> =
    when (artifacts) {
        is YamlMap -> artifacts.entries.flatMap { (moduleName, moduleArtifacts) ->
            readArtifacts(moduleArtifacts, groupId, versionVariables).map {
                it.copy(module = moduleName.content)
            }
        }
        is YamlList -> artifacts.items.map {
            ArtifactReference.parse(it.yamlScalar.content, groupId, versionVariables)
        }
        is YamlScalar -> listOf(
            ArtifactReference.parse(artifacts.content, groupId, versionVariables)
        )
        else -> throw IllegalArgumentException("Unexpected node $artifacts")
    }

private fun YamlMap.readArtifacts(
    groupId: String,
    pluginId: String,
    versionVariables: Map<String, String>
): Map<ArtifactVersion, Artifacts> =
    entries.entries.filterNot { (key) ->
        key.content.matches(Regex("[\\w-]+"))
    }.associate { (versionRangeKey, artifacts) ->
        val artifactReferences = readArtifacts(artifacts, groupId, versionVariables)
        val versionRange = try {
            ArtifactVersion.parse(versionRangeKey.content)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid version range $versionRangeKey in plugin $pluginId", e)
        }

        versionRange to artifactReferences
    }

/**
 * Gets the latest configs by path (i.e., plugin, module)
 *
 * Used for resolving and compiling.
 */
fun List<PluginConfiguration>.latestByPath(): List<PluginConfiguration> =
    groupBy { it.path }.map { (_, configs) -> configs.maxBy { it.release } }
