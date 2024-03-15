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
fun Path.readPluginFiles(client: Boolean = false, filter: (String) -> Boolean = { true }): Sequence<PluginReference> = sequence {
    val seen = mutableSetOf<String>()

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

            val versions: Map<String, Artifacts> = readVersionsMapping(pluginFile, groupId)
            require(versions.isNotEmpty()) { "Version mapping is required" }
            require(groupInfo != null) { "Missing group.ktor.yaml for plugin $pluginId"}

            yield(
                PluginReference(
                    id = pluginId,
                    group = groupInfo,
                    versions = versions,
                    client = client,
                )
            )
        }
    }
}

private fun readVersionsMapping(pluginFile: Path, groupId: String): Map<String, Artifacts> {
    try {
        val versionsYamlNode = pluginFile.readYamlMap()
            ?: throw IllegalArgumentException("Could not find versions.ktor.yaml")

        val versions: Map<String, Artifacts> =
            versionsYamlNode.entries.entries.associate { (range, artifacts) ->
                val ktorVersionRange = range.content
                val artifactReferences = when (artifacts) {
                    is YamlList -> artifacts.items.map {
                        ArtifactReference.parseReferenceString(
                            it.yamlScalar.content,
                            groupId
                        )
                    }

                    is YamlScalar -> listOf(ArtifactReference.parseReferenceString(artifacts.content, groupId))

                    else -> throw IllegalArgumentException("Unexpected node $versionsYamlNode")
                }
                try {
                    ArtifactVersion.parse(ktorVersionRange)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid version range $ktorVersionRange in $pluginFile", e)
                }

                ktorVersionRange to artifactReferences
            }
        return versions

    } catch (e: IOException) {
        throw IllegalArgumentException("Failed to read versions.ktor.yaml for plugin \"${pluginFile.parent.name}\"", e)
    } catch (e: YamlException) {
        throw IllegalArgumentException("Failed to parse versions.ktor.yaml for plugin \"${pluginFile.parent.name}\"", e)
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

