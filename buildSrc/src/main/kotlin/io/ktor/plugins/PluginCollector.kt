package io.ktor.plugins

import com.charleskorn.kaml.*
import java.nio.file.Path
import kotlin.io.path.*

fun Path.readPluginFiles(): Sequence<PluginReference> = sequence {
    for (groupFolder in listDirectoryEntries()) {
        val group = groupFolder.name

        for (pluginFolder in groupFolder.listDirectoryEntries()) {
            val pluginFile = pluginFolder.resolve("versions.yaml")
            if (!pluginFile.exists())
                continue
            val yamlNode = pluginFile.inputStream().use(Yaml.default::parseToYamlNode)
            val versions: Map<String, Artifacts> = yamlNode.yamlMap.entries.entries.associate { (range, artifacts) ->
                range.content to when (artifacts) {
                    is YamlList -> artifacts.items.map {
                        ArtifactReference.parseReferenceString(
                            it.yamlScalar.content,
                            defaultGroup = group
                        )
                    }

                    is YamlScalar -> listOf(
                        ArtifactReference.parseReferenceString(
                            artifacts.content,
                            defaultGroup = group
                        )
                    )

                    else -> throw IllegalArgumentException("Unexpected node $yamlNode")
                }
            }
            assert(versions.isNotEmpty()) { "version mapping is required" }
            yield(
                PluginReference(
                    id = pluginFolder.name,
                    group = groupFolder.name,
                    versions = versions,
                )
            )
        }
    }
}