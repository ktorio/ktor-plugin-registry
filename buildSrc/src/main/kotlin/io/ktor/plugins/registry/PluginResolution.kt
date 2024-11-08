/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import org.gradle.api.artifacts.ResolvedConfiguration
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.inputStream

fun writeResolvedPluginConfigurations(
    pluginConfigs: List<PluginConfiguration>,
    configurations: (String) -> ResolvedConfiguration,
) {
    val pluginsDir = Paths.get("build/plugins").clear()
    val classpathsDir = pluginsDir.resolve("classpaths").createDirectory()

    pluginsDir.resolve("configurations.yaml").outputStream().use { output ->
        Yaml.default.encodeToStream(pluginConfigs, output)
    }
    for (pluginConfig in pluginConfigs) {
        val classPathFile = classpathsDir.resolve("${pluginConfig.id}.${pluginConfig.release}.yaml")
        val alreadyResolved: Set<String> = if (classPathFile.exists()) {
            classPathFile.inputStream().use { input ->
                Yaml.default.decodeFromStream<Map<String, ResolvedArtifact>>(input).keys
            }
        } else emptySet()

        val artifactsMap = configurations(pluginConfig.name).resolvedArtifacts.associate { artifact ->
            "${artifact.moduleVersion.id.group}:${artifact.moduleVersion.id.name}" to ResolvedArtifact(
                version = artifact.moduleVersion.id.version,
                path = artifact.file.absolutePath
            )
        } - alreadyResolved

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