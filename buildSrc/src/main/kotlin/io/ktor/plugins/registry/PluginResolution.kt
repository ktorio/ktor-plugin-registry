/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import org.gradle.api.artifacts.ResolvedConfiguration
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.inputStream

/**
 * Writes the resolved plugin configuration classpaths to the expected paths for use during export.
 *
 * @param pluginConfigs List of plugin configurations to be processed.
 * @param gradleConfigLookup Function for finding gradle ResolvedConfiguration for actual artifact paths
 */
fun writeResolvedPluginConfigurations(
    pluginConfigs: List<PluginConfiguration>,
    gradleConfigLookup: (String) -> ResolvedConfiguration,
) {
    val pluginsDir = Paths.get("build/plugins").clear()
    val classpathsDir = pluginsDir.resolve("classpaths").createDirectory()

    pluginsDir.resolve("configurations.yaml").outputStream().use { output ->
        Yaml.default.encodeToStream(pluginConfigs, output)
    }
    for (pluginConfig in pluginConfigs) {
        val classPathFile = classpathsDir.resolve("${pluginConfig.id}.${pluginConfig.release}.yaml")
        val alreadyResolved = readAlreadyResolved(classPathFile)
        val artifactsMap = pluginConfig.getResolvedArtifacts(gradleConfigLookup) - alreadyResolved

        if (artifactsMap.isNotEmpty()) {
            classPathFile.outputStream(
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            ).use { output ->
                Yaml.default.encodeToStream(artifactsMap, output)
            }
        }
    }
}

private fun PluginConfiguration.getResolvedArtifacts(gradleConfigLookup: (String) -> ResolvedConfiguration): Map<String, ResolvedArtifact> =
    gradleConfigLookup(name).resolvedArtifacts.associate { artifact ->
        "${artifact.moduleVersion.id.group}:${artifact.moduleVersion.id.name}" to ResolvedArtifact(
            version = artifact.moduleVersion.id.version,
            path = artifact.file.absolutePath
        )
    }

private fun readAlreadyResolved(classPathFile: Path): Set<String> {
    if (!classPathFile.exists())
        return emptySet()
    return classPathFile.inputStream().use { input ->
        Yaml.default.decodeFromStream<Map<String, ResolvedArtifact>>(input).keys
    }
}