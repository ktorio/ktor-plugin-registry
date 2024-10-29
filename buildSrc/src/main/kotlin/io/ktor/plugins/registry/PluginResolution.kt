/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.encodeToStream
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import java.io.BufferedWriter
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Get all artifact dependency strings for the given Ktor version. Used in gradle classpath resolution.
 */
fun Sequence<PluginReference>.allArtifactsForVersion(ktorRelease: String): Sequence<String> =
    flatMap { plugin ->
        plugin.allArtifactsForVersion(ktorRelease).map { artifact ->
            with(artifact) {
                "$group:$name:" + when(version) {
                    is MatchKtor -> ktorRelease
                    else -> version.toString()
                }
            }
        }
    }

/**
 * Prints out artifacts required for each Ktor release for the registry builder.
 */
fun outputReleaseArtifacts(outputFile: Path, configurations: Map<String, Set<ResolvedArtifact>>) {
    val artifactsByRelease =
        configurations.mapValues { (_, artifacts) ->
            artifacts.associate { resolvedArtifact ->
                Pair(
                    resolvedArtifact.referenceString(),
                    resolvedArtifact.file.path
                )
            }
        }

    outputFile.outputStream().use { output ->
        Yaml.default.encodeToStream(artifactsByRelease, output)
    }
}

@OptIn(ExperimentalPathApi::class)
fun prepareDirectory(directory: Path) {
    directory.deleteRecursively()
    directory.createDirectories()
}

/**
 * Outputs the dependency trace for all configurations to the specified output file.
 */
fun outputDependencyTrees(directory: Path, configurations: Map<String, ResolvedConfiguration>) {
    directory.createDirectory()
    configurations.forEach { (name, configuration) ->
        val releaseDir = directory.resolve(name).createDirectory()
        val firstLevelDependencies = configuration.firstLevelModuleDependencies
        for (dependency in firstLevelDependencies) {
            releaseDir.resolve("${dependency.moduleName}.yaml").bufferedWriter().use { writer ->
                writer.outputDependencyTree(dependency, 0)
            }
        }
    }
}

fun BufferedWriter.outputDependencyTree(dependency: ResolvedDependency, level: Int) {
    if (dependency.children.isEmpty()) {
        appendLine("${"  ".repeat(level)}\"${dependency.moduleGroup}:${dependency.moduleName}:${dependency.moduleVersion}\": []")
    } else {
        appendLine("${"  ".repeat(level)}\"${dependency.moduleGroup}:${dependency.moduleName}:${dependency.moduleVersion}\":")
        for (child in dependency.children) {
            outputDependencyTree(child, level + 1)
        }
    }
}

fun ResolvedArtifact.referenceString() =
    "${moduleVersion.id.group}:$name:${moduleVersion.id.version}"