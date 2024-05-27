/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.*
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Reads plugin details using our custom file structure:
 *
 * /<group>
 *     /group.ktor.yaml
 *     /<plugin>
 *         /versions.ktor.yaml
 *         /<version>
 *             /manifest.ktor.yaml
 */
fun Path.readPluginFiles(client: Boolean = false, filter: (String) -> Boolean = { true }): Sequence<PluginReference> {
    val seen = mutableSetOf<String>()
    val versionVariables = mutableMapOf<String, String>()
    val unparsedReferences = mutableListOf<UnparsedPluginReference>()

    for (groupFolder in listDirectoryEntries()) {
        val groupId = groupFolder.name
        if (groupId.startsWith("."))
            continue

        val groupInfo = groupFolder.resolve("group.ktor.yaml").readPluginGroup()

        for (pluginFolder in groupFolder.listDirectoryEntries()) {
            if (!pluginFolder.isDirectory() || !filter(pluginFolder.name) || pluginFolder.resolve("ignore").exists())
                continue

            val pluginId = pluginFolder.name
            if (!seen.add(pluginId))
                throw IllegalArgumentException("Duplicate plugin ID \"$pluginId\"")

            val pluginFile = pluginFolder.resolve("versions.ktor.yaml")
            if (!pluginFile.exists())
                throw IllegalArgumentException("Missing \"versions.ktor.yaml\" in $pluginId")

            // Split version ranges and variables for resolution
            val versionsFileMap = pluginFile.readYamlMap()
                ?: throw IllegalArgumentException("Could not find versions.ktor.yaml")
            val versionsMap = mutableMapOf<String, YamlNode>()

            for ((key, value) in versionsFileMap.entries.mapKeys { it.key.content }) {
                if (key.first().isLetter())
                    versionVariables[key] = value.yamlScalar.content
                else
                    versionsMap[key] = value
            }

            require(groupInfo != null) { "Missing group.ktor.yaml for plugin $pluginId"}

            unparsedReferences += UnparsedPluginReference(pluginId, groupInfo, versionsMap)
        }
    }

    return unparsedReferences.asSequence().map {
        it.resolve(versionVariables, client)
    }
}

private data class UnparsedPluginReference(
    val id: String,
    val group: PluginGroup,
    val versions: Map<String, YamlNode>,
) {
    fun resolve(variables: Map<String, String>, client: Boolean): PluginReference {
        val versions = readVersionsMapping(id, group.id, versions, variables)
        require(versions.isNotEmpty()) { "Version mapping is required" }
        return PluginReference(
            id = id,
            group = group,
            versions = versions,
            client = client,
        )
    }
}

private fun readVersionsMapping(id: String, groupId: String, versionsYamlNode: Map<String, YamlNode>, versionVariables: Map<String, String>): Map<String, Artifacts> {
    try {
        val versions: Map<String, Artifacts> =
            versionsYamlNode.entries.associate { (ktorVersionRange, artifacts) ->
                val artifactReferences = when (artifacts) {
                    is YamlList -> artifacts.items.map {
                        ArtifactReference.parse(it.yamlScalar.content, groupId, versionVariables)
                    }
                    is YamlScalar -> listOf(
                        ArtifactReference.parse(artifacts.content, groupId, versionVariables)
                    )
                    else -> throw IllegalArgumentException("Unexpected node $versionsYamlNode")
                }
                try {
                    ArtifactVersion.parse(ktorVersionRange)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid version range $ktorVersionRange in plugin $id", e)
                }

                ktorVersionRange to artifactReferences
            }
        return versions

    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse versions.ktor.yaml for plugin \"$id\". ${e.message ?: ""}", e)
    }
}

fun Path.readPluginGroup(): PluginGroup? =
    readYamlMap()?.let { yaml ->
        val (name, url, email, logo) = listOf("name", "url", "email", "logo").map {
            yaml.get<YamlScalar>(it)?.content
        }
        PluginGroup(parent.name, name, url, email, logo)
    }

private fun Path.readYamlMap(): YamlMap? =
    takeIf { it.exists() }
        ?.inputStream()
        ?.use(Yaml.default::parseToYamlNode)
        ?.yamlMap

