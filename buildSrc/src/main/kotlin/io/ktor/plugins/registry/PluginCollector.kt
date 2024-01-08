package io.ktor.plugins.registry

import com.charleskorn.kaml.*
import com.vdurmont.semver4j.Requirement
import java.nio.file.Path
import kotlin.io.path.*
import io.ktor.plugins.registry.SemverUtils.semverString

fun Path.readPluginFiles(): Sequence<PluginReference> = sequence {
    for (groupFolder in listDirectoryEntries()) {
        val groupId = groupFolder.name
        val groupInfo = groupFolder.resolve("group.ktor.yaml").inputStream().use(Yaml.default::parseToYamlNode).yamlMap.let { yaml: YamlMap ->
            PluginGroup(
                id = groupId,
                name = yaml.get<YamlScalar>("name")?.content,
                url = yaml.get<YamlScalar>("url")?.content
            )
        }

        for (pluginFolder in groupFolder.listDirectoryEntries()) {
            val pluginFile = pluginFolder.resolve("versions.ktor.yaml")
            if (!pluginFile.exists())
                continue
            val yamlNode = pluginFile.inputStream().use(Yaml.default::parseToYamlNode)
            val versions: Map<String, Artifacts> = yamlNode.yamlMap.entries.entries.associate { (range, artifacts) ->
                val ktorVersionRange = range.content
                val artifactReferences = when (artifacts) {
                    is YamlList -> artifacts.items.map { ArtifactReference.parseReferenceString(it.yamlScalar.content, groupId) }
                    is YamlScalar -> listOf(ArtifactReference.parseReferenceString(artifacts.content, groupId))

                    else -> throw IllegalArgumentException("Unexpected node $yamlNode")
                }
                try {
                    Requirement.buildNPM(ktorVersionRange.semverString())
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid version range $ktorVersionRange in $pluginFile", e)
                }

                ktorVersionRange to artifactReferences
            }
            assert(versions.isNotEmpty()) { "version mapping is required" }
            yield(
                PluginReference(
                    id = pluginFolder.name,
                    group = groupInfo,
                    versions = versions,
                )
            )
        }
    }
}